package com.googlesource.gerrit.plugins.hooks.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsName;

import org.eclipse.jgit.lib.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssueExtractor {
  private static final Logger log = LoggerFactory.getLogger(
      IssueExtractor.class);

  @Inject @GerritServerConfig
  private Config gerritConfig;

  @Inject @ItsName
  private String itsName;

  /**
   * Gets issue ids from a string.
   *
   * @param haystack String to extract issue ids from
   * @return array of {@link Strings}. Each String being a found issue id.
   */
  public String[] getIssueIds(String haystack) {
    List<Pattern> commentRegexList = getCommentRegexList();
    if (commentRegexList == null) return new String[] {};

    log.debug("Matching '" + haystack + "' against " + commentRegexList);

    ArrayList<String> issues = new ArrayList<String>();
    for (Pattern pattern : commentRegexList) {
      Matcher matcher = pattern.matcher(haystack);

      while (matcher.find()) {
        String issueId = extractMatchedWorkItems(matcher);
        if (issueId != null) {
          issues.add(issueId);
        }
      }
    }

    return issues.toArray(new String[issues.size()]);
  }

  public String extractMatchedWorkItems(Matcher matcher) {
    int groupCount = matcher.groupCount();
    if (groupCount >= 1)
      return matcher.group(1);
    else
      return null;
  }

  /**
   * Gets the regular expression used to identify issue ids.
   * @return the regular expression, or {@code null}, if there is no pattern
   *    to match issue ids.
   */
  public Pattern getPattern() {
    Pattern ret = null;
    String match = gerritConfig.getString("commentLink", itsName, "match");
    if (match != null) {
      ret = Pattern.compile(match);
    }
    return ret;
  }

  private List<Pattern> getCommentRegexList() {
    ArrayList<Pattern> regexList = new ArrayList<Pattern>();

    Pattern pattern = getPattern();
    if (pattern != null) {
      regexList.add(pattern);
    }

    return regexList;
  }
}
