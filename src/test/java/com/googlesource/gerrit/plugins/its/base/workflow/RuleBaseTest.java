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

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.jgit.util.FileUtils;

public class RuleBaseTest extends LoggingMockingTestCase {
  private Injector injector;

  private Path itsPath;
  private Rule.Factory ruleFactory;
  private Condition.Factory conditionFactory;
  private ActionRequest.Factory actionRequestFactory;

  private boolean cleanupSitePath;

  private enum RuleBaseKind {
    GLOBAL("actions"),
    ITS("actions-ItsTestName"),
    FAULTY("action");

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

    replayMocks();

    createRuleBase();
  }

  public void testSimpleRuleBase() throws IOException {
    injectRuleBase("[rule \"rule1\"]\n" + "\tconditionA = value1\n" + "\taction = action1");

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    Condition condition1 = createMock(Condition.class);
    expect(conditionFactory.create("conditionA", "value1")).andReturn(condition1);
    rule1.addCondition(condition1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    replayMocks();

    createRuleBase();
  }

  public void testBasicRuleBase() throws IOException {
    injectRuleBase(
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
            + "\taction = action3");

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    Condition condition1 = createMock(Condition.class);
    expect(conditionFactory.create("conditionA", "value1,value2")).andReturn(condition1);
    rule1.addCondition(condition1);

    Condition condition2 = createMock(Condition.class);
    expect(conditionFactory.create("conditionA", "value3,value of 4")).andReturn(condition2);
    rule1.addCondition(condition2);

    Condition condition3 = createMock(Condition.class);
    expect(conditionFactory.create("conditionB", "value5")).andReturn(condition3);
    rule1.addCondition(condition3);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action2 param")).andReturn(actionRequest2);
    rule1.addActionRequest(actionRequest2);

    Rule rule2 = createMock(Rule.class);
    expect(ruleFactory.create("rule2")).andReturn(rule2);

    Condition condition4 = createMock(Condition.class);
    expect(conditionFactory.create("conditionC", "value6")).andReturn(condition4);
    rule2.addCondition(condition4);

    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action3")).andReturn(actionRequest3);
    rule2.addActionRequest(actionRequest3);

    replayMocks();

    createRuleBase();
  }

  public void testActionRequestsForSimple() throws IOException {
    injectRuleBase("[rule \"rule1\"]\n" + "\taction = action1");

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    Map<String, String> properties = ImmutableMap.of();

    List<ActionRequest> rule1Match = Lists.newArrayListWithCapacity(1);
    rule1Match.add(actionRequest1);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match);

    replayMocks();

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
            + "\taction = action3");

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action2")).andReturn(actionRequest2);
    rule1.addActionRequest(actionRequest2);

    Rule rule2 = createMock(Rule.class);
    expect(ruleFactory.create("rule2")).andReturn(rule2);

    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action3")).andReturn(actionRequest3);
    rule2.addActionRequest(actionRequest3);

    Map<String, String> properties = ImmutableMap.of("sample", "property");

    List<ActionRequest> rule1Match = Lists.newArrayListWithCapacity(2);
    rule1Match.add(actionRequest1);
    rule1Match.add(actionRequest2);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match);

    List<ActionRequest> rule2Match = Lists.newArrayListWithCapacity(1);
    rule2Match.add(actionRequest3);
    expect(rule2.actionRequestsFor(properties)).andReturn(rule2Match);

    replayMocks();

    RuleBase ruleBase = createRuleBase();
    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = Lists.newArrayListWithCapacity(3);
    expected.add(actionRequest1);
    expected.add(actionRequest2);
    expected.add(actionRequest3);

    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testWarnExistingFaultyNameRuleBaseFile() throws IOException {
    injectRuleBase("", RuleBaseKind.FAULTY);

    replayMocks();

    createRuleBase();

    assertLogMessageContains("Please migrate"); // Migration warning for old name
    assertLogMessageContains("Neither global"); // For rule file at at usual places
  }

  public void testSimpleFaultyNameRuleBase() throws IOException {
    injectRuleBase(
        "[rule \"rule1\"]\n" + "\tconditionA = value1\n" + "\taction = action1",
        RuleBaseKind.FAULTY);

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    Condition condition1 = createMock(Condition.class);
    expect(conditionFactory.create("conditionA", "value1")).andReturn(condition1);
    rule1.addCondition(condition1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    replayMocks();

    createRuleBase();

    assertLogMessageContains("Please migrate"); // Migration warning for old name
    assertLogMessageContains("Neither global"); // For rule file at at usual places
  }

  public void testSimpleItsRuleBase() throws IOException {
    injectRuleBase(
        "[rule \"rule1\"]\n" + "\tconditionA = value1\n" + "\taction = action1", RuleBaseKind.ITS);

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    Condition condition1 = createMock(Condition.class);
    expect(conditionFactory.create("conditionA", "value1")).andReturn(condition1);
    rule1.addCondition(condition1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    replayMocks();

    createRuleBase();
  }

  public void testAllRuleBaseFilesAreLoaded() throws IOException {
    injectRuleBase("[rule \"rule1\"]\n" + "\taction = action1", RuleBaseKind.FAULTY);

    injectRuleBase("[rule \"rule2\"]\n" + "\taction = action2", RuleBaseKind.GLOBAL);

    injectRuleBase("[rule \"rule3\"]\n" + "\taction = action3", RuleBaseKind.ITS);

    Map<String, String> properties = ImmutableMap.of("sample", "property");

    Rule rule1 = createMock(Rule.class);
    expect(ruleFactory.create("rule1")).andReturn(rule1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action1")).andReturn(actionRequest1);
    rule1.addActionRequest(actionRequest1);

    List<ActionRequest> rule1Match = Lists.newArrayListWithCapacity(1);
    rule1Match.add(actionRequest1);
    expect(rule1.actionRequestsFor(properties)).andReturn(rule1Match);

    Rule rule2 = createMock(Rule.class);
    expect(ruleFactory.create("rule2")).andReturn(rule2);

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action2")).andReturn(actionRequest2);
    rule2.addActionRequest(actionRequest2);

    List<ActionRequest> rule2Match = Lists.newArrayListWithCapacity(1);
    rule2Match.add(actionRequest2);
    expect(rule2.actionRequestsFor(properties)).andReturn(rule2Match);

    Rule rule3 = createMock(Rule.class);
    expect(ruleFactory.create("rule3")).andReturn(rule3);

    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    expect(actionRequestFactory.create("action3")).andReturn(actionRequest3);
    rule3.addActionRequest(actionRequest3);

    List<ActionRequest> rule3Match = Lists.newArrayListWithCapacity(1);
    rule3Match.add(actionRequest3);
    expect(rule3.actionRequestsFor(properties)).andReturn(rule3Match);

    replayMocks();

    RuleBase ruleBase = createRuleBase();

    Collection<ActionRequest> actual = ruleBase.actionRequestsFor(properties);

    List<ActionRequest> expected = Lists.newArrayListWithCapacity(3);
    expected.add(actionRequest1);
    expected.add(actionRequest2);
    expected.add(actionRequest3);

    assertEquals("Matched actionRequests do not match", expected, actual);

    assertLogMessageContains("Please migrate"); // Migration warning for old name
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

      ruleFactory = createMock(Rule.Factory.class);
      bind(Rule.Factory.class).toInstance(ruleFactory);

      conditionFactory = createMock(Condition.Factory.class);
      bind(Condition.Factory.class).toInstance(conditionFactory);

      actionRequestFactory = createMock(ActionRequest.Factory.class);
      bind(ActionRequest.Factory.class).toInstance(actionRequestFactory);
    }
  }
}
