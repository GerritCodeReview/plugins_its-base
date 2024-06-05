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

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.jbcsrc.api.SoySauce.Renderer;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds a short predefined comments to an issue.
 *
 * <p>Comments are added for merging, abandoning, restoring of changes and adding of patch sets.
 */
public class AddSoyComment extends IssueAction {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public interface Factory {
    AddSoyComment create();
  }

  private final Path templateDir;
  protected HashMap<String, Object> soyContext;

  @Inject
  public AddSoyComment(@ItsPath Path itsPath) {
    this.templateDir = itsPath.resolve("templates");
  }

  private String soyTextTemplate(
  private String soyTextTemplate(
      SoyFileSet.Builder builder, String template, Map<String, String> properties) {

    Path templatePath = templateDir.resolve(template + ".soy");
    String content;

    try (Reader r = Files.newBufferedReader(templatePath, StandardCharsets.UTF_8)) {
      content = CharStreams.toString(r);
    } catch (IOException err) {
      throw new ProvisionException(
          "Failed to read template file " + templatePath.toAbsolutePath().toString(), err);
    }

    builder.add(content, templatePath.toAbsolutePath().toString());

    int baseNameIndex = template.indexOf("_");
    // In case there are multiple templates in file.
    String fileNamespace =
        baseNameIndex == -1 ? template : template.substring(0, baseNameIndex);
    String templateInFileNamespace =
        String.join(".", "etc.its.templates", fileNamespace, template);
    String templateInCommonNamespace = String.join(".", "etc.its.templates", template);
    SoySauce soySauce = builder.build().compileTemplates();
    // For backwards compatibility with existing customizations and plugin templates with the
    // old non-unique namespace.
    String fullTemplateName =
        soySauce.hasTemplate(templateInFileNamespace)
            ? templateInFileNamespace
            : templateInCommonNamespace;
    Renderer renderer = soySauce.renderTemplate(fullTemplateName).setData(properties);
    String rendered = renderer.renderText().get();
    logger.atFinest().log("Rendered template %s to:\n%s", templatePath, rendered);
    return rendered;
  }

  @Override
  public void execute(
      ItsFacade its, String issue, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    String comment = buildComment(actionRequest, properties);
    if (!Strings.isNullOrEmpty(comment)) {
      its.addComment(issue, comment);
    }
  }

  private String buildComment(ActionRequest actionRequest, Map<String, String> properties) {
    String template = actionRequest.getParameter(1);
    if (!Strings.isNullOrEmpty(template)) {
      return soyTextTemplate(SoyFileSet.builder(), template, properties);
    }
    logger.atSevere().log("No template name given in %s", actionRequest);
    return "";
  }
}
