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

import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.eclipse.jgit.util.FileUtils;

public class AddSoyCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;
  private boolean cleanupSitePath;
  private Path itsPath;

  public void testTemplateFileDoesNotExist() {
    ActionRequest actionRequest = new ActionRequest("foo nonExistingTemplate");

    AddSoyComment addSoyComment = createAddSoyComment();

    assertThrows(
        ProvisionException.class,
        () -> addSoyComment.execute(its, "4711", actionRequest, ImmutableMap.of()));
  }

  public void testPlain() throws IOException {
    String templateName = "plain";

    ActionRequest actionRequest = new ActionRequest("foo " + templateName);

    injectTemplate(templateName, "bar");

    AddSoyComment addSoyComment = createAddSoyComment();
    addSoyComment.execute(its, "4711", actionRequest, ImmutableMap.of());

    verify(its).addComment("4711", "bar");
  }

  public void testParameterPlain() throws IOException {
    String templateName = "parameterPlain";

    ActionRequest actionRequest = new ActionRequest("foo " + templateName);
    Map<String, String> properties =
        ImmutableMap.<String, String>builder().put("param1", "simple").build();

    injectTemplate(templateName, "{@param param1:string}{$param1}");

    AddSoyComment addSoyComment = createAddSoyComment();
    addSoyComment.execute(its, "4711", actionRequest, properties);

    verify(its).addComment("4711", "simple");
  }

  public void testParameterEscapingDefault() throws IOException {
    String templateName = "parameterEscapingDefault";

    ActionRequest actionRequest = new ActionRequest("foo " + templateName);
    Map<String, String> properties =
        ImmutableMap.<String, String>builder().put("param1", "<b>bar\"/>").build();

    injectTemplate(templateName, "{@param param1:string}{$param1}");

    AddSoyComment addSoyComment = createAddSoyComment();
    addSoyComment.execute(its, "4711", actionRequest, properties);

    verify(its).addComment("4711", "&lt;b&gt;bar&quot;/&gt;");
  }

  public void testParameterEscapingTextUnescaped() throws IOException {
    String templateName = "parameterEscapingTextUnescaped";

    ActionRequest actionRequest = new ActionRequest("foo " + templateName);
    Map<String, String> properties =
        ImmutableMap.<String, String>builder().put("param1", "<b>bar\"/>").build();

    injectTemplate(templateName, "text", "{@param param1:string}{$param1}");

    AddSoyComment addSoyComment = createAddSoyComment();
    addSoyComment.execute(its, "4711", actionRequest, properties);

    verify(its).addComment("4711", "<b>bar\"/>");
  }

  public void testParameterEscapingTextForcedEscapingUri() throws IOException {
    String templateName = "parameterEscapingTextForcedEscapingUri";

    ActionRequest actionRequest = new ActionRequest("foo " + templateName);
    Map<String, String> properties =
        ImmutableMap.<String, String>builder().put("param1", "<b>bar\"/>").build();

    injectTemplate(templateName, "text", "{@param param1:string}{$param1|escapeUri}");

    AddSoyComment addSoyComment = createAddSoyComment();
    addSoyComment.execute(its, "4711", actionRequest, properties);

    verify(its).addComment("4711", "%3Cb%3Ebar%22%2F%3E");
  }

  private AddSoyComment createAddSoyComment() {
    return injector.getInstance(AddSoyComment.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cleanupSitePath = false;
    injector = Guice.createInjector(new TestModule());
  }

  @Override
  public void tearDown() throws Exception {
    if (cleanupSitePath) {
      if (Files.exists(itsPath)) {
        FileUtils.delete(itsPath.toFile(), FileUtils.RECURSIVE);
      }
    }
    super.tearDown();
  }

  private void injectTemplate(String name, String content) throws IOException {
    injectTemplate(name, null, content);
  }

  private void injectTemplate(String name, String kind, String content) throws IOException {
    Path templatePath = itsPath.resolve("templates").resolve(name + ".soy");
    Files.createDirectories(templatePath.getParent());
    String namespace = "{namespace etc.its.templates}";
    String opening = "{template ." + name + (kind != null ? (" kind=\"" + kind + "\"") : "") + "}";
    String closing = "{/template}";

    String fullTemplate = namespace + opening + content + closing;

    Files.write(templatePath, fullTemplate.getBytes());
  }

  private Path randomTargetPath() {
    return Paths.get("target", "random_name_" + UUID.randomUUID().toString().replaceAll("-", "_"));
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      its = mock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);

      itsPath = randomTargetPath().resolve("etc").resolve("its");
      assertFalse("itsPath (" + itsPath + ") already exists", Files.exists(itsPath));
      cleanupSitePath = true;
      bind(Path.class).annotatedWith(ItsPath.class).toInstance(itsPath);
    }
  }
}
