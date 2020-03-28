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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.SinceLastTagCommitCollector;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class FireEventOnCommitsTest extends TestCase {

  private static final String ITS_PROJECT = "test-project";
  private static final String COMMIT = "1234";
  private static final String PROJECT = "testProject";

  private Injector injector;
  private ItsFacade its;
  private PropertyExtractor propertyExtractor;
  private IssueExtractor issueExtractor;
  private RuleBase ruleBase;
  private ActionExecutor actionExecutor;
  private FireEventOnCommitsParametersExtractor parametersExtractor;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      its = mock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);

      propertyExtractor = mock(PropertyExtractor.class);
      bind(PropertyExtractor.class).toInstance(propertyExtractor);

      ruleBase = mock(RuleBase.class);
      bind(RuleBase.class).toInstance(ruleBase);

      issueExtractor = mock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      actionExecutor = mock(ActionExecutor.class);
      bind(ActionExecutor.class).toInstance(actionExecutor);

      parametersExtractor = mock(FireEventOnCommitsParametersExtractor.class);
      bind(FireEventOnCommitsParametersExtractor.class).toInstance(parametersExtractor);
    }
  }

  public void testHappyPath() throws IOException {
    Map<String, String> properties = ImmutableMap.of("project", PROJECT);

    FireEventOnCommitsParameters parameters = mock(FireEventOnCommitsParameters.class);
    SinceLastTagCommitCollector collector = mock(SinceLastTagCommitCollector.class);
    when(parameters.getCommitCollector()).thenReturn(collector);
    when(parameters.getProjectName()).thenReturn(PROJECT);

    ActionRequest actionRequest = mock(ActionRequest.class);
    when(parametersExtractor.extract(actionRequest, properties))
        .thenReturn(Optional.of(parameters));
    when(collector.collect(properties)).thenReturn(Collections.singletonList(COMMIT));

    Map<String, Set<String>> associations = Maps.newHashMap();
    when(issueExtractor.getIssueIds(PROJECT, COMMIT)).thenReturn(associations);

    Set<Map<String, String>> issuesProperties = ImmutableSet.of(properties);
    when(propertyExtractor.extractIssuesProperties(properties, associations))
        .thenReturn(issuesProperties);

    ActionRequest subActionRequest = mock(ActionRequest.class);
    Collection<ActionRequest> subActionRequests = Collections.singleton(subActionRequest);
    when(ruleBase.actionRequestsFor(properties)).thenReturn(subActionRequests);

    FireEventOnCommits fireEventOnCommits = createFireEventOnCommits();
    fireEventOnCommits.execute(its, ITS_PROJECT, actionRequest, properties);

    verify(actionExecutor).executeOnIssue(subActionRequests, properties);
  }

  private FireEventOnCommits createFireEventOnCommits() {
    return injector.getInstance(FireEventOnCommits.class);
  }
}
