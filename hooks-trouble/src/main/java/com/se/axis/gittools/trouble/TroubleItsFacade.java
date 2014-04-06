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

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;

import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;

import com.google.inject.Inject;

import com.google.gson.GsonBuilder;

import com.googlesource.gerrit.plugins.hooks.its.NoopItsFacade;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ITS base hook.
 *
 * This Class is called from ITS base on relevant events.
 */
public class TroubleItsFacade extends NoopItsFacade {

  /**
   * Object represenation of the add-velocity-event actions in action.config.
   */
  public class VelocityComment {
    /**
     * Identity of the event.
     */
    public String id;

    /**
     * The username of the person that triggered the event.
     */
    public String blame;

    /**
     * The change number.
     */
    public Integer change;

    /**
     * The patch set number.
     */
    public Integer patchSet;

    /**
     * The patch set reference (e.g. refs/changes/*).
     */
    public String ref;

    /**
     * The patch set revision.
     */
    public String rev;

    /**
     * The target branch.
     */
    public String branch;

    /**
     * The repo.
     */
    public String project;

    /**
     * CBM approval.
     */
    public String cbmApproved;

    /**
     * Daily Build OK.
     */
    public String dailyBuildOk;

    /**
     * Validates velocity-comment data.
     */
    public final VelocityComment check() {
      if (id == null) {
        throw new IllegalArgumentException("rule.*.id is not defined");
      }
      if (change == null) {
        throw new IllegalArgumentException("rule.*.change is not defined");
      }
      if (patchSet == null) {
        throw new IllegalArgumentException("rule.*.patchSet is not defined");
      }
      if (rev == null) {
        throw new IllegalArgumentException("rule.*.rev is not defined");
      }
      if (branch == null) {
        throw new IllegalArgumentException("rule.*.branch is not defined");
      }
      if (project == null) {
        throw new IllegalArgumentException("rule.*.project is not defined");
      }
      if (blame == null) {
        throw new IllegalArgumentException("rule.*.blame is not defined");
      }
      if (cbmApproved != null) {
        try {
          Integer.parseInt(cbmApproved);
        } catch (NumberFormatException e) { // did not change
          cbmApproved = null;
        }
      }
      if (dailyBuildOk != null) {
        try {
          Integer.parseInt(dailyBuildOk);
        } catch (NumberFormatException e) { // did not change
          dailyBuildOk = null;
        }
      }
      return this;
    }

    /**
     * Creates a Change.Id object.
     */
    public final Change.Id changeId() {
      return new Change.Id(change);
    }

    /**
     * Creates a PatchSet.Id object.
     */
    public final PatchSet.Id patchSetId() {
      return new PatchSet.Id(changeId(), patchSet);
    }
  }

  /**
   * Gerrit configuration section name.
   */
  public static final String ITS_NAME_TROUBLE = "trouble";

  private static final int CODE_REVIEW_MAX = 2;
  private static final int CODE_REVIEW_MIN = -2;
  private static final int DAILY_BUILT_MAX = 1;
  private static final int DAILY_BUILT_MIN = -1;

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

  private final GsonBuilder gson = new GsonBuilder();

  private final String formatGerritUrl;

  /**
   * Constructor.
   */
  @Inject
  public TroubleItsFacade(@GerritServerConfig final Config cfg, final SchemaFactory<ReviewDb> schema,
      final GitRepositoryManager repoManager) {
    gerritConfig = cfg;
    reviewDbProvider = schema;
    this.repoManager = repoManager;
    apiUser = gerritConfig.getString(TroubleItsFacade.ITS_NAME_TROUBLE, null, "username");
    if (apiUser == null) {
      throw new IllegalArgumentException("trouble.username not set in gerrit.config");
    }
    String url = gerritConfig.getString("gerrit", null, "canonicalWebUrl");
    if (url == null) {
      throw new IllegalArgumentException("gerrit.canonicalWebUrl not set in gerrit.config");
    }
    formatGerritUrl = "\"%s@%s\":" + url.replaceFirst("/+$", "") + "/#/c/%d/";
  }

  @Override
  public final String name() {
    return "Trouble";
  }

  @Override
  public final void addComment(final String issueId, final String json) throws IOException {
    LOG.debug("addComment({},{})", issueId, json);

    // deserialze the VelocityComment
    VelocityComment event = gson.create().fromJson(json, VelocityComment.class).check(); // deserialize!

    // create the TroubleClient
    int ticket = parseIssueId(issueId);
    String username = event.blame.indexOf('.') != -1 ? apiUser : event.blame;
    TroubleClient troubleClient = TroubleClient.create(gerritConfig, ticket, username);

    String comment = null;
    String targetLink = String.format(formatGerritUrl, event.branch, event.project, event.change);
    if (event.id.equals("patchset-created")) {
      TroubleClient.Package troublePackage = createPackage(event.project, event.branch, event.rev, event.ref, null);
      troubleClient.addOrUpdatePackage(troublePackage);
      // create comment about the new patchset
      comment = String.format(FORMAT_COMMENT_REVIEW, event.patchSet == 1 ? "STARTED" : "UPDATED", targetLink);
    } else if (event.id.equals("change-restored")) {
      TroubleClient.Package troublePackage = createPackage(event.project, event.branch, event.rev, event.ref, event.patchSetId());
      troubleClient.addOrUpdatePackage(troublePackage);
      // create comment about the restored review
      comment = String.format(FORMAT_COMMENT_REVIEW, "RESTORED", targetLink);
    } else if (event.id.equals("change-abandoned")) {
      troubleClient.deletePackage(event.project, event.branch);
      // create comment about the abandoned review
      comment = String.format(FORMAT_COMMENT_REVIEW, "ABANDONED", targetLink);
    } else if (event.id.equals("comment-added")) {
      TroubleClient.Package troublePackage = createPackage(event.project, event.branch, event.rev, event.ref, event.patchSetId());
      troubleClient.addOrUpdatePackage(troublePackage);
    }
    if (comment != null) {
      troubleClient.addComment(new TroubleClient.Comment(comment));
    }
  }

  @Override
  public final boolean exists(final String issueId) throws IOException {
    LOG.debug("exists({})", issueId);
    return TroubleClient.create(gerritConfig, parseIssueId(issueId)).ticketExists();
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

  /**
   * Invoked when a new PatchSet is created.
   */
  private TroubleClient.Package createPackage(final String name, final String targetBranch,
      final String revision, final String patchSetRef, final PatchSet.Id patchSetId) throws IOException {
    TroubleClient.Package troublePackage = new TroubleClient.Package();
    troublePackage.name = name;
    troublePackage.targetBranch = targetBranch;
    troublePackage.preMergeRef = new TroubleClient.Reference(patchSetRef);

    // resolve dailyBuildOk and cbmApproved
    ReviewDb db = null;
    try {
      if (patchSetId != null) {
        db = reviewDbProvider.open();
        for (PatchSetApproval approval : db.patchSetApprovals().byPatchSet(patchSetId)) {
          LOG.debug("{}, {}", approval.getLabelId(), approval.getValue());
          if ("Code-Review".equals(approval.getLabelId().get())) {
            if (troublePackage.cbmApproved == null && approval.getValue() == CODE_REVIEW_MAX) {
              troublePackage.cbmApproved = true;
            } else if (approval.getValue() == CODE_REVIEW_MIN) {
              troublePackage.cbmApproved = false;
            }
          } else if ("Daily-Built".equals(approval.getLabelId().get())) {
            if (troublePackage.dailyBuildOk == null && approval.getValue() == DAILY_BUILT_MAX) {
              troublePackage.dailyBuildOk = true;
            } else if (approval.getValue() == DAILY_BUILT_MIN) {
              troublePackage.dailyBuildOk = false;
            }
          }
        }
      }
      if (troublePackage.cbmApproved == null) {
        troublePackage.cbmApproved = false;
      }
      if (troublePackage.dailyBuildOk == null) {
        troublePackage.dailyBuildOk = false;
      }
    } catch (OrmException err) {
      LOG.error("Failed to access ReviewDb: ", err.toString());
      troublePackage.cbmApproved = null; // don't change
      troublePackage.dailyBuildOk = null; //don't changes
    } finally {
      if (db != null) {
        db.close();
      }
    }

    // resolve fix information
    List<String> referenceFooters = getReferenceFooters(repoManager, name, revision);
    for (String footer : referenceFooters) {
      if (troublePackage.fixRef == null) {
        troublePackage.fixBranch = parseSourceBranch(footer);
        troublePackage.fixRef = new TroubleClient.Reference(parseRevision(footer));
      }
    }

    return troublePackage;
  }

  /**
   * Reads the reference footer from the repository.
   */
  private static List<String> getReferenceFooters(final GitRepositoryManager repoManager,
      final String packageName, final String rev) throws IOException {
    Repository git = repoManager.openRepository(new Project.NameKey(packageName));
    try {
      RevWalk revWalk = new RevWalk(git);
      RevCommit commit = revWalk.parseCommit(git.resolve(rev));
      return commit.getFooterLines("Reference");
    } finally {
      git.close();
    }
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
