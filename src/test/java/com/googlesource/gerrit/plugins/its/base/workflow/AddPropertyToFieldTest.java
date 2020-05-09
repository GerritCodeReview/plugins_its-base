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

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import junit.framework.TestCase;

public class AddPropertyToFieldTest extends TestCase {
  private static final String ISSUE_ID = "4711";
  private static final String FIELD_ID = "fieldId";
  private static final String PROPERTY_ID = "propertyId";
  private static final String PROPERTY_VALUE = "propertyValue";

  private Injector injector;
  private AddPropertyToFieldParametersExtractor parametersExtractor;
  private ItsFacade its;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      parametersExtractor = mock(AddPropertyToFieldParametersExtractor.class);
      bind(AddPropertyToFieldParametersExtractor.class).toInstance(parametersExtractor);

      its = mock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);
    }
  }

  public void testHappyPath() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties = Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE);
    when(parametersExtractor.extract(actionRequest, properties))
        .thenReturn(Optional.of(new AddPropertyToFieldParameters(PROPERTY_VALUE, FIELD_ID)));

    AddPropertyToField addPropertyToField = createAddPropertyToField();
    addPropertyToField.execute(its, ISSUE_ID, actionRequest, properties);

    verify(its).addValueToField(ISSUE_ID, PROPERTY_VALUE, FIELD_ID);
  }

  private AddPropertyToField createAddPropertyToField() {
    return injector.getInstance(AddPropertyToField.class);
  }
}
