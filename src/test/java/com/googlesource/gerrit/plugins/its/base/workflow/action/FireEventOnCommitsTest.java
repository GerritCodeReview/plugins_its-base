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

package com.googlesource.gerrit.plugins.its.base.workflow.action;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.MockingTestCase;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionController;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import com.googlesource.gerrit.plugins.its.base.workflow.action.commit_collector.SinceLastTagCommitCollector;
import org.easymock.EasyMock;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.expect;

public class FireEventOnCommitsTest extends MockingTestCase {

  private static final String ITS_PROJECT = "test-project";
  private static final String COMMIT = "1234";
  private static final String COLLECTOR_NAME = "since-last-tag";
  private static final String PROJECT = "project";

  private Injector injector;
  private PropertyExtractor propertyExtractor;
  private IssueExtractor issueExtractor;
  private ActionController actionController;
  private SinceLastTagCommitCollector.Factory sinceLastTagCommitCollectorFactory;

  public void testNoParameter() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testTwoParameters() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {COLLECTOR_NAME, COLLECTOR_NAME});
  }

  public void testBlankCollectorName() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {""});

    replayMocks();

    FireEventOnCommits fireEventOnCommits = createFireEventOnCommits();
    fireEventOnCommits.execute(ITS_PROJECT, actionRequest, Collections.emptySet());
  }

  public void testHappyPath() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {COLLECTOR_NAME});

    Property projectProperty = createMock(Property.class);
    expect(projectProperty.getKey()).andReturn("project");
    expect(projectProperty.getValue()).andReturn(PROJECT);
    Set<Property> properties = Collections.singleton(projectProperty);

    SinceLastTagCommitCollector collector = createMock(SinceLastTagCommitCollector.class);
    expect(sinceLastTagCommitCollectorFactory.create()).andReturn(collector);
    expect(collector.collect(properties)).andReturn(Collections.singletonList(COMMIT));

    Map<String, Set<String>> associations = Maps.newHashMap();
    expect(issueExtractor.getIssueIds(PROJECT, COMMIT)).andReturn(associations);

    Set<Set<Property>> issuesProperties = Sets.newHashSet();
    issuesProperties.add(properties);

    expect(propertyExtractor.extractIssuesProperties(properties, associations))
        .andReturn(issuesProperties);

    actionController.handleIssuesEvent(issuesProperties);
    EasyMock.expectLastCall().once();

    replayMocks();

    FireEventOnCommits fireEventOnCommits = createFireEventOnCommits();
    fireEventOnCommits.execute(ITS_PROJECT, actionRequest, Collections.singleton(projectProperty));
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(parameters);

    replayMocks();

    FireEventOnCommits fireEventOnCommits = createFireEventOnCommits();
    fireEventOnCommits.execute(ITS_PROJECT, actionRequest, Collections.emptySet());
  }

  private FireEventOnCommits createFireEventOnCommits() {
    return injector.getInstance(FireEventOnCommits.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      propertyExtractor = createMock(PropertyExtractor.class);
      bind(PropertyExtractor.class).toInstance(propertyExtractor);

      actionController = createMock(ActionController.class);
      bind(ActionController.class).toInstance(actionController);

      issueExtractor = createMock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      sinceLastTagCommitCollectorFactory = createMock(SinceLastTagCommitCollector.Factory.class);
      bind(SinceLastTagCommitCollector.Factory.class)
          .toInstance(sinceLastTagCommitCollectorFactory);
    }
  }
}
