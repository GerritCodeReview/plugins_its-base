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
package com.googlesource.gerrit.plugins.its.base.workflow.action;

import static org.easymock.EasyMock.expect;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddComment;

import java.io.IOException;
import java.util.HashSet;

public class AddCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;

  public void testEmpty() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(
        new String[] {});

    replayMocks();

    AddComment addComment = createAddComment();
    addComment.execute("4711", actionRequest, new HashSet<Property>());
  }

  public void testPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(
        new String[] {"Some", "test", "comment"});

    its.addComment("4711", "Some test comment");

    replayMocks();

    AddComment addComment = createAddComment();
    addComment.execute("4711", actionRequest, new HashSet<Property>());
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
      its = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);
    }
  }
}