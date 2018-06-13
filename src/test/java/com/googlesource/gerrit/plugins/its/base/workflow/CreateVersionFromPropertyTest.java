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
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.MockingTestCase;
import java.io.IOException;
import java.util.Collections;
import org.easymock.EasyMock;

public class CreateVersionFromPropertyTest extends MockingTestCase {

  private static final String ITS_PROJECT = "test-project";
  private static final String PROPERTY_ID = "propertyId";
  private static final String PROPERTY_VALUE = "propertyValue";

  private Injector injector;
  private ItsFacade its;

  public void testNoParameter() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testTwoParameters() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {PROPERTY_ID, PROPERTY_ID});
  }

  public void testBlankPropertyId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {""});

    replayMocks();

    CreateVersionFromProperty createVersionFromProperty = createCreateVersionFromProperty();
    createVersionFromProperty.execute(its, ITS_PROJECT, actionRequest, Collections.emptyMap());
  }

  public void testUnknownPropertyId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    replayMocks();

    CreateVersionFromProperty createVersionFromProperty = createCreateVersionFromProperty();
    createVersionFromProperty.execute(its, ITS_PROJECT, actionRequest, Collections.emptyMap());
  }

  public void testHappyPath() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    its.createVersion(ITS_PROJECT, PROPERTY_VALUE);
    EasyMock.expectLastCall().once();

    replayMocks();

    CreateVersionFromProperty createVersionFromProperty = createCreateVersionFromProperty();
    createVersionFromProperty.execute(
        its, ITS_PROJECT, actionRequest, Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE));
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(parameters);

    replayMocks();

    CreateVersionFromProperty addPropertyToField = createCreateVersionFromProperty();
    addPropertyToField.execute(its, ITS_PROJECT, actionRequest, Collections.emptyMap());
  }

  private CreateVersionFromProperty createCreateVersionFromProperty() {
    return injector.getInstance(CreateVersionFromProperty.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      its = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);
    }
  }
}
