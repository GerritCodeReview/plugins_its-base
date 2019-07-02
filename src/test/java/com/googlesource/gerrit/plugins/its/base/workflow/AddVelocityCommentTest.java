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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.AddVelocityComment.VelocityAdapterItsFacade;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.jgit.util.FileUtils;

public class AddVelocityCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private Path itsPath;
  private ItsFacade its;
  private RuntimeInstance velocityRuntime;

  private boolean cleanupSitePath;

  public void testWarnNoTemplateNameGiven() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("");
    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, ImmutableMap.of());

    assertLogMessageContains("No template name");
  }

  public void testInlinePlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters()).andReturn(new String[] {"inline", "Simple-text"});

    IAnswer<Boolean> answer = new VelocityWriterFiller("Simple-text");
    expect(
            velocityRuntime.evaluate(
                (VelocityContext) anyObject(),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("Simple-text")))
        .andAnswer(answer);

    its.addComment("4711", "Simple-text");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, new HashMap<String, String>());
  }

  public void testInlineWithMultipleParameters() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters()).andReturn(new String[] {"inline", "Param2", "Param3"});

    IAnswer<Boolean> answer = new VelocityWriterFiller("Param2 Param3");
    expect(
            velocityRuntime.evaluate(
                (VelocityContext) anyObject(),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("Param2 Param3")))
        .andAnswer(answer);

    its.addComment("4711", "Param2 Param3");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, new HashMap<String, String>());
  }

  public void testInlineWithSingleProperty() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters()).andReturn(new String[] {"inline", "${subject}"});

    Map<String, String> properties = new HashMap<>();
    properties.put("subject", "Rosebud");

    IAnswer<Boolean> answer = new VelocityWriterFiller("Rosebud");
    Capture<VelocityContext> contextCapture = createCapture();
    expect(
            velocityRuntime.evaluate(
                capture(contextCapture),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("${subject}")))
        .andAnswer(answer);

    its.addComment("4711", "Rosebud");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, properties);

    VelocityContext context = contextCapture.getValue();
    assertEquals("Subject property of context did not match", "Rosebud", context.get("subject"));
  }

  public void testInlineWithUnusedProperty() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters()).andReturn(new String[] {"inline", "Test"});

    Map<String, String> properties = new HashMap<>();
    properties.put("subject", "Rosebud");

    IAnswer<Boolean> answer = new VelocityWriterFiller("Test");
    expect(
            velocityRuntime.evaluate(
                (VelocityContext) anyObject(),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("Test")))
        .andAnswer(answer);

    its.addComment("4711", "Test");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, properties);
  }

  public void testInlineWithMultipleProperties() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters())
        .andReturn(new String[] {"inline", "${subject}", "${reason}", "${subject}"});

    Map<String, String> properties = new HashMap<>();
    properties.put("subject", "Rosebud");
    properties.put("reason", "Life");

    IAnswer<Boolean> answer = new VelocityWriterFiller("Rosebud Life Rosebud");
    Capture<VelocityContext> contextCapture = createCapture();
    expect(
            velocityRuntime.evaluate(
                capture(contextCapture),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("${subject} ${reason} ${subject}")))
        .andAnswer(answer);

    its.addComment("4711", "Rosebud Life Rosebud");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, properties);

    VelocityContext context = contextCapture.getValue();
    assertEquals("Subject property of context did not match", "Rosebud", context.get("subject"));
    assertEquals("Reason property of context did not match", "Life", context.get("reason"));
  }

  public void testItsWrapperFormatLink1Parameter()
      throws IOException, SecurityException, IllegalArgumentException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters()).andReturn(new String[] {"inline", "Simple-Text"});

    IAnswer<Boolean> answer = new VelocityWriterFiller("Simple-Text");
    Capture<VelocityContext> contextCapture = createCapture();
    expect(
            velocityRuntime.evaluate(
                capture(contextCapture),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("Simple-Text")))
        .andAnswer(answer);

    its.addComment("4711", "Simple-Text");

    expect(its.createLinkForWebui("http://www.example.org/", "http://www.example.org/"))
        .andReturn("Formatted Link");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, new HashMap<String, String>());

    VelocityContext context = contextCapture.getValue();
    Object itsAdapterObj = context.get("its");
    assertNotNull("its property is null", itsAdapterObj);
    assertTrue(
        "Its is not a VelocityAdapterItsFacade instance",
        itsAdapterObj instanceof VelocityAdapterItsFacade);
    VelocityAdapterItsFacade itsAdapter = (VelocityAdapterItsFacade) itsAdapterObj;
    String formattedLink = itsAdapter.formatLink("http://www.example.org/");
    assertEquals("Result of formatLink does not match", "Formatted Link", formattedLink);
  }

  public void testItsWrapperFormatLink2Parameters()
      throws IOException, SecurityException, IllegalArgumentException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("inline");
    expect(actionRequest.getParameters()).andReturn(new String[] {"inline", "Simple-Text"});

    IAnswer<Boolean> answer = new VelocityWriterFiller("Simple-Text");
    Capture<VelocityContext> contextCapture = createCapture();
    expect(
            velocityRuntime.evaluate(
                capture(contextCapture),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("Simple-Text")))
        .andAnswer(answer);

    its.addComment("4711", "Simple-Text");

    expect(its.createLinkForWebui("http://www.example.org/", "Caption"))
        .andReturn("Formatted Link");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, new HashMap<String, String>());

    VelocityContext context = contextCapture.getValue();
    Object itsAdapterObj = context.get("its");
    assertNotNull("its property is null", itsAdapterObj);
    assertTrue(
        "Its is not a VelocityAdapterItsFacade instance",
        itsAdapterObj instanceof VelocityAdapterItsFacade);
    VelocityAdapterItsFacade itsAdapter = (VelocityAdapterItsFacade) itsAdapterObj;
    String formattedLink = itsAdapter.formatLink("http://www.example.org/", "Caption");
    assertEquals("Result of formatLink does not match", "Formatted Link", formattedLink);
  }

  public void testWarnTemplateNotFound() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("non-existing-template");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, ImmutableMap.of());

    assertLogMessageContains("non-existing-template");
  }

  public void testTemplateSimple() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("test-template");

    injectTestTemplate("Simple Test Template");

    IAnswer<Boolean> answer = new VelocityWriterFiller("Simple Test Template");
    expect(
            velocityRuntime.evaluate(
                (VelocityContext) anyObject(),
                (Writer) anyObject(),
                (String) anyObject(),
                eq("Simple Test Template")))
        .andAnswer(answer);

    its.addComment("4711", "Simple Test Template");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, new HashMap<String, String>());
  }

  public void testTemplateMultipleParametersAndProperties() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("test-template");

    Map<String, String> properties = new HashMap<>();
    properties.put("subject", "Rosebud");
    properties.put("reason", "Life");
    injectTestTemplate(
        "Test Template with subject: ${subject}.\n" + "${reason} is the reason for ${subject}.");

    IAnswer<Boolean> answer =
        new VelocityWriterFiller(
            "Test Template with subject: Rosebud.\n" + "Life is the reason for Rosebud.");
    Capture<VelocityContext> contextCapture = createCapture();
    expect(
            velocityRuntime.evaluate(
                capture(contextCapture),
                (Writer) anyObject(),
                (String) anyObject(),
                eq(
                    "Test Template with subject: ${subject}.\n"
                        + "${reason} is the reason for ${subject}.")))
        .andAnswer(answer);

    its.addComment(
        "4711", "Test Template with subject: Rosebud.\n" + "Life is the reason for Rosebud.");

    replayMocks();

    AddVelocityComment addVelocityComment = createAddVelocityComment();
    addVelocityComment.execute("4711", actionRequest, properties);

    VelocityContext context = contextCapture.getValue();
    assertEquals("Subject property of context did not match", "Rosebud", context.get("subject"));
    assertEquals("Reason property of context did not match", "Life", context.get("reason"));
  }

  private AddVelocityComment createAddVelocityComment() {
    return injector.getInstance(AddVelocityComment.class);
  }

  private void injectTestTemplate(String template) throws IOException {
    Path templatesFolder = itsPath.resolve("templates");
    Files.createDirectories(templatesFolder);
    Path templateFile = templatesFolder.resolve("test-template.vm");
    Files.write(templateFile, template.getBytes());
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

  private Path randomTargetPath() {
    return Paths.get("target", "random-name-" + UUID.randomUUID().toString());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      itsPath = randomTargetPath();
      assertFalse("sitePath already (" + itsPath + ") already exists", Files.exists(itsPath));
      cleanupSitePath = true;

      bind(Path.class).annotatedWith(ItsPath.class).toInstance(itsPath);

      its = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);

      velocityRuntime = createMock(RuntimeInstance.class);
      bind(RuntimeInstance.class).toInstance(velocityRuntime);
    }
  }

  private class VelocityWriterFiller implements IAnswer<Boolean> {
    private final String fill;
    private final boolean returnValue;

    private VelocityWriterFiller(String fill, boolean returnValue) {
      this.fill = fill;
      this.returnValue = returnValue;
    }

    private VelocityWriterFiller(String fill) {
      this(fill, true);
    }

    @Override
    public Boolean answer() throws Throwable {
      Object[] arguments = EasyMock.getCurrentArguments();
      Writer writer = (Writer) arguments[1];
      writer.write(fill);
      return returnValue;
    }
  }
}