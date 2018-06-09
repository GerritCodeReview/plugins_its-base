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

public class AddPropertyToFieldTest extends MockingTestCase {

  private static final String ISSUE_ID = "4711";
  private static final String FIELD_ID = "fieldId";
  private static final String PROPERTY_ID = "propertyId";
  private static final String PROPERTY_VALUE = "propertyValue";

  private Injector injector;
  private ItsFacade its;

  public void testNoParameter() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testOneParameter() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {PROPERTY_ID});
  }

  public void testThreeParameters() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {PROPERTY_ID, PROPERTY_ID, PROPERTY_ID});
  }

  public void testBlankFieldId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID, ""});

    replayMocks();

    AddPropertyToField addPropertyToField = createAddPropertyToField();
    addPropertyToField.execute(ISSUE_ID, actionRequest, Collections.emptyMap());
  }

  public void testBlankPropertyId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {"", FIELD_ID});

    replayMocks();

    AddPropertyToField addPropertyToField = createAddPropertyToField();
    addPropertyToField.execute(ISSUE_ID, actionRequest, Collections.emptyMap());
  }

  public void testUnknownPropertyId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {FIELD_ID, PROPERTY_ID});

    replayMocks();

    AddPropertyToField addPropertyToField = createAddPropertyToField();
    addPropertyToField.execute(ISSUE_ID, actionRequest, Collections.emptyMap());
  }

  public void testHappyPath() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID, FIELD_ID});

    its.addValueToField(ISSUE_ID, PROPERTY_VALUE, FIELD_ID);
    EasyMock.expectLastCall().once();

    replayMocks();

    AddPropertyToField addPropertyToField = createAddPropertyToField();
    addPropertyToField.execute(
        ISSUE_ID, actionRequest, Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE));
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(parameters);

    replayMocks();

    AddPropertyToField addPropertyToField = createAddPropertyToField();
    addPropertyToField.execute(ISSUE_ID, actionRequest, Collections.emptyMap());
  }

  private AddPropertyToField createAddPropertyToField() {
    return injector.getInstance(AddPropertyToField.class);
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
