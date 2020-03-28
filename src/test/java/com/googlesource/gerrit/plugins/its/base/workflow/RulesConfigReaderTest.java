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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.util.Collection;
import org.eclipse.jgit.lib.Config;

public class RulesConfigReaderTest extends LoggingMockingTestCase {

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      ruleFactory = mock(Rule.Factory.class);
      bind(Rule.Factory.class).toInstance(ruleFactory);

      conditionFactory = mock(Condition.Factory.class);
      bind(Condition.Factory.class).toInstance(conditionFactory);

      actionRequestFactory = mock(ActionRequest.Factory.class);
      bind(ActionRequest.Factory.class).toInstance(actionRequestFactory);
    }
  }

  private static final String ACTION_1 = "action1";
  private static final String CONDITION_KEY = "condition";
  private static final String RULE_1 = "rule1";
  private static final String VALUE_1 = "value1";

  private ActionRequest.Factory actionRequestFactory;
  private Condition.Factory conditionFactory;
  private Rule.Factory ruleFactory;
  private Injector injector;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  public void testGetRulesFromConfig() {
    Config cfg = new Config();
    cfg.setString(RULE_SECTION, RULE_1, CONDITION_KEY, VALUE_1);
    cfg.setString(RULE_SECTION, RULE_1, ACTION_KEY, ACTION_1);

    Rule rule1 = mock(Rule.class);
    when(ruleFactory.create(RULE_1)).thenReturn(rule1);

    ActionRequest actionRequest1 = mock(ActionRequest.class);
    when(actionRequestFactory.create(ACTION_1)).thenReturn(actionRequest1);

    Condition condition1 = mock(Condition.class);
    when(conditionFactory.create(CONDITION_KEY, VALUE_1)).thenReturn(condition1);

    Collection<Rule> expected = ImmutableList.of(rule1);

    RulesConfigReader rulesConfigReader = injector.getInstance(RulesConfigReader.class);
    assertEquals("Rules do not match", expected, rulesConfigReader.getRulesFromConfig(cfg));

    verify(rule1).addActionRequest(actionRequest1);
    verify(rule1).addCondition(condition1);
  }
}
