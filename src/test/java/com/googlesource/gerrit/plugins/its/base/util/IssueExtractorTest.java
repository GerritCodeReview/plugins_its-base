// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.its.base.util;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor.PatchSetDb;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class IssueExtractorTest extends LoggingMockingTestCase {
  private Injector injector;
  private ItsConfig itsConfig;
  private CommitMessageFetcher commitMessageFetcher;
  private PatchSetDb db;

  public void testIssueIdsNullPattern() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    String[] ret = issueExtractor.getIssueIds("Test");
    assertEquals("Number of found ids do not match", 0, ret.length);

    verifyOneOrMore(itsConfig).getIssuePattern();
  }

  public void testIssueIdsNoMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("Test");
    assertEquals("Number of found ids do not match", 0, ret.length);

    assertLogMessageContains("Matching");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsEmptyGroup() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(X*)(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("bug#4711");
    assertEquals("Number of found ids do not match", 0, ret.length);

    assertLogMessageContains("Matching");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsFullMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("bug#4711");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("First found issue id do not match", "4711", ret[0]);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "4711", ret[0]);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsGrouplessMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#\\d+"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(0);

    String[] ret = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "bug#4711", ret[0]);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsMultiGroupMatchGroup1() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d)(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "4", ret[0]);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsMultiGroupMatchGroup2() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d)(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(2);

    String[] ret = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "711", ret[0]);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsMulipleMatches() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("Foo bug#4711 bug#42 bar bug#123");
    assertEquals("Number of found ids do not match", 3, ret.length);
    List<String> retList = Arrays.asList(ret);
    assertTrue("4711 not among the extracted ids", retList.contains("4711"));
    assertTrue("42 not among the extracted ids", retList.contains("42"));
    assertTrue("123 not among the extracted ids", retList.contains("123"));

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched", 3); // #42, #123, #4711

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsMulipleMatchesWithDuplicates() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    String[] ret = issueExtractor.getIssueIds("Foo bug#4711 bug#42 bar\n" + "bug#123 baz bug#42");
    assertEquals("Number of found ids do not match", 3, ret.length);
    List<String> retList = Arrays.asList(ret);
    assertTrue("4711 not among the extracted ids", retList.contains("4711"));
    assertTrue("42 not among the extracted ids", retList.contains("42"));
    assertTrue("123 not among the extracted ids", retList.contains("123"));

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matched", 2); // #42
    assertLogMessageContains("Matched"); // #123
    assertLogMessageContains("Matched"); // #4711

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitSingleIssue() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 5);
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitMultipleIssues() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "bug#42, and bug#4711\n"
                + "\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("4711", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 5);
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #4711 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitMultipleIssuesMultipleTimes() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "bug#42, bug#4711, bug#4711, bug#42, and bug#4711\n"
                + "\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("4711", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 5);
    assertLogMessageContains("Matched", 4); // #42 -> 2x somewhere, 2x subject
    assertLogMessageContains("Matched", 6); // #42 -> 3x somewhere, 3x subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitSingleIssueBody() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject does not reference a bug\n"
                + "Body references bug#42\n"
                + "\n"
                + "Footer: does not reference a bug\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, body

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitSingleIssueFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject does not reference a bug\n"
                + "Body does not reference a bug\n"
                + "\n"
                + "Footer: references bug#42\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "footer", "footer-Footer"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 3); // #42 -> somewhere, footer, footer-Footer

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitMultipleIssuesFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject does not reference a bug\n"
                + "Body does not reference a bug\n"
                + "\n"
                + "KeyA: references bug#42\n"
                + "KeyB: does not reference bug\n"
                + "KeyC: references bug#176\n"
                + "Unkeyed reference to bug#4711\n"
                + "Change-Id: I1234567891123456789212345678931234567894\n"
                + "KeyZ: references bug#256");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "footer", "footer-KeyA"));
    expected.put("176", Sets.newHashSet("somewhere", "footer", "footer-KeyC"));
    expected.put("256", Sets.newHashSet("somewhere", "footer", "footer-KeyZ"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 9);
    assertLogMessageContains("Matched", 3); // #42 -> somewhere, footer, footer-KeyA
    assertLogMessageContains("Matched", 3); // #176 -> somewhere, footer, footer-KeyC
    assertLogMessageContains("Matched", 3); // #256 -> somewhere, footer, footer-KeyZ
    assertLogMessageContains("Matched", 2); // #4711 -> somewhere, footer

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentParts() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject references bug#42.\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.\n"
                + "\n"
                + "Bug: bug#4711 in footer\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body
    assertLogMessageContains("Matched", 3); // #4711 -> somewhere, footer, footer-Bug

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentPartsEmptySubject() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.\n"
                + "\n"
                + "Bug: bug#4711 in footer\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body
    assertLogMessageContains("Matched", 3); // #4711 -> somewhere, footer, footer-Bug

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentPartsLinePastFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject references bug#42.\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.\n"
                + "\n"
                + "Bug: bug#4711 in footer\n"
                + "Change-Id: I1234567891123456789212345678931234567894\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body
    assertLogMessageContains("Matched", 3); // #4711 -> somewhere, footer, footer-Bug

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentPartsLinesPastFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject references bug#42.\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.\n"
                + "\n"
                + "Bug: bug#4711 in footer\n"
                + "Change-Id: I1234567891123456789212345678931234567894\n"
                + "\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body
    assertLogMessageContains("Matched", 3); // #4711 -> somewhere, footer, footer-Bug

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentPartsNoFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject references bug#42.\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 3);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentPartsNoFooterTrailingLine() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject references bug#42.\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 3);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitDifferentPartsNoFooterTrailingLines() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject references bug#42.\n"
                + "Body references bug#16.\n"
                + "Body also references bug#176.\n"
                + "\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 3);
    assertLogMessageContains("Matched", 2); // #16 -> somewhere, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // #176 -> somewhere, body

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitEmpty() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 3);

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitBlankLine() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitBlankLines() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("\n\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitMoreBlankLines() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("\n\n\n");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitMixed() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "Subject bug#42, bug#1984, and bug#16\n"
                + "\n"
                + "bug#4711 in body,\n"
                + "along with bug#1984, and bug#5150.\n"
                + "bug#4711 in body again, along with bug#16\n"
                + "\n"
                + "Bug: bug#176, bug#1984, and bug#5150\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds("testProject", "1234567891123456789212345678931234567894");

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "subject", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    expected.put("1984", Sets.newHashSet("somewhere", "subject", "body", "footer", "footer-Bug"));
    expected.put("4711", Sets.newHashSet("somewhere", "body"));
    expected.put("5150", Sets.newHashSet("somewhere", "body", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 6);
    assertLogMessageContains("Matched", 3); // #16 -> somewhere, subject, body
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject
    assertLogMessageContains("Matched", 3); // #176 -> somewhere, footer, footer-Bug
    assertLogMessageContains(
        "Matched", 8); // #1984 -> 3xsomewhere, subject, 2xbody, footer, footer-Bug
    assertLogMessageContains("Matched", 4); // #4711 -> 2xsomewhere, 2xbody
    assertLogMessageContains("Matched", 5); // #5150 -> 2xsomewhere, body, footer, footer-Bug

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedEmptyFirst() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("");

    PatchSet.Id patchSetId = PatchSet.id(Change.id(4), 1);
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", patchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 3);

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueFirst() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I1234567891123456789212345678931234567894");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(1);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject", "added@somewhere", "added@subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 5);
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondEmpty() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    // Call for current patch set
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = PatchSet.id(changeId, 1);
    when(db.getRevision(previousPatchSetId)).thenReturn("9876543211987654321298765432139876543214");

    when(commitMessageFetcher.fetchGuarded(
            "testProject", "9876543211987654321298765432139876543214"))
        .thenReturn("subject\n" + "\n" + "Change-Id: I9876543211987654321298765432139876543214");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(2);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject", "added@somewhere", "added@subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 10);
    assertLogMessageContains("Matched", 2); // #42 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondSame() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    // Call for current patch set
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = PatchSet.id(changeId, 1);
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "9876543211987654321298765432139876543214"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I9876543211987654321298765432139876543214");

    when(db.getRevision(previousPatchSetId)).thenReturn("9876543211987654321298765432139876543214");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(2);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 10);
    assertLogMessageContains("Matched", 2); // Current PS: #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // Previous PS: #42 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondBody() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    // Call for current patch set
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = PatchSet.id(changeId, 1);
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "9876543211987654321298765432139876543214"))
        .thenReturn(
            "subject\n"
                + "bug#42\n"
                + "\n"
                + "Change-Id: I9876543211987654321298765432139876543214");

    when(db.getRevision(previousPatchSetId)).thenReturn("9876543211987654321298765432139876543214");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(2);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject", "added@subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 10);
    assertLogMessageContains("Matched", 2); // Current PS: #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // Previous PS: #42 -> somewhere, body

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    // Call for current patch set
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "subject\n"
                + "\n"
                + "Bug: bug#42\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = PatchSet.id(changeId, 1);
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "9876543211987654321298765432139876543214"))
        .thenReturn("bug#42\n" + "\n" + "Change-Id: I9876543211987654321298765432139876543214");

    when(db.getRevision(previousPatchSetId)).thenReturn("9876543211987654321298765432139876543214");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(2);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put(
        "42",
        Sets.newHashSet("somewhere", "footer", "added@footer", "footer-Bug", "added@footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 11);
    assertLogMessageContains("Matched", 3); // Current PS: #42 -> somewhere, footer, footer-Bug
    assertLogMessageContains("Matched", 2); // Previous PS: #42 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedSubjectFooter() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    // Call for current patch set
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "subject bug#42\n"
                + "\n"
                + "body bug#42\n"
                + "\n"
                + "Bug: bug#42\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = PatchSet.id(changeId, 1);
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "9876543211987654321298765432139876543214"))
        .thenReturn(
            "subject\n"
                + "bug#42\n"
                + "\n"
                + "Change-Id: I9876543211987654321298765432139876543214");

    when(db.getRevision(previousPatchSetId)).thenReturn("9876543211987654321298765432139876543214");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(2);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put(
        "42",
        Sets.newHashSet(
            "somewhere",
            "subject",
            "added@subject",
            "body",
            "footer",
            "added@footer",
            "footer-Bug",
            "added@footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 11);
    assertLogMessageContains(
        "Matched", 7); // Current PS: 3x somewhere, subject, body, footer, footer-Bug
    assertLogMessageContains("Matched", 2); // Previous PS: somewhere, body

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  public void testIssueIdsCommitWAddedMultiple() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    when(itsConfig.getIssuePatternGroupIndex()).thenReturn(1);

    Change.Id changeId = mock(Change.Id.class);

    // Call for current patch set
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "1234567891123456789212345678931234567894"))
        .thenReturn(
            "subject bug#42\n"
                + "\n"
                + "body bug#42 bug#16\n"
                + "\n"
                + "Bug: bug#42\n"
                + "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = PatchSet.id(changeId, 1);
    when(commitMessageFetcher.fetchGuarded(
            "testProject", "9876543211987654321298765432139876543214"))
        .thenReturn(
            "subject\n"
                + "bug#42 bug#4711\n"
                + "\n"
                + "Bug: bug#16\n"
                + "Change-Id: I9876543211987654321298765432139876543214");

    when(db.getRevision(previousPatchSetId)).thenReturn("9876543211987654321298765432139876543214");

    PatchSet.Id currentPatchSetId = mock(PatchSet.Id.class);
    when(currentPatchSetId.get()).thenReturn(2);
    when(currentPatchSetId.changeId()).thenReturn(changeId);

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String, Set<String>> actual =
        issueExtractor.getIssueIds(
            "testProject", "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String, Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body", "added@body"));
    expected.put(
        "42",
        Sets.newHashSet(
            "somewhere",
            "subject",
            "added@subject",
            "body",
            "footer",
            "added@footer",
            "footer-Bug",
            "added@footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching", 12);
    assertLogMessageContains("Matched", 2); // Current PS: #16 -> somewhere, body
    assertLogMessageContains(
        "Matched", 7); // Current PS: #42 -> 3xsomewhere, subject, body, footer, footer-Bug
    assertLogMessageContains("Matched", 3); // Previous PS: #16 -> somewhere, footer, footer-Bug
    assertLogMessageContains("Matched", 2); // Previous PS: #42 -> somewhere, subject
    assertLogMessageContains("Matched", 2); // Previous PS: #4711 -> somewhere, subject

    verifyOneOrMore(itsConfig).getIssuePattern();
    verifyOneOrMore(itsConfig).getIssuePatternGroupIndex();
  }

  private <T> T verifyOneOrMore(T mock) {
    return verify(mock, atLeastOnce());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      itsConfig = mock(ItsConfig.class);
      bind(ItsConfig.class).toInstance(itsConfig);

      commitMessageFetcher = mock(CommitMessageFetcher.class);
      bind(CommitMessageFetcher.class).toInstance(commitMessageFetcher);

      db = mock(PatchSetDb.class);
      bind(PatchSetDb.class).toInstance(db);
    }
  }
}
