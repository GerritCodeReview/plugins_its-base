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

package com.googlesource.gerrit.plugins.its.base.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.GlobalRulesFileName;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.PluginRulesFileName;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.util.FileUtils;

public class RuleBaseTest extends LoggingMockingTestCase {
  private static final String PROJECT_KEY = "project";
  private static final String TEST_PROJECT = "testProject";

  private Injector injector;

  private Path itsPath;
  private RulesConfigReader rulesConfigReader;
  private ItsRulesProjectCache rulesProjectCache;

  private boolean cleanupSitePath;

  public enum RuleBaseKind {
    GLOBAL("actions"),
    ITS("actions-ItsTestName");

    String fileName;

    RuleBaseKind(String fileName) {
      this.fileName = fileName + ".config";
    }
  }

  public void testActionRequestsForSimple() throws IOException {
    String rules = "[rule \"rule1\"]\n\taction = action1\n";
    injectRuleBase(rules);

    Rule rule1 = mock(Rule.class);
    ActionRequest actionRequest1 = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of(PROJECT_KEY, TEST_PROJECT);

    List<ActionRequest> rule1Match = Lists.newArrayListWithCapacity(1);
    rule1Match.add(actionRequest1);
    when(rule1.actionRequestsFor(properties)).thenReturn(rule1Match);

    when(rulesConfigReader.getRulesFromConfig(any(Config.class)))
        .thenReturn(ImmutableList.of(rule1))
        .thenThrow(new UnsupportedOperationException("Method called more than once"));

    when(rulesProjectCache.get(TEST_PROJECT)).thenReturn(ImmutableList.of());

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = Lists.newArrayListWithCapacity(3);
    expected.add(actionRequest1);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testActionRequestsForExtended() throws IOException {
    injectRuleBase(
        "[rule \"rule1\"]\n"
            + "\taction = action1\n"
            + "\taction = action2\n"
            + "\n"
            + "[rule \"rule2\"]\n"
            + "\taction = action3\n");

    Rule rule1 = mock(Rule.class);
    ActionRequest actionRequest1 = mock(ActionRequest.class);
    ActionRequest actionRequest2 = mock(ActionRequest.class);

    Rule rule2 = mock(Rule.class);
    ActionRequest actionRequest3 = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of(PROJECT_KEY, TEST_PROJECT);

    List<ActionRequest> rule1Match = ImmutableList.of(actionRequest1, actionRequest2);
    when(rule1.actionRequestsFor(properties)).thenReturn(rule1Match);

    List<ActionRequest> rule2Match = ImmutableList.of(actionRequest3);
    when(rule2.actionRequestsFor(properties)).thenReturn(rule2Match);

    when(rulesProjectCache.get(TEST_PROJECT)).thenReturn(ImmutableList.of());

    when(rulesConfigReader.getRulesFromConfig(any(Config.class)))
        .thenReturn(ImmutableList.of(rule1, rule2))
        .thenReturn(ImmutableList.of());

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest1, actionRequest2, actionRequest3);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testAllRuleBaseFilesAreLoaded() throws IOException {
    injectRuleBase("[rule \"rule2\"]\n\taction = action2", RuleBaseKind.GLOBAL);

    injectRuleBase("[rule \"rule3\"]\n\taction = action3", RuleBaseKind.ITS);

    Map<String, String> properties = ImmutableMap.of(PROJECT_KEY, TEST_PROJECT);

    Rule rule2 = mock(Rule.class);
    ActionRequest actionRequest2 = mock(ActionRequest.class);

    List<ActionRequest> rule2Match = ImmutableList.of(actionRequest2);
    when(rule2.actionRequestsFor(properties)).thenReturn(rule2Match);

    Rule rule3 = mock(Rule.class);
    ActionRequest actionRequest3 = mock(ActionRequest.class);

    List<ActionRequest> rule3Match = ImmutableList.of(actionRequest3);
    when(rule3.actionRequestsFor(properties)).thenReturn(rule3Match);

    when(rulesProjectCache.get(TEST_PROJECT)).thenReturn(ImmutableList.of());

    when(rulesConfigReader.getRulesFromConfig(any(Config.class)))
        .thenReturn(ImmutableList.of(rule2, rule3))
        .thenReturn(ImmutableList.of());

    RuleBase ruleBase = createRuleBase();

    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest2, actionRequest3);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testProjectConfigIsLoaded() {
    Rule rule1 = mock(Rule.class);
    ActionRequest actionRequest1 = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of(PROJECT_KEY, TEST_PROJECT);

    List<ActionRequest> rule1Match = ImmutableList.of(actionRequest1);
    when(rule1.actionRequestsFor(properties)).thenReturn(rule1Match);

    when(rulesProjectCache.get(TEST_PROJECT)).thenReturn(ImmutableList.of(rule1));

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest1);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  private RuleBase createRuleBase() {
    return injector.getInstance(RuleBase.class);
  }

  private void injectRuleBase(String rules) throws IOException {
    injectRuleBase(rules, RuleBaseKind.GLOBAL);
  }

  private void injectRuleBase(String rules, RuleBaseKind ruleBaseKind) throws IOException {
    Path ruleBaseFile = itsPath.resolve(ruleBaseKind.fileName);
    Files.createDirectories(ruleBaseFile.getParent());
    Files.write(ruleBaseFile, rules.getBytes());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cleanupSitePath = false;
    injector = Guice.createInjector(new TestModule());
  }

  @Override
  public void tearDown() throws Exception {
    if (cleanupSitePath) {
      if (Files.exists(itsPath)) {
        FileUtils.delete(itsPath.toFile(), FileUtils.RECURSIVE);
      }
    }
    super.tearDown();
  }

  private Path randomTargetPath() {
    return Paths.get("target", "random-name-" + UUID.randomUUID().toString());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {

      bind(String.class).annotatedWith(PluginName.class).toInstance("ItsTestName");

      itsPath = randomTargetPath().resolve("etc").resolve("its");
      assertFalse("itsPath (" + itsPath + ") already exists", Files.exists(itsPath));
      cleanupSitePath = true;

      bind(Path.class).annotatedWith(ItsPath.class).toInstance(itsPath);

      rulesConfigReader = mock(RulesConfigReader.class);
      bind(RulesConfigReader.class).toInstance(rulesConfigReader);

      rulesProjectCache = mock(ItsRulesProjectCache.class);
      bind(ItsRulesProjectCache.class).toInstance(rulesProjectCache);

      bind(String.class)
          .annotatedWith(GlobalRulesFileName.class)
          .toInstance(RuleBaseKind.GLOBAL.fileName);

      bind(String.class)
          .annotatedWith(PluginRulesFileName.class)
          .toInstance(RuleBaseKind.ITS.fileName);
    }
  }
}
