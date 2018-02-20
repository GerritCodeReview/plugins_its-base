// Copyright (C) 2017 The Android Open Source Project
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

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import java.io.IOException;
import java.util.Map;

public class AddStandardCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;

  public void testChangeMergedPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "change-merged");

    its.addComment("42", "Change merged");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testChangeMergedFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "change-merged")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("submitterName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    its.addComment(
        "176",
        "Change 4711 merged by John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void testChangeAbandonedPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "change-abandoned");

    its.addComment("42", "Change abandoned");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testChangeAbandonedFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "change-abandoned")
            .put("reason", "Test-Reason")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("abandonerName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    its.addComment(
        "176",
        "Change 4711 abandoned by John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "Reason:\n"
            + "Test-Reason\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void testChangeRestoredPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "change-restored");

    its.addComment("42", "Change restored");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testChangeRestoredFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "change-restored")
            .put("reason", "Test-Reason")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("restorerName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    its.addComment(
        "176",
        "Change 4711 restored by John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "Reason:\n"
            + "Test-Reason\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void testPatchSetCreatedPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "patchset-created");

    its.addComment("42", "Change had a related patch set uploaded");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testPatchSetCreatedFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "patchset-created")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("uploaderName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    its.addComment(
        "176",
        "Change 4711 had a related patch set uploaded by "
            + "John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
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
