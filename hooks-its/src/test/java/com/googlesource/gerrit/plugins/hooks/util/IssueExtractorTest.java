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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.hooks.its.ItsName;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

public class IssueExtractorTest extends LoggingMockingTestCase {
  private Injector injector;
  private Config serverConfig;
  private CommitMessageFetcher commitMessageFetcher;

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
    expected.put("42", Sets.newHashSet("somewhere"));
    assertEquals("Extracted issues do not match", expected, actual);

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
    expected.put("42", Sets.newHashSet("somewhere"));
    expected.put("4711", Sets.newHashSet("somewhere"));
    assertEquals("Extracted issues do not match", expected, actual);

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
    expected.put("42", Sets.newHashSet("somewhere"));
    expected.put("4711", Sets.newHashSet("somewhere"));
    assertEquals("Extracted issues do not match", expected, actual);

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
      bind(String.class).annotatedWith(ItsName.class)
          .toInstance("ItsTestName");

      serverConfig = createMock(Config.class);
      bind(Config.class).annotatedWith(GerritServerConfig.class)
          .toInstance(serverConfig);

      commitMessageFetcher = createMock(CommitMessageFetcher.class);
      bind(CommitMessageFetcher.class).toInstance(commitMessageFetcher);
    }
  }
}