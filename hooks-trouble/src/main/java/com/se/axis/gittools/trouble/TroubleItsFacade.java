/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.ChangeMessage;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;

import com.google.inject.Singleton;

import com.google.gson.GsonBuilder;

import com.googlesource.gerrit.plugins.hooks.its.NoopItsFacade;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ITS base hook.
 *
 * This Class is called from ITS base on relevant events.
 */
@Singleton
public class TroubleItsFacade extends NoopItsFacade implements LifecycleListener {

  /**
   * Object represenation of the add-velocity-event actions in action.config.
   */
  public class VelocityComment {
    /**
     * The ticket.
     */
    public Integer ticket;

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
      if (ticket == null) {
        throw new IllegalArgumentException("rule.*.ticket is not defined");
      }
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
   * Approval container.
   */
  private static class Approvals {
    /**
     * CBM Approval status.
     */
    public final Boolean cbmApproved;

    /**
     * Daily-build status.
     */
    public final Boolean dailyBuildOk;

    /**
     * Helper constructor.
     */
    public Approvals(final Boolean cbmApproved, final Boolean dailyBuildOk) {
      this.cbmApproved = cbmApproved;
      this.dailyBuildOk = dailyBuildOk;
    }

    /**
     * Aggregated approval.
     */
    public boolean submittable() {
      return cbmApproved != null && dailyBuildOk != null && cbmApproved && dailyBuildOk;
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

  // cache containing the correct slug2title mapping
  private static final String SLUG_TO_TITLE_MAP = "/var/cache/slug2title";
  private static final int SLUG_TO_TITLE_MAX_AGE = 3600000;

  private static final String GERRIT_CONFIG_USERNAME = "username";
  private static final String GERRIT_CONFIG_PASSWORD = "password";
  private static final String GERRIT_CONFIG_URL = "url";

  private static final String FORMAT_COMMENT_REVIEW = "Gerrit Review %s for target: %s.";

  private static final String PATTERN_SHA1 = "[a-fA-F0-9]{40}";

  private static final Logger LOG = LoggerFactory.getLogger(TroubleItsFacade.class);

  private final SchemaFactory<ReviewDb> reviewDbProvider;
  private final GitRepositoryManager repoManager;

  private final TroubleWorkQueue workQueue;

  private final String apiUser;
  private final String apiPass;
  private final String baseApiUrl;
  private final String gerritUrl;

  private final Account apiUserAccount;

  private static HashMap<String, String> slugToTitleMap = new HashMap<String, String>();
  private static long slugToTitleRip = 0;

  /**
   * Constructor.
   */
  public TroubleItsFacade(final String pluginName, final PluginConfig cfg, final SchemaFactory<ReviewDb> schema,
      final GitRepositoryManager repoManager, final WorkQueue workQueue) {
    this.reviewDbProvider = schema;
    this.repoManager = repoManager;

    // Gerrit config
    String url = cfg.getString("gerritUrl");
    if (url == null) {
      throw new IllegalArgumentException("plugin." + pluginName + ".gerritUrl is not set in gerrit.config");
    }
    gerritUrl = url.replaceFirst("/+$", "");

    // Trouble API config
    url = cfg.getString("troubleUrl");
    assert  url != null;  // This needs to be checked earlier
    baseApiUrl =  url.replaceFirst("/+$", "");
    apiUser = cfg.getString("username");
    if (apiUser == null) {
      throw new IllegalArgumentException("plugin." + pluginName + ".username is not set in gerrit.config");
    }
    apiUserAccount = getApiUserAccount();
    if (apiUserAccount == null) {
      LOG.warn("plugin." + pluginName + "." + apiUser + " in gerrit.config is not valid Gerrit user");
    }
    apiPass = cfg.getString("password");
    if (apiPass == null) {
      throw new IllegalArgumentException("plugin." + pluginName + ".password is not set in secure.config");
    }
    // TroubleWorkQueue config
    int numThreads = cfg.getInt("poolSize", TroubleWorkQueue.DEFAULT_POOL_SIZE);
    int retryLimit = cfg.getInt("retryLimit", TroubleWorkQueue.DEFAULT_RETRY_LIMIT_SECONDS);
    this.workQueue = new TroubleWorkQueue(pluginName, workQueue, numThreads, retryLimit);
  }

  @Override
  public final void start() {
    LOG.info("starting up ...");
  }

  @Override
  public final void stop() {
    LOG.info("stopping ...");
    workQueue.shutdown();
  }

  @Override
  public final String name() {
    return "Trouble";
  }

  @Override
  public final void addComment(final String issueId, final String json) throws IOException {
    LOG.debug("addComment({},{})", issueId, json);

    // deserialize the VelocityComment
    GsonBuilder gson = new GsonBuilder();
    final VelocityComment event = gson.create().fromJson(json, VelocityComment.class).check(); // deserialize!

    // create the tsk to be executed in the work queue
    Callable<Void> task = new Callable<Void>() {
      public final Void call() throws Exception {
        handleAddComment(event);
        return null;
      }
      @Override
      public final String toString() {
        return json;
      }
    };

    // add the task to the work queueu
    workQueue.submit(event.ticket, task);
  }

  @Override
  public final boolean exists(final String issueId) throws IOException {
    LOG.debug("exists({})", issueId);
    return TroubleClient.create(baseApiUrl, apiUser, apiPass, parseIssueId(issueId)).ticketExists();
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
   * Handles the addComment event.
   */
  private void handleAddComment(final VelocityComment event) throws IOException {
    // create the TroubleClient
    String username = event.blame.indexOf('.') != -1 ? apiUser : event.blame;
    TroubleClient troubleClient = TroubleClient.create(baseApiUrl, apiUser, apiPass, event.ticket, username);

    // try find all package in the fix (same targetBranch)
    TroubleClient.Package[] packages = troubleClient.getPackages(event.branch);
    String projectName = slugToTitle(event.project);
    LOG.debug("resolved projectName: {}", projectName);
    TroubleClient.Package existingPackage = findPackage(packages, event.project);

    String comment = null;
    String targetLink = createCommentUrl(event.branch, projectName, event.change);
    if (existingPackage != null && event.id.equals("change-abandoned")) { // delete package
      troubleClient.deletePackageFromFix(event.branch, existingPackage.id);
      troubleClient.deletePackage(existingPackage.id);
      // create comment about the abandoned review
      comment = String.format(FORMAT_COMMENT_REVIEW, "ABANDONED", targetLink);
    } else if (event.id.equals("patchset-created") || event.id.equals("change-restored")) { // add or update package
      Approvals approvals = null;
      if (event.id.equals("patchset-created")) { // reset approvals
        // create comment about the new patchset
        comment = String.format(FORMAT_COMMENT_REVIEW, event.patchSet == 1 ? "STARTED" : "UPDATED", targetLink);
      } else { // change-restored
        // create comment about the restored review
        comment = String.format(FORMAT_COMMENT_REVIEW, "RESTORED", targetLink);
        approvals = resolveApprovals(event.patchSetId());
      }

      // create a new (untampered) package
      TroubleClient.Package newPackage = createPackage(projectName, event.branch, event.rev, event.ref, approvals);
      if (existingPackage != null) {
        newPackage.id = existingPackage.id;
      }
      newPackage = troubleClient.addOrUpdatePackage(newPackage); // create/update the new package
      if (event.patchSet == 1 || event.id.equals("change-restored")) {
        // create a fix and add the package to it, putting the ticket into the reviewing state.
        troubleClient.createOrUpdateFix(event.branch, newPackage.id);
      }
    } else if (existingPackage != null && event.id.equals("comment-added")) {
      // create a new (untampered) package
      Approvals approvals = resolveApprovals(event.patchSetId());
      TroubleClient.Package newPackage = new TroubleClient.Package();
      newPackage.id = existingPackage.id;

      if (event.blame.equals(apiUser) && approvals.submittable()) {
        // handle Gerrit's comment about the auto-tag
        newPackage.mergeRef = findMergeTag(event.patchSetId()); // get the merge tag from the Review comments
        LOG.debug("found merge tag: {}", newPackage.mergeRef);
        if (newPackage.mergeRef.equals(existingPackage.mergeRef)) {
          newPackage.mergeRef = null; // block the update (same tag)
        }
        newPackage.username = "zalanb"; // See ticket 61387
      } else { // handle approval related comments
        if (!existingPackage.cbmApproved.equals(approvals.cbmApproved)) {
          newPackage.cbmApproved = approvals.cbmApproved;
          LOG.debug("cbmApproved: {}", newPackage.cbmApproved);
        }
        if (!existingPackage.dailyBuildOk.equals(approvals.dailyBuildOk)) {
          newPackage.dailyBuildOk = approvals.dailyBuildOk;
          LOG.debug("dailyBuildOk: {}", newPackage.dailyBuildOk);
        }
      }

      // update the package
      if (newPackage.cbmApproved != null || newPackage.dailyBuildOk != null || newPackage.mergeRef != null) {
        newPackage = troubleClient.addOrUpdatePackage(newPackage); // create/update the new package
      }
    }

    if (comment != null) {
      troubleClient.addComment(comment);
    }
  }

  /**
   * Create comment URL.
   */
  private String createCommentUrl(final String targetBranch, final String troublePackage, final int change) {
    return String.format("\"%s@%s\":" + gerritUrl + "/#/c/%d/", targetBranch, troublePackage, change);
  }

  /**
   * Find a package.
   */
  private TroubleClient.Package findPackage(final TroubleClient.Package[] packages, final String slug) {
    for (TroubleClient.Package troublePackage : packages) {
      if (slug.equals(titleToSlug(troublePackage.name))) {
        return troublePackage; // bingo!
      }
    }
    return null;
  }

  /**
   * Invoked when a new PatchSet is created.
   */
  private TroubleClient.Package createPackage(final String name, final String targetBranch,
      final String revision, final String patchSetRef, final Approvals approvals) throws IOException {
    TroubleClient.Package troublePackage = new TroubleClient.Package();
    troublePackage.name = name;
    troublePackage.targetBranch = targetBranch;
    troublePackage.preMergeRef = new TroubleClient.Reference(patchSetRef);
    if (approvals != null) {
      troublePackage.cbmApproved = approvals.cbmApproved;
      troublePackage.dailyBuildOk = approvals.dailyBuildOk;
    }
    // resolve fix information
    List<String> referenceFooters = getReferenceFooters(repoManager, name, revision);
    for (String footer : referenceFooters) {
      troublePackage.fixBranch = parseSourceBranch(footer);
      troublePackage.fixRef = new TroubleClient.Reference(parseRevision(footer));
      break; // first footer is enough
    }
    return troublePackage;
  }

  /**
   * Resolves the effective approval status for a patch set.
   */
  private Approvals resolveApprovals(final PatchSet.Id patchSetId) {
    Boolean cbmApproved = null;
    Boolean dailyBuildOk = null;
    ReviewDb db = null;
    try {
      db = reviewDbProvider.open();
      for (PatchSetApproval approval : db.patchSetApprovals().byPatchSet(patchSetId)) {
        //LOG.debug("{}, {}", approval.getLabelId(), approval.getValue());
        if ("Code-Review".equals(approval.getLabelId().get())) {
          if (cbmApproved == null && approval.getValue() == CODE_REVIEW_MAX) {
            cbmApproved = true;
          } else if (approval.getValue() == CODE_REVIEW_MIN) {
            cbmApproved = false;
          }
        } else if ("Daily-Built".equals(approval.getLabelId().get())) {
          if (dailyBuildOk == null && approval.getValue() == DAILY_BUILT_MAX) {
            dailyBuildOk = true;
          } else if (approval.getValue() == DAILY_BUILT_MIN) {
            dailyBuildOk = false;
          }
        }
      }
      if (cbmApproved == null) {
        cbmApproved = false;
      }
      if (dailyBuildOk == null) {
        dailyBuildOk = false;
      }
    } catch (OrmException err) { // cbmApproved and dailyBuildOk is null => nothing is changed
      LOG.error("Failed to access ReviewDb: ", err.toString());
    } finally {
      if (db != null) {
        db.close();
      }
    }
    return new Approvals(cbmApproved, dailyBuildOk);
  }

  /**
   * Finds the R-Tag from the comments.
   */
  private TroubleClient.Reference findMergeTag(final PatchSet.Id patchSetId) {
    String tag = null;
    ReviewDb db = null;
    Account.Id accountId = apiUserAccount != null ? apiUserAccount.getId() : null;
    try {
      db = reviewDbProvider.open();
      Pattern pattern = Pattern.compile("Auto-tagged as (\\S+)$");
      for (ChangeMessage message : db.changeMessages().byPatchSet(patchSetId)) {
        //LOG.debug("{}", message.getMessage());
        if (accountId == null || accountId.equals(message.getAuthor())) {
          Matcher matcher = pattern.matcher(message.getMessage());
          if (matcher.find()) {
            tag = matcher.group(1);
          }
        }
      }
    } catch (OrmException err) { // cbmApproved and dailyBuildOk is null => nothing is changed
      LOG.error("Failed to access ReviewDb: ", err.toString());
    } finally {
      if (db != null) {
        db.close();
      }
    }
    return tag != null ? new TroubleClient.Reference(tag) : null;
  }

  /**
   * Finds the account id of a user.
   */
  private Account getApiUserAccount() {
    ReviewDb db = null;
    try {
      db = reviewDbProvider.open();
      for (Account account : db.accounts().byPreferredEmail(apiUser + "@axis.com")) {
        return account;
      }
    } catch (OrmException err) { // cbmApproved and dailyBuildOk is null => nothing is changed
      LOG.error("Failed to access ReviewDb: ", err.toString());
    } finally {
      if (db != null) {
        db.close();
      }
    }
    return null;
  }

  /**
   * Reads the reference footer from the repository.
   */
  private static List<String> getReferenceFooters(final GitRepositoryManager repoManager,
      final String packageName, final String rev) throws IOException {
    String slug = titleToSlug(packageName);
    Repository git = repoManager.openRepository(new Project.NameKey(slug));
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
      return branches[0]; // first match
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

  /**
   * Converts a title to slug notation.
   */
  private static String titleToSlug(final String title) {
    return title.toLowerCase().replaceFirst("[^a-z0-9_]+$", "").replaceAll("[^a-z0-9_]+", "-");
  }

  /**
   * Convert a slug to a title.
   *
   * The proper way to do this is to look in the slug2title mapping.
   * However, this is a best effort implementations so its not necessary
   * to have an exact conversion.
   */
  private static String slugToTitle(final String slug) {
    long now = System.currentTimeMillis();
    if (slugToTitleRip < now) { // reload file from disk periodically
      HashMap<String, String> newMap = new HashMap<String, String>();
      BufferedReader bufferedReader = null;
      try {
        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(SLUG_TO_TITLE_MAP), "utf-8"));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
          String[] keyVal = line.split("\\s+", 2);
          if (keyVal.length != 2) {
            LOG.warn("slug2title file is corrupted {}", keyVal.length);
          } else {
            newMap.put(keyVal[0], keyVal[1]);
          }
        }
        slugToTitleMap = newMap;
        slugToTitleRip = now + SLUG_TO_TITLE_MAX_AGE;
        LOG.debug("Loaded title2slug map from disk");
      } catch (IOException ioe) {
        LOG.warn("Failed to load title2slug map", ioe);
      } finally {
        if (bufferedReader != null) {
          try { bufferedReader.close(); } catch (IOException e) { /* ignore errors on close */ }
        }
      }
    }

    String title = slugToTitleMap.get(slug);
    return title != null ? title : slug;
  }

}
