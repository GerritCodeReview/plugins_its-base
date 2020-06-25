package com.googlesource.gerrit.plugins.its.base.util;

import static java.util.Arrays.copyOfRange;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueExtractor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final CommitMessageFetcher commitMessageFetcher;
  private final PatchSetDb db;
  private final ItsConfig itsConfig;

  @ImplementedBy(PatchSetDbImpl.class)
  public interface PatchSetDb {
    public String getRevision(PatchSet.Id patchSetId);
  }

  public static class PatchSetDbImpl implements PatchSetDb {
    private final GerritApi gApi;

    @Inject
    public PatchSetDbImpl(GerritApi gApi) {
      this.gApi = gApi;
    }

    @Override
    public String getRevision(PatchSet.Id patchSetId) {
      try {
        ChangeInfo info =
            gApi.changes()
                .id(patchSetId.changeId().get())
                .get(EnumSet.of(ListChangesOption.ALL_REVISIONS));
        for (Map.Entry<String, RevisionInfo> e : info.revisions.entrySet()) {
          if (e.getValue()._number == patchSetId.get()) {
            return e.getKey();
          }
        }
        return null;
      } catch (RestApiException e) {
        // previous is still empty to indicate that there was no previous
        // accessible patch set. We treat every occurrence as added.
      }
      return null;
    }
  }

  @Inject
  IssueExtractor(ItsConfig itsConfig, CommitMessageFetcher commitMessageFetcher, PatchSetDb db) {
    this.commitMessageFetcher = commitMessageFetcher;
    this.db = db;
    this.itsConfig = itsConfig;
  }

  /**
   * Gets issue ids from a string.
   *
   * @param haystack String to extract issue ids from
   * @return array of {@link String}. Each String being a found issue id.
   */
  public String[] getIssueIds(String haystack) {
    Pattern pattern = itsConfig.getIssuePattern();
    if (pattern == null) return new String[] {};

    logger.atFine().log("Matching '%s' against '%s'", haystack, pattern.pattern());

    Set<String> issues = Sets.newHashSet();
    Matcher matcher = pattern.matcher(haystack);

    int groupIdx = itsConfig.getIssuePatternGroupIndex();
    while (matcher.find()) {
      String issueId = matcher.group(groupIdx);
      if (!Strings.isNullOrEmpty(issueId)) {
        issues.add(issueId);
      }
    }

    return issues.toArray(new String[issues.size()]);
  }

  /**
   * Helper function for {@link #getIssueIds(String, String)}.
   *
   * <p>Adds a text's issues for a given occurrence to the map returned by {@link
   * #getIssueIds(String, String)}.
   *
   * @param text The text to extract issues from.
   * @param occurrence The occurrence the issues get added at in {@code map}.
   * @param map The map that the issues should get added to.
   */
  private void addIssuesOccurrence(String text, String occurrence, Map<String, Set<String>> map) {
    for (String issue : getIssueIds(text)) {
      Set<String> occurrences = map.computeIfAbsent(issue, k -> Sets.newLinkedHashSet());
      occurrences.add(occurrence);
    }
  }

  /**
   * Gets issues for a commit.
   *
   * @param projectName The project to fetch {@code commitId} from.
   * @param commitId The commit id to fetch issues for.
   * @return A mapping, whose keys are issue ids and whose values is a set of places where the issue
   *     occurs. Each issue occurs at least in "somewhere". Issues from the first line get tagged
   *     with an occurrence "subject". Issues in the last block get tagged with "footer". Issues
   *     occurring between "subject" and "footer" get tagged with "body".
   */
  public Map<String, Set<String>> getIssueIds(String projectName, String commitId) {
    Map<String, Set<String>> ret = Maps.newHashMap();
    String commitMessage = commitMessageFetcher.fetchGuarded(projectName, commitId);
    addIssueIdsFromCommitMessage(ret, commitMessage);
    return ret;
  }

  /**
   * Gets issues from a commit message.
   *
   * @param commitMessage The commit message string.
   * @return A mapping, whose keys are issue ids and whose values is a set of places where the issue
   *     occurs. Each issue occurs at least in "somewhere". Issues from the first line get tagged
   *     with an occurrence "subject". Issues in the last block get tagged with "footer". Issues
   *     occurring between "subject" and "footer" get tagged with "body".
   */
  public Map<String, Set<String>> getIssueIdsFromCommitMessage(String commitMessage) {
    Map<String, Set<String>> ret = Maps.newHashMap();
    addIssueIdsFromCommitMessage(ret, commitMessage);
    return ret;
  }

  private void addIssueIdsFromCommitMessage(Map<String, Set<String>> ret, String commitMessage) {
    addIssuesOccurrence(commitMessage, "somewhere", ret);

    String[] lines = commitMessage.split("\n");
    if (lines.length > 0) {
      // Parsing for "subject"
      addIssuesOccurrence(lines[0], "subject", ret);

      // Determining footer line numbers
      int currentLine = lines.length - 1;
      while (currentLine >= 0 && lines[currentLine].isEmpty()) {
        currentLine--;
      }
      int footerEnd = currentLine + 1;
      while (currentLine >= 0 && !lines[currentLine].isEmpty()) {
        currentLine--;
      }
      int footerStart = currentLine + 1;

      if (footerStart == 0) {
        // The first block of non-blank lines is not considered a footer, so
        // we adjust that.
        footerStart = -1;
      }

      // Parsing for "body", and "footer"
      String body = null;
      String footer = null;
      if (footerStart == -1) {
        // No footer could be found. So all lines after the first one (that's
        // the subject) is the body.
        if (lines.length > 0) {
          body = String.join("\n", copyOfRange(lines, 1, lines.length));
        }
      } else {
        body = String.join("\n", copyOfRange(lines, 1, footerStart - 1));

        StringBuilder footerBuilder = new StringBuilder();
        for (int lineIdx = footerStart; lineIdx < footerEnd; lineIdx++) {
          String line = lines[lineIdx];

          // Adding occurrences for footer keys
          int colonIdx = line.indexOf(':');
          if (colonIdx > 0) {
            // tag of length at least 1
            String tag = line.substring(0, colonIdx);
            addIssuesOccurrence(line, "footer-" + tag, ret);
          }

          // Putting back together the footer to a single String
          if (lineIdx > footerStart) {
            footerBuilder.append('\n');
          }
          footerBuilder.append(line);
        }
        footer = String.join("\n", copyOfRange(lines, footerStart, footerEnd));
      }
      if (body != null) {
        addIssuesOccurrence(body, "body", ret);
      }
      if (footer != null) {
        addIssuesOccurrence(footer, "footer", ret);
      }
    }
  }

  /**
   * Gets issues for a commit with new issue occurrences marked as "added".
   *
   * <p>Fetches the patch set's immediate ancestor and compares issue occurrences between them. Any
   * new occurrence gets marked as "added." So if for example in patch sets 1, and 2 issue 23 occurs
   * in the subject, while in patch set the issue occurs in the body, then patch set 2 has
   * occurrences "somewhere", and "subject" for issue 23. Patch set 3 has occurrences "somewhere",
   * "body", and "body-added" for issue 23.
   *
   * @param projectName The project to fetch {@code commitId} from.
   * @param commitId The commit id to fetch issues for.
   * @param patchSetId The patch set for the {@code commitId}. If it is null, no occurrence can be
   *     marked as "-added".
   * @return A mapping, whose keys are issue ids and whose values is a set of places where the issue
   *     occurs. Each issue occurs at least in "somewhere". Issues from the first line get tagged
   *     with an occurrence "subject". Issues in the last block get tagged with "footer". Issues
   *     occurring between "subject" and "footer" get tagged with "body".
   */
  public Map<String, Set<String>> getIssueIds(
      String projectName, String commitId, PatchSet.Id patchSetId) {
    Map<String, Set<String>> current = getIssueIds(projectName, commitId);
    if (patchSetId != null) {
      Map<String, Set<String>> previous = Maps.newHashMap();
      if (patchSetId.get() != 1) {
        PatchSet.Id previousPatchSetId = PatchSet.id(patchSetId.changeId(), patchSetId.get() - 1);
        String previousPatchSet = db.getRevision(previousPatchSetId);
        if (previousPatchSet != null) {
          previous = getIssueIds(projectName, previousPatchSet);
        }
      }

      for (String issue : current.keySet()) {
        Set<String> currentOccurrences = current.get(issue);
        Set<String> previousOccurrences = previous.get(issue);
        Set<String> newOccurrences;
        if (previousOccurrences == null || previousOccurrences.isEmpty()) {
          newOccurrences = Sets.newHashSet(currentOccurrences);
        } else {
          newOccurrences = Sets.newHashSet(currentOccurrences);
          newOccurrences.removeAll(previousOccurrences);
        }
        for (String occurrence : newOccurrences) {
          currentOccurrences.add("added@" + occurrence);
        }
      }
    }
    return current;
  }
}
