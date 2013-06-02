package com.googlesource.gerrit.plugins.hooks.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsName;

import org.eclipse.jgit.lib.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssueExtractor {
  private static final Logger log = LoggerFactory.getLogger(
      IssueExtractor.class);

  private final Config gerritConfig;
  private final String itsName;

  @Inject
  IssueExtractor(@GerritServerConfig Config gerritConfig,
      @ItsName String itsName) {
    this.gerritConfig = gerritConfig;
    this.itsName = itsName;
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
    String match = gerritConfig.getString("commentLink", itsName, "match");
    if (match != null) {
      ret = Pattern.compile(match);
    }
    return ret;
  }
}
