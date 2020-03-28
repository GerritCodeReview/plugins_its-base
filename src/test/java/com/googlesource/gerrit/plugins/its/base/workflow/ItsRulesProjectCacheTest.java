// Copyright (C) 2018 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.GlobalRulesFileName;
import com.googlesource.gerrit.plugins.its.base.PluginRulesFileName;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.RuleBaseTest.RuleBaseKind;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

public class ItsRulesProjectCacheTest extends LoggingMockingTestCase {
  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      rulesConfigReader = mock(RulesConfigReader.class);
      bind(RulesConfigReader.class).toInstance(rulesConfigReader);

      projectCache = mock(ProjectCache.class);
      bind(ProjectCache.class).toInstance(projectCache);

      bind(String.class)
          .annotatedWith(GlobalRulesFileName.class)
          .toInstance(RuleBaseKind.GLOBAL.fileName);
      bind(String.class)
          .annotatedWith(PluginRulesFileName.class)
          .toInstance(RuleBaseKind.ITS.fileName);
    }
  }

  private static final String ACTION_1 = "action1";
  private static final String CONDITION_KEY = "condition";
  private static final String RULE_1 = "rule1";
  private static final String TEST_PROJECT = "testProject";
  private static final String VALUE_1 = "value1";

  private Injector injector;
  private ProjectCache projectCache;
  private RulesConfigReader rulesConfigReader;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  public void testProjectConfigIsLoaded() throws IOException {
    Rule rule1 = new Rule(RULE_1);
    ActionRequest action1 = new ActionRequest(ACTION_1);
    Condition condition1 = new Condition(CONDITION_KEY, VALUE_1);
    rule1.addActionRequest(action1);
    rule1.addCondition(condition1);

    ProjectState projectState = mock(ProjectState.class);
    ProjectLevelConfig projectLevelConfigGlobal = mock(ProjectLevelConfig.class);
    Config projectGlobalCfg = new Config();
    projectGlobalCfg.setString(RULE_SECTION, RULE_1, CONDITION_KEY, VALUE_1);
    projectGlobalCfg.setString(RULE_SECTION, RULE_1, ACTION_KEY, ACTION_1);
    when(projectLevelConfigGlobal.get()).thenReturn(projectGlobalCfg);
    when(projectState.getConfig(RuleBaseKind.GLOBAL.fileName)).thenReturn(projectLevelConfigGlobal);
    ProjectLevelConfig projectLevelConfigPlugin = mock(ProjectLevelConfig.class);
    when(projectLevelConfigPlugin.get()).thenReturn(new Config());
    when(projectState.getConfig(RuleBaseKind.ITS.fileName)).thenReturn(projectLevelConfigPlugin);
    when(projectCache.get(Project.nameKey(TEST_PROJECT))).thenReturn(Optional.of(projectState));
    when(rulesConfigReader.getRulesFromConfig(any(Config.class)))
        .thenReturn(ImmutableList.of(rule1))
        .thenReturn(ImmutableList.of());

    ItsRulesProjectCacheImpl.Loader loader =
        injector.getInstance(ItsRulesProjectCacheImpl.Loader.class);
    Collection<Rule> actual = loader.load(TEST_PROJECT);
    List<Rule> expected = ImmutableList.of(rule1);

    assertEquals("Rules do not match", expected, actual);
    assertTrue(actual.contains(rule1));
  }

  public void testParentProjectConfigIsLoaded() throws IOException {
    Rule rule1 = new Rule(RULE_1);
    ActionRequest action1 = new ActionRequest(ACTION_1);
    Condition condition1 = new Condition(CONDITION_KEY, VALUE_1);
    rule1.addActionRequest(action1);
    rule1.addCondition(condition1);

    ProjectState projectState = mock(ProjectState.class);
    ProjectLevelConfig projectLevelConfigGlobal = mock(ProjectLevelConfig.class);
    when(projectLevelConfigGlobal.get()).thenReturn(new Config());
    when(projectState.getConfig(RuleBaseKind.GLOBAL.fileName)).thenReturn(projectLevelConfigGlobal);
    ProjectLevelConfig projectLevelConfigPlugin = mock(ProjectLevelConfig.class);
    when(projectLevelConfigPlugin.get()).thenReturn(new Config());
    when(projectState.getConfig(RuleBaseKind.ITS.fileName)).thenReturn(projectLevelConfigPlugin);

    ProjectState parentProjectState = mock(ProjectState.class);
    ProjectLevelConfig parentProjectConfigGlobal = mock(ProjectLevelConfig.class);
    Config parentGlobalCfg = new Config();
    parentGlobalCfg.setString(RULE_SECTION, RULE_1, CONDITION_KEY, VALUE_1);
    parentGlobalCfg.setString(RULE_SECTION, RULE_1, ACTION_KEY, ACTION_1);
    when(parentProjectConfigGlobal.get()).thenReturn(parentGlobalCfg);
    when(parentProjectState.getConfig(RuleBaseKind.GLOBAL.fileName))
        .thenReturn(parentProjectConfigGlobal);
    ProjectLevelConfig parentProjectConfigPlugin = mock(ProjectLevelConfig.class);
    when(parentProjectConfigPlugin.get()).thenReturn(new Config());
    when(parentProjectState.getConfig(RuleBaseKind.ITS.fileName))
        .thenReturn(parentProjectConfigPlugin);
    when(projectState.parents()).thenReturn(FluentIterable.of(parentProjectState));
    when(projectCache.get(Project.nameKey(TEST_PROJECT))).thenReturn(Optional.of(projectState));

    when(rulesConfigReader.getRulesFromConfig(any(Config.class)))
        .thenReturn(ImmutableList.of())
        .thenReturn(ImmutableList.of())
        .thenReturn(ImmutableList.of(rule1))
        .thenReturn(ImmutableList.of());

    ItsRulesProjectCacheImpl.Loader loader =
        injector.getInstance(ItsRulesProjectCacheImpl.Loader.class);
    Collection<Rule> actual = loader.load(TEST_PROJECT);
    List<Rule> expected = ImmutableList.of(rule1);

    assertEquals("Rules do not match", expected, actual);
    assertTrue(actual.contains(rule1));
  }
}
