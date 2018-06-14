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

import static org.easymock.EasyMock.expect;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.MockingTestCase;
import java.util.Collections;
import java.util.Optional;

public class MarkPropertyAsReleasedVersionParametersExtractorTest extends MockingTestCase {

  private static final String PROPERTY_ID = "propertyId";
  private static final String PROPERTY_VALUE = "propertyValue";

  private MarkPropertyAsReleasedVersionParametersExtractor extractor;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector injector = Guice.createInjector(new TestModule());
    extractor = injector.getInstance(MarkPropertyAsReleasedVersionParametersExtractor.class);
  }

  private class TestModule extends FactoryModule {}

  public void testNoParameter() {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testTwoParameters() {
    testWrongNumberOfReceivedParameters(new String[] {PROPERTY_ID, PROPERTY_ID});
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(parameters);

    replayMocks();

    Optional<MarkPropertyAsReleasedVersionParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testBlankPropertyId() {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {""});

    replayMocks();

    Optional<MarkPropertyAsReleasedVersionParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testUnknownPropertyId() {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    replayMocks();

    Optional<MarkPropertyAsReleasedVersionParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testHappyPath() {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    replayMocks();

    Optional<MarkPropertyAsReleasedVersionParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE));
    assertTrue(extractedParameters.isPresent());
    assertEquals(PROPERTY_VALUE, extractedParameters.get().getPropertyValue());
  }
}
