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
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.SinceLastTagCommitCollector;
import java.util.Collections;
import java.util.Optional;
import junit.framework.TestCase;

public class FireEventOnCommitsParametersExtractorTest extends TestCase {

  private static final String SINCE_LAST_TAG_COLLECTOR = "since-last-tag";

  private SinceLastTagCommitCollector.Factory sinceLastTagCommitCollectorFactory;
  private FireEventOnCommitsParametersExtractor extractor;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector injector = Guice.createInjector(new TestModule());
    extractor = injector.getInstance(FireEventOnCommitsParametersExtractor.class);
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      sinceLastTagCommitCollectorFactory = mock(SinceLastTagCommitCollector.Factory.class);
      bind(SinceLastTagCommitCollector.Factory.class)
          .toInstance(sinceLastTagCommitCollectorFactory);
    }
  }

  public void testNoParameter() {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testTwoParameters() {
    testWrongNumberOfReceivedParameters(
        new String[] {SINCE_LAST_TAG_COLLECTOR, SINCE_LAST_TAG_COLLECTOR});
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(parameters);

    Optional<FireEventOnCommitsParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testBlankCollectorName() {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {""});

    Optional<FireEventOnCommitsParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testUnknownCollectorName() {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {"foo"});

    Optional<FireEventOnCommitsParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testSinceLastTagCollector() {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {SINCE_LAST_TAG_COLLECTOR});

    SinceLastTagCommitCollector collector = mock(SinceLastTagCommitCollector.class);
    when(sinceLastTagCommitCollectorFactory.create()).thenReturn(collector);

    Optional<FireEventOnCommitsParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.singletonMap("project", "testProject"));
    if (!extractedParameters.isPresent()) {
      fail();
    }
    assertEquals(collector, extractedParameters.get().getCommitCollector());
    assertEquals("testProject", extractedParameters.get().getProjectName());
  }
}
