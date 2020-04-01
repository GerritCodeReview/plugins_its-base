// Copyright (C) 2013 The Android Open Source Project
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
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;

public class AddCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;

  public void testEmpty() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {});

    AddComment addComment = createAddComment();
    addComment.execute(null, "4711", actionRequest, ImmutableMap.of());
  }

  public void testPlain() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {"Some", "test", "comment"});

    AddComment addComment = createAddComment();
    addComment.execute(its, "4711", actionRequest, ImmutableMap.of());

    verify(its).addComment("4711", "Some test comment");
  }

  private AddComment createAddComment() {
    return injector.getInstance(AddComment.class);
  }

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
    }
  }
}
