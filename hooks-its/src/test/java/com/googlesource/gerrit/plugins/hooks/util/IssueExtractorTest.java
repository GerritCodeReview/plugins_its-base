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
package com.googlesource.gerrit.plugins.hooks.util;

import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Config;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.RevId;
import com.google.gerrit.reviewdb.server.PatchSetAccess;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PatchSet.class,RevId.class})
public class IssueExtractorTest extends LoggingMockingTestCase {
  private Injector injector;
  private Config serverConfig;
  private CommitMessageFetcher commitMessageFetcher;
  private ReviewDb db;

  public void testPatternNullMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn(null).atLeastOnce();

    replayMocks();

    assertNull("Pattern for null match is not null",
        issueExtractor.getPattern());
  }

  public void testPattern() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("TestPattern").atLeastOnce();

    replayMocks();

    assertEquals("Expected and generated pattern are not equal",
        "TestPattern", issueExtractor.getPattern().pattern());
  }

  public void testIssueIdsNullPattern() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);

    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn(null).atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Test");
    assertEquals("Number of found ids do not match", 0, ret.length);
  }

  public void testIssueIdsNoMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#(\\d+)").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Test");
    assertEquals("Number of found ids do not match", 0, ret.length);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsFullMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#(\\d+)").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("bug#4711");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("First found issue id do not match", "4711", ret[0]);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#(\\d+)").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "4711", ret[0]);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsGrouplessMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#\\d+").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "bug#4711", ret[0]);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsMultiGroupMatch() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#(\\d)(\\d+)").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Foo bug#4711 bar");
    assertEquals("Number of found ids do not match", 1, ret.length);
    assertEquals("Found issue id does not match", "4", ret[0]);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsMulipleMatches() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#(\\d+)").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Foo bug#4711 bug#42 bar bug#123");
    assertEquals("Number of found ids do not match", 3, ret.length);
    List<String> retList = Arrays.asList(ret);
    assertTrue("4711 not among the extracted ids", retList.contains("4711"));
    assertTrue("42 not among the extracted ids", retList.contains("42"));
    assertTrue("123 not among the extracted ids", retList.contains("123"));

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsMulipleMatchesWithDuplicates() {
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("bug#(\\d+)").atLeastOnce();

    replayMocks();

    String ret[] = issueExtractor.getIssueIds("Foo bug#4711 bug#42 bar\n" +
        "bug#123 baz bug#42");
    assertEquals("Number of found ids do not match", 3, ret.length);
    List<String> retList = Arrays.asList(ret);
    assertTrue("4711 not among the extracted ids", retList.contains("4711"));
    assertTrue("42 not among the extracted ids", retList.contains("42"));
    assertTrue("123 not among the extracted ids", retList.contains("123"));

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitSingleIssue() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitMultipleIssues() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42, and bug#4711\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("4711", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitMultipleIssuesMultipleTimes() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42, bug#4711, bug#4711, bug#42, and bug#4711\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("4711", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitSingleIssueBody() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject does not reference a bug\n" +
            "Body references bug#42\n" +
            "\n" +
            "Footer: does not reference a bug\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitSingleIssueFooter() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject does not reference a bug\n" +
            "Body does not reference a bug\n" +
            "\n" +
            "Footer: references bug#42\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "footer",
        "footer-Footer"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitMultipleIssuesFooter() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject does not reference a bug\n" +
            "Body does not reference a bug\n" +
            "\n" +
            "KeyA: references bug#42\n" +
            "KeyB: does not reference bug\n" +
            "KeyC: references bug#176\n" +
            "Unkeyed reference to bug#4711\n" +
            "Change-Id: I1234567891123456789212345678931234567894\n" +
            "KeyZ: references bug#256" );

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "footer", "footer-KeyA"));
    expected.put("176", Sets.newHashSet("somewhere", "footer", "footer-KeyC"));
    expected.put("256", Sets.newHashSet("somewhere", "footer", "footer-KeyZ"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentParts() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject references bug#42.\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.\n" +
            "\n" +
            "Bug: bug#4711 in footer\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentPartsEmptySubject() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.\n" +
            "\n" +
            "Bug: bug#4711 in footer\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentPartsLinePastFooter() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject references bug#42.\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.\n" +
            "\n" +
            "Bug: bug#4711 in footer\n" +
            "Change-Id: I1234567891123456789212345678931234567894\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentPartsLinesPastFooter() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject references bug#42.\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.\n" +
            "\n" +
            "Bug: bug#4711 in footer\n" +
            "Change-Id: I1234567891123456789212345678931234567894\n" +
            "\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    expected.put("4711", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentPartsNoFooter() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject references bug#42.\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentPartsNoFooterTrailingLine() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject references bug#42.\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitDifferentPartsNoFooterTrailingLines() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject references bug#42.\n" +
            "Body references bug#16.\n" +
            "Body also references bug#176.\n" +
            "\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "body"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitEmpty() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn("");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitBlankLine() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn("\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitBlankLines() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn("\n\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitMoreBlankLines() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn("\n\n\n");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitMixed() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "Subject bug#42, bug#1984, and bug#16\n" +
            "\n" +
            "bug#4711 in body,\n" +
            "along with bug#1984, and bug#5150.\n" +
            "bug#4711 in body again, along with bug#16\n" +
            "\n" +
            "Bug: bug#176, bug#1984, and bug#5150\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894");

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "subject", "body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    expected.put("176", Sets.newHashSet("somewhere", "footer", "footer-Bug"));
    expected.put("1984", Sets.newHashSet("somewhere", "subject", "body",
        "footer", "footer-Bug"));
    expected.put("4711", Sets.newHashSet("somewhere", "body"));
    expected.put("5150", Sets.newHashSet("somewhere", "body", "footer",
        "footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedEmptyFirst() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn("");

    replayMocks();

    PatchSet.Id patchSetId = new PatchSet.Id(new Change.Id(4), 1);
    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", patchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueFirst() {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(1).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject",
        "added@somewhere", "added@subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondEmpty()
      throws OrmException {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    // Call for current patch set
    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = new PatchSet.Id(changeId, 1);
    RevId previousRevId = createMock(RevId.class);
    expect(previousRevId.get())
        .andReturn("9876543211987654321298765432139876543214").anyTimes();

    PatchSet previousPatchSet = createMock(PatchSet.class);
    expect(previousPatchSet.getRevision()).andReturn(previousRevId).anyTimes();

    PatchSetAccess patchSetAccess = createMock(PatchSetAccess.class);
    expect(patchSetAccess.get(previousPatchSetId)).andReturn(previousPatchSet);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "9876543211987654321298765432139876543214")).andReturn(
            "subject\n" +
            "\n" +
            "Change-Id: I9876543211987654321298765432139876543214");

    expect(db.patchSets()).andReturn(patchSetAccess);

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(2).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject",
        "added@somewhere", "added@subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondSame()
      throws OrmException {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    // Call for current patch set
    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = new PatchSet.Id(changeId, 1);
    RevId previousRevId = createMock(RevId.class);
    expect(previousRevId.get())
        .andReturn("9876543211987654321298765432139876543214").anyTimes();

    PatchSet previousPatchSet = createMock(PatchSet.class);
    expect(previousPatchSet.getRevision()).andReturn(previousRevId).anyTimes();

    PatchSetAccess patchSetAccess = createMock(PatchSetAccess.class);
    expect(patchSetAccess.get(previousPatchSetId)).andReturn(previousPatchSet);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "9876543211987654321298765432139876543214")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I9876543211987654321298765432139876543214");

    expect(db.patchSets()).andReturn(patchSetAccess);

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(2).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondBody()
      throws OrmException {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    // Call for current patch set
    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = new PatchSet.Id(changeId, 1);
    RevId previousRevId = createMock(RevId.class);
    expect(previousRevId.get())
        .andReturn("9876543211987654321298765432139876543214").anyTimes();

    PatchSet previousPatchSet = createMock(PatchSet.class);
    expect(previousPatchSet.getRevision()).andReturn(previousRevId).anyTimes();

    PatchSetAccess patchSetAccess = createMock(PatchSetAccess.class);
    expect(patchSetAccess.get(previousPatchSetId)).andReturn(previousPatchSet);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "9876543211987654321298765432139876543214")).andReturn(
            "subject\n" +
            "bug#42\n" +
            "\n" +
            "Change-Id: I9876543211987654321298765432139876543214");

    expect(db.patchSets()).andReturn(patchSetAccess);

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(2).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject", "added@subject"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedSingleSubjectIssueSecondFooter()
      throws OrmException {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    // Call for current patch set
    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "subject\n" +
            "\n" +
            "Bug: bug#42\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = new PatchSet.Id(changeId, 1);
    RevId previousRevId = createMock(RevId.class);
    expect(previousRevId.get())
        .andReturn("9876543211987654321298765432139876543214").anyTimes();

    PatchSet previousPatchSet = createMock(PatchSet.class);
    expect(previousPatchSet.getRevision()).andReturn(previousRevId).anyTimes();

    PatchSetAccess patchSetAccess = createMock(PatchSetAccess.class);
    expect(patchSetAccess.get(previousPatchSetId)).andReturn(previousPatchSet);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "9876543211987654321298765432139876543214")).andReturn(
            "bug#42\n" +
            "\n" +
            "Change-Id: I9876543211987654321298765432139876543214");

    expect(db.patchSets()).andReturn(patchSetAccess);

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(2).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "footer", "added@footer",
        "footer-Bug", "added@footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedSubjectFooter()
      throws OrmException {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    // Call for current patch set
    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "subject bug#42\n" +
            "\n" +
            "body bug#42\n" +
            "\n" +
            "Bug: bug#42\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = new PatchSet.Id(changeId, 1);
    RevId previousRevId = createMock(RevId.class);
    expect(previousRevId.get())
        .andReturn("9876543211987654321298765432139876543214").anyTimes();

    PatchSet previousPatchSet = createMock(PatchSet.class);
    expect(previousPatchSet.getRevision()).andReturn(previousRevId).anyTimes();

    PatchSetAccess patchSetAccess = createMock(PatchSetAccess.class);
    expect(patchSetAccess.get(previousPatchSetId)).andReturn(previousPatchSet);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "9876543211987654321298765432139876543214")).andReturn(
            "subject\n" +
            "bug#42\n" +
            "\n" +
            "Change-Id: I9876543211987654321298765432139876543214");

    expect(db.patchSets()).andReturn(patchSetAccess);

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(2).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("42", Sets.newHashSet("somewhere", "subject", "added@subject",
        "body", "footer", "added@footer", "footer-Bug",
        "added@footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  public void testIssueIdsCommitWAddedMultiple()
      throws OrmException {
    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
    .andReturn("bug#(\\d+)").atLeastOnce();

    Change.Id changeId = createMock(Change.Id.class);

    // Call for current patch set
    expect(commitMessageFetcher.fetchGuarded("testProject",
        "1234567891123456789212345678931234567894")).andReturn(
            "subject bug#42\n" +
            "\n" +
            "body bug#42 bug#16\n" +
            "\n" +
            "Bug: bug#42\n" +
            "Change-Id: I1234567891123456789212345678931234567894");

    // Call for previous patch set
    PatchSet.Id previousPatchSetId = new PatchSet.Id(changeId, 1);
    RevId previousRevId = createMock(RevId.class);
    expect(previousRevId.get())
        .andReturn("9876543211987654321298765432139876543214").anyTimes();

    PatchSet previousPatchSet = createMock(PatchSet.class);
    expect(previousPatchSet.getRevision()).andReturn(previousRevId).anyTimes();

    PatchSetAccess patchSetAccess = createMock(PatchSetAccess.class);
    expect(patchSetAccess.get(previousPatchSetId)).andReturn(previousPatchSet);

    expect(commitMessageFetcher.fetchGuarded("testProject",
        "9876543211987654321298765432139876543214")).andReturn(
            "subject\n" +
            "bug#42 bug#4711\n" +
            "\n" +
            "Bug: bug#16\n" +
            "Change-Id: I9876543211987654321298765432139876543214");

    expect(db.patchSets()).andReturn(patchSetAccess);

    PatchSet.Id currentPatchSetId = createMock(PatchSet.Id.class);
    expect(currentPatchSetId.get()).andReturn(2).anyTimes();
    expect(currentPatchSetId.getParentKey()).andReturn(changeId)
        .anyTimes();

    replayMocks();

    IssueExtractor issueExtractor = injector.getInstance(IssueExtractor.class);
    Map<String,Set<String>> actual = issueExtractor.getIssueIds("testProject",
        "1234567891123456789212345678931234567894", currentPatchSetId);

    Map<String,Set<String>> expected = Maps.newHashMap();
    expected.put("16", Sets.newHashSet("somewhere", "body", "added@body"));
    expected.put("42", Sets.newHashSet("somewhere", "subject", "added@subject",
        "body", "footer", "added@footer", "footer-Bug",
        "added@footer-Bug"));
    assertEquals("Extracted issues do not match", expected, actual);

    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
    assertLogMessageContains("Matching");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      bind(String.class).annotatedWith(PluginName.class)
          .toInstance("ItsTestName");

      serverConfig = createMock(Config.class);
      bind(Config.class).annotatedWith(GerritServerConfig.class)
          .toInstance(serverConfig);

      commitMessageFetcher = createMock(CommitMessageFetcher.class);
      bind(CommitMessageFetcher.class).toInstance(commitMessageFetcher);

      db = createMock(ReviewDb.class);
      bind(ReviewDb.class).toInstance(db);
    }
  }
}