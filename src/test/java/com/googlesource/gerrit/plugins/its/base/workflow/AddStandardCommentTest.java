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
package com.googlesource.gerrit.plugins.its.base.workflow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.util.Map;

public class AddStandardCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;

  public void testChangeMergedPlain() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "change-merged");

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "42", actionRequest, properties);

    verify(its).addComment("42", "Change merged");
  }

  public void testChangeMergedFull() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "change-merged")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("submitterName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "176", actionRequest, properties);

    verify(its)
        .addComment(
            "176",
            "Change 4711 merged by John Doe:\n"
                + "Test-Change-Subject\n"
                + "\n"
                + "HtTp://ExAmPlE.OrG/ChAnGe");
  }

  public void testChangeAbandonedPlain() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "change-abandoned");

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "42", actionRequest, properties);

    verify(its).addComment("42", "Change abandoned");
  }

  public void testChangeAbandonedFull() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "change-abandoned")
            .put("reason", "Test-Reason")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("abandonerName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "176", actionRequest, properties);

    verify(its)
        .addComment(
            "176",
            "Change 4711 abandoned by John Doe:\n"
                + "Test-Change-Subject\n"
                + "\n"
                + "Reason:\n"
                + "Test-Reason\n"
                + "\n"
                + "HtTp://ExAmPlE.OrG/ChAnGe");
  }

  public void testChangeRestoredPlain() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "change-restored");

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "42", actionRequest, properties);

    verify(its).addComment("42", "Change restored");
  }

  public void testChangeRestoredFull() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "change-restored")
            .put("reason", "Test-Reason")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("restorerName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "176", actionRequest, properties);

    verify(its)
        .addComment(
            "176",
            "Change 4711 restored by John Doe:\n"
                + "Test-Change-Subject\n"
                + "\n"
                + "Reason:\n"
                + "Test-Reason\n"
                + "\n"
                + "HtTp://ExAmPlE.OrG/ChAnGe");
  }

  public void testPatchSetCreatedPlain() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties = ImmutableMap.of("event-type", "patchset-created");

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "42", actionRequest, properties);

    verify(its).addComment("42", "Change had a related patch set uploaded");
  }

  public void testPatchSetCreatedFull() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("event-type", "patchset-created")
            .put("subject", "Test-Change-Subject")
            .put("changeNumber", "4711")
            .put("uploaderName", "John Doe")
            .put("formatChangeUrl", "HtTp://ExAmPlE.OrG/ChAnGe")
            .build();

    StandardAction action = injector.getInstance(AddStandardComment.class);
    action.execute(its, "176", actionRequest, properties);

    verify(its)
        .addComment(
            "176",
            "Change 4711 had a related patch set uploaded by "
                + "John Doe:\n"
                + "Test-Change-Subject\n"
                + "\n"
                + "HtTp://ExAmPlE.OrG/ChAnGe");
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
