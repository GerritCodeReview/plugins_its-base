/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;

import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ITS base hook.
 *
 * This Class is called from ITS base on relevant events.
 */
public class TroubleItsFacade implements ItsFacade {
  /**
   * Gerrit configuration section name.
   */
  public static final String ITS_NAME_TROUBLE = "trouble";

  private static final String GERRIT_CONFIG_USERNAME = "username";
  private static final String GERRIT_CONFIG_PASSWORD = "password";
  private static final String GERRIT_CONFIG_URL = "url";

  private static final String FORMAT_COMMENT_REVIEW = "Gerrit Review %s for target: %s.";

  private static final String PATTERN_SHA1 = "[a-fA-F0-9]{40}";

  private static final Logger LOG = LoggerFactory.getLogger(TroubleItsFacade.class);

  private final Config gerritConfig;
  private final SchemaFactory<ReviewDb> reviewDbProvider;
  private final GitRepositoryManager repoManager;
  private final String apiUser;

  private URL relatedUrl;

  /**
   * Constructor.
   */
  @Inject
  public TroubleItsFacade(@GerritServerConfig final Config cfg, final SchemaFactory<ReviewDb> schema,
                          final GitRepositoryManager repoManager) {
    this.gerritConfig = cfg;
    this.reviewDbProvider = schema;
    this.repoManager = repoManager;
    this.apiUser = gerritConfig.getString(TroubleItsFacade.ITS_NAME_TROUBLE, null, "username");
    if (apiUser == null) {
      throw new NullPointerException("trouble.username not set in gerrit.config");
    }
  }

  @Override
  public final String name() {
    return "Trouble";
  }

  @Override
  public final void addComment(final String issueId, final String comment) throws IOException {
    LOG.debug("addComment({},{})", issueId, comment);
  }

  @Override
  public final void addRelatedLink(final String issueId, final URL url, final String description) throws IOException {
    LOG.debug("addRelatedLink({},{},{})", new Object[] {issueId, relatedUrl, description});
    relatedUrl = url;
  }

  @Override
  public final boolean exists(final String issueId) throws IOException {
    LOG.debug("exists({})", issueId);
    return TroubleClient.create(gerritConfig, parseIssueId(issueId)).ticketExists();
  }

  @Override
  public final void performAction(final String issueId, final String actionName) throws IOException {
    LOG.debug("performAction({},{})", issueId, actionName);
    if (relatedUrl == null) { // safety
      throw new AssertionError("addRelatedLink or createLinkForWebui was NOT called before this method");
    }

    try {
      // get the info from database
      ReviewDb db = null;
      Change change = null;
      PatchSet patchSet = null;
      Account account = null;
      try {
        db = reviewDbProvider.open();
        change = db.changes().get(extractChangeId(relatedUrl));
        patchSet = db.patchSets().get(change.currentPatchSetId());
        account = db.accounts().get(patchSet.getUploader());
      } catch (OrmException e) {
        throw new IOException("Failed to access review db", e);
      } finally {
        if (db != null) {
          db.close();
        }
      }

      // create the trouble client
      int ticket = parseIssueId(issueId);
      String axisId = getAxisId(account, apiUser);
      TroubleClient troubleClient = TroubleClient.create(gerritConfig, ticket, axisId);


      // handle the events
      String targetLink = createCommentTargetLink(change, relatedUrl);
      if (actionName.equals("patchset-created")) {
        // Create comment about the new patchset
        String comment = String.format(FORMAT_COMMENT_REVIEW, patchSet.getId().get() == 1 ? "STARTED" : "UPDATED", targetLink);
        troubleClient.addComment(new TroubleClient.Comment(comment));
        handlePatchSetCreated(account, change, patchSet, troubleClient);
      } else if (actionName.equals("change-restored")) {
        // Create comment about the restored review
        String comment = String.format(FORMAT_COMMENT_REVIEW, "RESTORED", targetLink);
        troubleClient.addComment(new TroubleClient.Comment(comment));
        handlePatchSetCreated(account, change, patchSet, troubleClient);
      } else if (actionName.equals("change-abandoned")) {
        // Create comment about the abandoned review
        String comment = String.format(FORMAT_COMMENT_REVIEW, "ABANDONED", targetLink);
        troubleClient.addComment(new TroubleClient.Comment(comment));
        handleChangeAbandoned(account, change, patchSet, troubleClient);
      }
    } finally {
      relatedUrl = null; // reset
    }
  }

  @Override
  public final String healthCheck(final Check check) throws IOException {
    LOG.debug("healthCheck()");
    exists("1"); // just fetch ticket 1
    String health = null;
    if (check.equals(Check.ACCESS)) {
      health = "{\"status\"=\"ok\",\"username\"=\"" + apiUser + "\"}";
      LOG.debug("health check on ACCESS result: {}", health);
    } else {
      health = "{\"status\"=\"ok\",\"system\"=\"" + ITS_NAME_TROUBLE + "\"}";
      LOG.debug("health check on SYSTEM result: {}", health);
    }
    return health;
  }

  @Override
  public final String createLinkForWebui(final String url, final String text) {
    LOG.debug("createLinkForWebui({},{})", url, text);
    String result = url;
    if (text != null && !text.equals(url)) {
        result += " (" + text + ")";
    }
    try {
      relatedUrl = new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalig url received: " + url, e);
    }
    return result;
  }

  /**
   * Invoked when a new PatchSet is created.
   */
  private void handlePatchSetCreated(final Account account, final Change change, final PatchSet patchSet,
      final TroubleClient troubleClient) throws IOException {
    TroubleClient.Package packageObj = new TroubleClient.Package();
    packageObj.name = change.getProject().get();
    packageObj.targetBranch = change.getDest().get().replaceFirst("^refs/heads/", "");
    List<String> referenceFooters = getReferenceFooters(repoManager, change.getProject(), patchSet.getRevision().get());
    for (String footer : referenceFooters) {
      if (packageObj.fixRef == null) {
        packageObj.fixBranch = parseSourceBranch(footer);
        packageObj.fixRef = new TroubleClient.Reference(parseRevision(footer));
      }
    }
    packageObj.preMergeRef = new TroubleClient.Reference(patchSet.getRefName());

    // add/update the package (package section)
    troubleClient.addOrUpdatePackage(packageObj);
  }

  /**
   * Invoked when a new PatchSet is created.
   */
  private void handleChangeAbandoned(final Account account, final Change change, final PatchSet patchSet,
      final TroubleClient troubleClient) throws IOException {
    // delete the package (package section)
    String packageName = change.getProject().get();
    String targetBranch = change.getDest().get().replaceFirst("^refs/heads/", "");
    troubleClient.deletePackage(packageName, targetBranch);
  }

  /**
   * Create a link back to the Gerrit Review.
   */
  private static String createCommentTargetLink(final Change change, final URL url) {
    String packageName = change.getProject().get();
    String targetBranch = change.getDest().get().replaceFirst("^refs/heads/", "");

    StringBuilder link = new StringBuilder();
    link.append('"').append(targetBranch).append('@').append(packageName).append("\":").append(url);
    return link.toString();
  }

  /**
   * Reads the Axis id from a Gerrit account.
   *
   * Fallback to defaultAxisId in case of an error.
   */
  private static String getAxisId(final Account account, final String defaultAxisId) {
    String axisId = null;  // default
    String email = account.getPreferredEmail();
    if (email != null) {
      int pos = email.indexOf('@');
      if (pos != -1) {
        String emailAccount = email.substring(0, pos);
        if (emailAccount.indexOf('.') == -1) { // complete email addresses are disregarded
          axisId = emailAccount;
        }
      }
    }
    if (axisId == null) {
      LOG.warn("Invalid preferred email {}", email);
      axisId = defaultAxisId;
    }
    return axisId;
  }

  /**
   * Reads the reference footer from the repository.
   */
  private static List<String> getReferenceFooters(final GitRepositoryManager repoManager,
      final Project.NameKey projectName, final String rev) throws IOException {
    Repository git = repoManager.openRepository(projectName);
    try {
      RevWalk revWalk = new RevWalk(git);
      RevCommit commit = revWalk.parseCommit(git.resolve(rev));
      return commit.getFooterLines("Reference");
    } finally {
      git.close();
    }
  }

  /**
   * Extract (parse) the change id from an URL.
   */
  private static Change.Id extractChangeId(final URL url) {
    // its the final segment of path, e.g. http://localhost:8090/4
    File f = new File(url.getPath());
    Change.Id id = null;
    try {
      id = new Change.Id(Integer.parseInt(f.getName()));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Failed to extract change id from URL: " + url, e);
    }
    return id;
  }

  /**
   * Convert issue id String to int.
   */
  private static int parseIssueId(final String issueId) {
    try {
      return Integer.parseInt(issueId);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("commentlink.trouble.match (group 1) is not a number", e);
    }
  }

  /**
   * Extract (parse) the Reference footer from the Review topic.
   *
   * The Reference footer value is returned if found.
   */
  private static String parseReferenceFooter(final String topic) {
    // trim off any trailing newlines.
    String trimmed = topic.trim();

    // cut off the subject and body keeping only the footers section
    String[] sections = trimmed.split("\\n{2,}");
    String footer = sections[sections.length - 1];

    // find the footer in question
    Pattern pattern = Pattern.compile("^Reference:\\s*(" + PATTERN_SHA1 + ".*)$", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(footer);

    return (matcher.find() ? matcher.group(1) : null);
  }

  /**
   * Extract (parse) the source (fix) branch of a Review from the reference footer.
   *
   * Only the last encountered branch is returned.
   */
  private static String parseSourceBranch(final String reference) {
    Pattern pattern = Pattern.compile("\\(([^\\)]+)\\)");
    Matcher matcher = pattern.matcher(reference);
    if (matcher.find()) {
      String[] branches = matcher.group(1).split("\\s*,\\s*");
      return branches[branches.length - 1]; // last match
    }
    return null;
  }

  /**
   * Extract (parse) the source branch revision from the reference footer.
   */
  private static String parseRevision(final String reference) {
    Pattern pattern = Pattern.compile("^" + PATTERN_SHA1);
    Matcher matcher = pattern.matcher(reference);
    return (matcher.find() ? matcher.group(0) : null);
  }
}
