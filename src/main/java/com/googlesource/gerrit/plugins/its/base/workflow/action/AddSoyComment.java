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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Strings;
import com.google.gerrit.server.config.SitePath;
import com.google.inject.Inject;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a short predefined comments to an issue.
 *
 * <p>Comments are added for merging, abandoning, restoring of changes and adding of patch sets.
 */
public class AddSoyComment implements Action {
  private static final Logger log = LoggerFactory.getLogger(AddSoyComment.class);

  public interface Factory {
    AddSoyComment create();
  }

  /** Directory (relative to site) to search templates in */
  private static final String ITS_TEMPLATE_DIR =
      "etc" + File.separator + "its" + File.separator + "templates";

  private final ItsFacade its;
  private final Path sitePath;
  private final SoyTofu soyTofu;
  protected HashMap<String, Object> soyContext;

  @Inject
  public AddSoyComment(SoyTofu soyTofu, @SitePath Path sitePath, ItsFacade its) {
    this.soyTofu = soyTofu;
    this.sitePath = sitePath;
    this.its = its;
  }

  private HashMap getSoyContext(Set<Property> properties) {
    HashMap<String, Object> soyContext = new HashMap<>();
    for (Property property : properties) {
      String key = property.getKey();
      if (!Strings.isNullOrEmpty(key)) {
        String value = property.getValue();
        if (!Strings.isNullOrEmpty(value)) {
          soyContext.put(key, value);
        }
      }
    }

    soyContext.put("its", new SoyAdapterItsFacade(its));

    return soyContext;
  }

  /*private String soyify(String template, Set<Property> properties) {
    HashMap context = getSoyContext(properties);
    StringWriter w = new StringWriter();
    soyTofu.evaluate(context, w, "ItsComment", template);
    return w.toString();
  }*/

  private String soyTemplate(String template, SanitizedContent.ContentKind kind, Set<Property> properties) {
    HashMap context = getSoyContext(properties);
    return soyTofu
        .compileToTofu()
        .newRenderer("etc.its.templates." + template)
        .setContentKind(kind)
        .setData(context)
        .render();
  }

  protected String soyTextTemplate(String template, Set<Property> properties) {
    return soyTemplate(template, SanitizedContent.ContentKind.TEXT, properties);
  }

  @Override
  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    String template = null;
    String templateName = actionRequest.getParameter(1);
    /*if ("inline".equals(templateName)) {
      String[] allParameters = actionRequest.getParameters();
      String[] templateParameters = Arrays.copyOfRange(allParameters, 1, allParameters.length);
      template = StringUtils.join(templateParameters, " ");
    } else {*/
      if (templateName.isEmpty()) {
        log.error("No template name given in " + actionRequest);
      } else {
        // Path templateDir = sitePath.resolve(ITS_TEMPLATE_DIR);
        // Path templatePath = templateDir.resolve(templateName + ".soy");
        // if (Files.isReadable(templatePath)) {
        //  template = new String(Files.readAllBytes(templatePath));
        // } else {
        //  log.error("Cannot read template " + templatePath);
        // }
        template = templateName;
      }
    // }
    if (!Strings.isNullOrEmpty(template)) {
      // String comment = soyify(template, properties);
      String comment = soyTextTemplate(template, properties);
      its.addComment(issue, comment);
    }
  }

  /** Adapter for ItsFacade to be used through Soy */
  // Although we'd prefer to keep this class private, Soy will only pick
  // it up, if it is public.
  public class SoyAdapterItsFacade {

    private final ItsFacade facade;

    private SoyAdapterItsFacade(ItsFacade facade) {
      this.facade = facade;
    }

    /**
     * Format a link to a URL in the used Its' syntax.
     *
     * @param url URL to link to
     * @param caption Text used to represent the link
     * @return Link to the given URL in the used Its' syntax.
     */
    public String formatLink(String url, String caption) {
      return facade.createLinkForWebui(url, caption);
    }

    /**
     * Format a link to an URL.
     *
     * <p>The provided URL is used as caption for the formatted link.
     *
     * @param url URL to link to
     * @return Link to the given URL in the used Its' syntax.
     */
    public String formatLink(String url) {
      return facade.createLinkForWebui(url, url);
    }
  }
}
