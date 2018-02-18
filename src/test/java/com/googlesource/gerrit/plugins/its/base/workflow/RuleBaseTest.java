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

import static com.googlesource.gerrit.plugins.its.base.workflow.RulesConfigReader.ACTION_KEY;
import static com.googlesource.gerrit.plugins.its.base.workflow.RulesConfigReader.RULE_SECTION;
import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.SitePath;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.util.FileUtils;

public class RuleBaseTest extends LoggingMockingTestCase {
  private static final String ACTION_1 = "action1";
  private static final String CONDITION_KEY = "condition";
  private static final String PROJECT_KEY = "project";
  private static final String RULE_1 = "rule1";
  private static final String TEST_PROJECT = "testProject";
  private static final String VALUE_1 = "value1";

  private Injector injector;

  private Path sitePath;
  private RulesConfigReader rulesConfigReader;
  private ProjectCache projectCache;

  private boolean cleanupSitePath;

  private enum RuleBaseKind {
    GLOBAL("actions"),
    ITS("actions-ItsTestName");

    String fileName;

    RuleBaseKind(String fileName) {
      this.fileName = fileName + ".config";
    }
  }

  public void testWarnNonExistingRuleBase() {
    replayMocks();

    createRuleBase();

    assertLogMessageContains("Neither global");
  }

  public void testEmptyRuleBase() throws IOException {
    injectRuleBase("");

    Capture<Config> capturedConfig = createCapture();
    expect(rulesConfigReader.getRulesFromConfig(and(capture(capturedConfig), isA(Config.class))))
        .andReturn(ImmutableList.of())
        .once();

    replayMocks();

    createRuleBase();

    Config config = capturedConfig.getValue();
    assertTrue(config.toText().isEmpty());
  }

  public void testSimpleRuleBase() throws IOException {
    String rules = "[rule \"rule1\"]\n\tconditionA = value1\n\taction = action1\n";
    injectRuleBase(rules);

    Capture<Config> capturedConfig = createCapture();
    expect(rulesConfigReader.getRulesFromConfig(and(capture(capturedConfig), isA(Config.class))))
        .andReturn(ImmutableList.of())
        .once();

    replayMocks();

    createRuleBase();

    assertInConfig(rules, capturedConfig);
  }

  public void testBasicRuleBase() throws IOException {
    String rules =
        "[rule \"rule1\"]\n"
            + "\tconditionA = value1,value2\n"
            + "\tconditionA = value3,value of 4\n"
            + "\tconditionB = value5\n"
            + "\taction = action1\n"
            + "\taction = action2 param\n"
            + "\n"
            + "[ruleXZ \"nonrule\"]\n"
            + "\tconditionA = value1\n"
            + "\taction = action2\n"
            + "[rule \"rule2\"]\n"
            + "\tconditionC = value6\n"
            + "\taction = action3\n";
    injectRuleBase(rules);

    Capture<Config> capturedConfig = createCapture();
    expect(rulesConfigReader.getRulesFromConfig(and(capture(capturedConfig), isA(Config.class))))
        .andReturn(ImmutableList.of())
        .once();

    replayMocks();

    createRuleBase();

    assertInConfig(rules, capturedConfig);
  }

  public void testActionRequestsForSimple() throws IOException {
    String rules = "[rule \"rule1\"]\n\taction = action1\n";
    injectRuleBase(rules);

    Rule rule1 = createMock(Rule.class);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn(PROJECT_KEY);
    expect(property1.getValue()).andReturn("");
    Collection<Property> properties = ImmutableList.of(property1);

    List<ActionRequest> rule1Match = ImmutableList.of(actionRequest1);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match);

    Capture<Config> capturedConfig = createCapture();
    expect(rulesConfigReader.getRulesFromConfig(and(capture(capturedConfig), isA(Config.class))))
        .andReturn(ImmutableList.of(rule1))
        .once();

    replayMocks();

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest1);

    assertEquals("Matched actionRequests do not match", expected, actual);
    assertInConfig(rules, capturedConfig);
  }

  public void testActionRequestsForExtended() throws IOException {
    injectRuleBase(
        "[rule \"rule1\"]\n"
            + "\taction = action1\n"
            + "\taction = action2\n"
            + "\n"
            + "[rule \"rule2\"]\n"
            + "\taction = action3\n");

    Rule rule1 = createMock(Rule.class);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    ActionRequest actionRequest2 = createMock(ActionRequest.class);

    Rule rule2 = createMock(Rule.class);
    ActionRequest actionRequest3 = createMock(ActionRequest.class);

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn(PROJECT_KEY);
    expect(property1.getValue()).andReturn(TEST_PROJECT);
    Collection<Property> properties = ImmutableList.of(property1);

    List<ActionRequest> rule1Match = ImmutableList.of(actionRequest1, actionRequest2);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match).anyTimes();

    List<ActionRequest> rule2Match = ImmutableList.of(actionRequest3);
    expect(rule2.actionRequestsFor(properties)).andReturn(rule2Match).anyTimes();

    ProjectState projectState = createMock(ProjectState.class);
    ProjectLevelConfig projectLevelConfigGlobal = createMock(ProjectLevelConfig.class);
    expect(projectLevelConfigGlobal.get()).andReturn(new Config());
    expect(projectState.getConfig(RuleBaseKind.GLOBAL.fileName))
        .andReturn(projectLevelConfigGlobal);
    ProjectLevelConfig projectLevelConfigPlugin = createMock(ProjectLevelConfig.class);
    expect(projectLevelConfigPlugin.get()).andReturn(new Config());
    expect(projectState.getConfig(RuleBaseKind.ITS.fileName)).andReturn(projectLevelConfigPlugin);
    expect(projectState.parents()).andReturn(FluentIterable.of());
    expect(projectCache.checkedGet(new Project.NameKey(TEST_PROJECT))).andReturn(projectState);

    expect(rulesConfigReader.getRulesFromConfig(isA(Config.class)))
        .andReturn(ImmutableList.of(rule1, rule2))
        .andReturn(ImmutableList.of())
        .anyTimes();

    replayMocks();

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest1, actionRequest2, actionRequest3);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testSimpleItsRuleBase() throws IOException {
    String rules = "[rule \"rule1\"]\n\tconditionA = value1\n\taction = action1\n";
    injectRuleBase(rules, RuleBaseKind.ITS);

    Capture<Config> capturedConfig = createCapture();
    expect(rulesConfigReader.getRulesFromConfig(and(capture(capturedConfig), isA(Config.class))))
        .andReturn(ImmutableList.of())
        .once();

    replayMocks();

    createRuleBase();

    assertInConfig(rules, capturedConfig);
  }

  public void testGlobalRuleBaseFilesAreLoaded() throws IOException {
    injectRuleBase("[rule \"rule2\"]\n\taction = action2", RuleBaseKind.GLOBAL);

    injectRuleBase("[rule \"rule3\"]\n\taction = action3", RuleBaseKind.ITS);

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn(PROJECT_KEY);
    expect(property1.getValue()).andReturn("");
    Collection<Property> properties = ImmutableList.of(property1);

    Rule rule2 = createMock(Rule.class);
    ActionRequest actionRequest2 = createMock(ActionRequest.class);

    List<ActionRequest> rule2Match = ImmutableList.of(actionRequest2);
    expect(rule2.actionRequestsFor(properties)).andReturn(rule2Match);

    Rule rule3 = createMock(Rule.class);
    ActionRequest actionRequest3 = createMock(ActionRequest.class);

    List<ActionRequest> rule3Match = ImmutableList.of(actionRequest3);
    expect(rule3.actionRequestsFor(properties)).andReturn(rule3Match);

    expect(rulesConfigReader.getRulesFromConfig(isA(Config.class)))
        .andReturn(ImmutableList.of(rule2, rule3))
        .andReturn(ImmutableList.of())
        .anyTimes();

    replayMocks();

    RuleBase ruleBase = createRuleBase();

    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest2, actionRequest3);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testProjectConfigIsLoaded() throws IOException {
    Rule rule1 = createMock(Rule.class);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn(PROJECT_KEY);
    expect(property1.getValue()).andReturn(TEST_PROJECT);
    Collection<Property> properties = ImmutableList.of(property1);

    List<ActionRequest> rule1Match = ImmutableList.of(actionRequest1);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match);

    ProjectState projectState = createMock(ProjectState.class);
    ProjectLevelConfig projectLevelConfigGlobal = createMock(ProjectLevelConfig.class);
    Config projectGlobalCfg = new Config();
    projectGlobalCfg.setString(RULE_SECTION, RULE_1, CONDITION_KEY, VALUE_1);
    projectGlobalCfg.setString(RULE_SECTION, RULE_1, ACTION_KEY, ACTION_1);
    expect(projectLevelConfigGlobal.get()).andReturn(projectGlobalCfg);
    expect(projectState.getConfig(RuleBaseKind.GLOBAL.fileName))
        .andReturn(projectLevelConfigGlobal);
    ProjectLevelConfig projectLevelConfigPlugin = createMock(ProjectLevelConfig.class);
    expect(projectLevelConfigPlugin.get()).andReturn(new Config());
    expect(projectState.getConfig(RuleBaseKind.ITS.fileName)).andReturn(projectLevelConfigPlugin);
    expect(projectCache.checkedGet(new Project.NameKey(TEST_PROJECT))).andReturn(projectState);

    expect(rulesConfigReader.getRulesFromConfig(isA(Config.class)))
        .andReturn(ImmutableList.of(rule1))
        .andReturn(ImmutableList.of());

    replayMocks();

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest1);

    assertEquals("Matched actionRequests do not match", expected, actual);
    assertLogMessageContains("Neither global rule file");
  }

  public void testParentProjectConfigIsLoaded() throws IOException {
    Rule rule1 = createMock(Rule.class);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn(PROJECT_KEY);
    expect(property1.getValue()).andReturn(TEST_PROJECT);
    Collection<Property> properties = ImmutableList.of(property1);

    List<ActionRequest> rule1Match = ImmutableList.of(actionRequest1);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match);

    ProjectState projectState = createMock(ProjectState.class);
    ProjectLevelConfig projectLevelConfigGlobal = createMock(ProjectLevelConfig.class);
    expect(projectLevelConfigGlobal.get()).andReturn(new Config());
    expect(projectState.getConfig(RuleBaseKind.GLOBAL.fileName))
        .andReturn(projectLevelConfigGlobal);
    ProjectLevelConfig projectLevelConfigPlugin = createMock(ProjectLevelConfig.class);
    expect(projectLevelConfigPlugin.get()).andReturn(new Config());
    expect(projectState.getConfig(RuleBaseKind.ITS.fileName)).andReturn(projectLevelConfigPlugin);

    ProjectState parentProjectState = createMock(ProjectState.class);
    ProjectLevelConfig parentProjectConfigGlobal = createMock(ProjectLevelConfig.class);
    Config parentGlobalCfg = new Config();
    parentGlobalCfg.setString(RULE_SECTION, RULE_1, CONDITION_KEY, VALUE_1);
    parentGlobalCfg.setString(RULE_SECTION, RULE_1, ACTION_KEY, ACTION_1);
    expect(parentProjectConfigGlobal.get()).andReturn(parentGlobalCfg);
    expect(parentProjectState.getConfig(RuleBaseKind.GLOBAL.fileName))
        .andReturn(parentProjectConfigGlobal);
    ProjectLevelConfig parentProjectConfigPlugin = createMock(ProjectLevelConfig.class);
    expect(parentProjectConfigPlugin.get()).andReturn(new Config());
    expect(parentProjectState.getConfig(RuleBaseKind.ITS.fileName))
        .andReturn(parentProjectConfigPlugin);
    expect(projectState.parents()).andReturn(FluentIterable.of(parentProjectState));
    expect(projectCache.checkedGet(new Project.NameKey(TEST_PROJECT))).andReturn(projectState);

    expect(rulesConfigReader.getRulesFromConfig(isA(Config.class)))
        .andReturn(ImmutableList.of())
        .andReturn(ImmutableList.of())
        .andReturn(ImmutableList.of(rule1))
        .andReturn(ImmutableList.of());

    replayMocks();

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = ImmutableList.of(actionRequest1);

    assertEquals("Matched actionRequests do not match", expected, actual);
    assertLogMessageContains("Neither global rule file");
  }

  private void assertInConfig(String rules, Capture<Config> capturedConfig) {
    Config config = capturedConfig.getValue();
    assertEquals(config.toText(), rules);
  }

  private RuleBase createRuleBase() {
    return injector.getInstance(RuleBase.class);
  }

  private void injectRuleBase(String rules) throws IOException {
    injectRuleBase(rules, RuleBaseKind.GLOBAL);
  }

  private void injectRuleBase(String rules, RuleBaseKind ruleBaseKind) throws IOException {
    Path ruleBaseFile = sitePath.resolve("etc").resolve("its").resolve(ruleBaseKind.fileName);
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
      if (Files.exists(sitePath)) {
        FileUtils.delete(sitePath.toFile(), FileUtils.RECURSIVE);
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

      sitePath = randomTargetPath();
      assertFalse("sitePath already (" + sitePath + ") already exists", Files.exists(sitePath));
      cleanupSitePath = true;

      rulesConfigReader = createMock(RulesConfigReader.class);
      bind(RulesConfigReader.class).toInstance(rulesConfigReader);

      bind(Path.class).annotatedWith(SitePath.class).toInstance(sitePath);

      projectCache = createMock(ProjectCache.class);
      bind(ProjectCache.class).toInstance(projectCache);
    }
  }
}
