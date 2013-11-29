package com.googlesource.gerrit.plugins.hooks.util;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssueExtractor {
  private static final Logger log = LoggerFactory.getLogger(
      IssueExtractor.class);

  private final Config gerritConfig;
  private final String pluginName;
  private final CommitMessageFetcher commitMessageFetcher;
  private final ReviewDb db;

  @Inject
  IssueExtractor(@GerritServerConfig Config gerritConfig,
      @PluginName String pluginName, CommitMessageFetcher commitMessageFetcher,
      ReviewDb db) {
    this.gerritConfig = gerritConfig;
    this.pluginName = pluginName;
    this.commitMessageFetcher = commitMessageFetcher;
    this.db = db;
  }

  /**
   * Gets issue ids from a string.
   *
   * @param haystack String to extract issue ids from
   * @return array of {@link Strings}. Each String being a found issue id.
   */
  public String[] getIssueIds(String haystack) {
    Pattern pattern = getPattern();
    if (pattern == null) return new String[] {};

    log.debug("Matching '" + haystack + "' against " + pattern.pattern());

    Set<String> issues = Sets.newHashSet();
    Matcher matcher = pattern.matcher(haystack);

    while (matcher.find()) {
      int groupIdx = Math.min(matcher.groupCount(), 1);
      issues.add(matcher.group(groupIdx));
    }

    return issues.toArray(new String[issues.size()]);
  }

  /**
   * Gets the regular expression used to identify issue ids.
   * @return the regular expression, or {@code null}, if there is no pattern
   *    to match issue ids.
   */
  public Pattern getPattern() {
    Pattern ret = null;
    String match = gerritConfig.getString("commentLink", pluginName, "match");
    if (match != null) {
      ret = Pattern.compile(match);
    }
    return ret;
  }

  /**
   * Helper funcion for {@link #getIssueIds(String, String)}.
   * <p>
   * Adds a text's issues for a given occurrence to the map returned by
   * {@link #getIssueIds(String, String)}.
   *
   * @param text The text to extract issues from.
   * @param occurrence The occurrence the issues get added at in {@code map}.
   * @param map The map that the issues should get added to.
   */
  private void addIssuesOccurrence(String text, String occurrence, Map<String,Set<String>> map) {
    for (String issue : getIssueIds(text)) {
      Set<String> occurrences = map.get(issue);
      if (occurrences == null) {
        occurrences = Sets.newLinkedHashSet();
        map.put(issue, occurrences);
      }
      occurrences.add(occurrence);
    }
  }

  /**
   * Gets issues for a commit.
   *
   * @param projectName The project to fetch {@code commitId} from.
   * @param commitId The commit id to fetch issues for.
   * @return A mapping, whose keys are issue ids and whose values is a set of
   *    places where the issue occurs. Each issue occurs at least in
   *    "somewhere". Issues from the first line get tagged with an occurrence
   *    "subject". Issues in the last block get tagged with "footer". Issues
   *    occurring between "subject" and "footer" get tagged with "body".
   */
  public Map<String,Set<String>> getIssueIds(String projectName,
      String commitId) {
    Map<String,Set<String>> ret = Maps.newHashMap();
    String commitMessage = commitMessageFetcher.fetchGuarded(projectName,
        commitId);

    addIssuesOccurrence(commitMessage, "somewhere", ret);

    String[] lines = commitMessage.split("\n");
    if (lines.length > 0) {
      // Parsing for "subject"
      addIssuesOccurrence(lines[0], "subject", ret);

      // Determining footer line numbers
      int currentLine = lines.length-1;
      while (currentLine >=0 && lines[currentLine].isEmpty()) {
        currentLine--;
      }
      int footerEnd = currentLine + 1;
      while (currentLine >=0 && !lines[currentLine].isEmpty()) {
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
        //body = String[] templateParameters =
          //  Arrays.copyOfRange(allParameters, 1, allParameters.length);
        if (lines.length > 0) {
          body = StringUtils.join(lines, "\n", 1, lines.length);
        }
      } else {
        body = StringUtils.join(lines, "\n", 1, footerStart - 1);

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
        footer = StringUtils.join(lines, "\n", footerStart, footerEnd);
      }
      if (body != null) {
        addIssuesOccurrence(body, "body", ret);
      }
      if (footer != null) {
        addIssuesOccurrence(footer, "footer", ret);
      }
    }
    return ret;
  }

  /**
   * Gets issues for a commit with new issue occurrences marked as "added".
   * <p>
   * Fetches the patch set's immediate ancestor and compares issue occurrences
   * between them. Any new occurrence gets marked as "added." So if for
   * example in patch sets 1, and 2 issue 23 occurs in the subject, while in
   * patch set the issue occurs in the body, then patch set 2 has occurrences
   * "somewhere", and "subject" for issue 23. Patch set 3 has occurrences
   * "somewhere", "body", and "body-added" for issue 23.
   *
   * @param projectName The project to fetch {@code commitId} from.
   * @param commitId The commit id to fetch issues for.
   * @param patchSetId The patch set for the {@code commitId}. If it is null,
   *    no occurrence can be marked as "-added".
   * @return A mapping, whose keys are issue ids and whose values is a set of
   *    places where the issue occurs. Each issue occurs at least in
   *    "somewhere". Issues from the first line get tagged with an occurrence
   *    "subject". Issues in the last block get tagged with "footer". Issues
   *    occurring between "subject" and "footer" get tagged with "body".
   */
  public Map<String,Set<String>> getIssueIds(String projectName,
      String commitId, PatchSet.Id patchSetId) {
    Map<String,Set<String>> current = getIssueIds(projectName, commitId);
    if (patchSetId != null) {
      Map<String,Set<String>> previous = Maps.newHashMap();
      if (patchSetId.get() != 1) {
        PatchSet.Id previousPatchSetId = new PatchSet.Id(
            patchSetId.getParentKey(), patchSetId.get() - 1);
        try {
          PatchSet previousPatchSet = db.patchSets().get(previousPatchSetId);
          if (previousPatchSet != null) {
            previous = getIssueIds(projectName,
                previousPatchSet.getRevision().get());
          }
        } catch (OrmException e) {
          // previous is still empty to indicate that there was no previous
          // accessible patch set. We treat every occurrence as added.
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
          currentOccurrences.add( "added@" + occurrence);
        }
      }
    }
    return current;
  }
}
