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

public class CreateVersionFromPropertyTest extends TestCase {
  private static final String ITS_PROJECT = "test-project";
  private static final String PROPERTY_VALUE = "propertyValue";

  private Injector injector;
  private ItsFacade its;
  private CreateVersionFromPropertyParametersExtractor parametersExtractor;

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

      parametersExtractor = mock(CreateVersionFromPropertyParametersExtractor.class);
      bind(CreateVersionFromPropertyParametersExtractor.class).toInstance(parametersExtractor);
    }
  }

  public void testHappyPath() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties = Collections.emptyMap();
    when(parametersExtractor.extract(actionRequest, properties))
        .thenReturn(Optional.of(new CreateVersionFromPropertyParameters(PROPERTY_VALUE)));

    CreateVersionFromProperty createVersionFromProperty = createCreateVersionFromProperty();
    createVersionFromProperty.execute(its, ITS_PROJECT, actionRequest, properties);

    verify(its).createVersion(ITS_PROJECT, PROPERTY_VALUE);
  }

  private CreateVersionFromProperty createCreateVersionFromProperty() {
    return injector.getInstance(CreateVersionFromProperty.class);
  }
}
