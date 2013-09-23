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

package com.googlesource.gerrit.plugins.hooks.workflow.action;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.parboiled.common.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gerrit.server.config.SitePath;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;

/**
 * Adds a short predefined comments to an issue.
 *
 * Comments are added for merging, abandoning, restoring of changes and adding
 * of patch sets.
 */
public class AddVelocityComment implements Action {
  private static final Logger log = LoggerFactory.getLogger(
      AddVelocityComment.class);

  public interface Factory {
    AddVelocityComment create();
  }

  /**
   * Directory (relative to site) to search templates in
   */
  private static final String ITS_TEMPLATE_DIR = "etc" + File.separator +
      "its" + File.separator + "templates";

  private final ItsFacade its;
  private final File sitePath;
  private final RuntimeInstance velocityRuntime;

  @Inject
  public AddVelocityComment(RuntimeInstance velocityRuntime, @SitePath File sitePath, ItsFacade its) {
    this.velocityRuntime = velocityRuntime;
    this.sitePath = sitePath;
    this.its = its;
  }

  private VelocityContext getVelocityContext(Set<Property> properties) {
    VelocityContext velocityContext = new VelocityContext();
    for (Property property : properties) {
      String key = property.getKey();
      if (!Strings.isNullOrEmpty(key)) {
        String value = property.getValue();
        if (!Strings.isNullOrEmpty(value)) {
          velocityContext.put(key, value);
        }
      }
    }

    velocityContext.put("its",  new VelocityAdapterItsFacade(its));

    return velocityContext;
  }

  private String velocify(String template, Set<Property> properties) throws IOException {
    VelocityContext context = getVelocityContext(properties);
    StringWriter w = new StringWriter();
    velocityRuntime.evaluate(context, w, "ItsComment", template);
    return w.toString();
  }

  @Override
  public void execute(String issue, ActionRequest actionRequest,
      Set<Property> properties) throws IOException {
    String template = null;
    String templateName = actionRequest.getParameter(1);
    if ("inline".equals(templateName)) {
      String[] allParameters = actionRequest.getParameters();
      String[] templateParameters =
          Arrays.copyOfRange(allParameters, 1, allParameters.length);
      template = StringUtils.join(templateParameters, " ");
    } else {
      if (templateName.isEmpty()) {
        log.error("No template name given in " + actionRequest);
      } else {
        File templateFile = new File(sitePath, ITS_TEMPLATE_DIR +
            File.separator + templateName + ".vm");
        if (templateFile.canRead()) {
          template = FileUtils.readAllText(templateFile);
        } else {
          log.error("Cannot read template " + templateFile);
        }
      }
    }
    if (!Strings.isNullOrEmpty(template)) {
      String comment = velocify(template, properties);
      its.addComment(issue, comment);
    }
  }

  /**
   * Adapter for ItsFacade to be used through Velocity
   */
  // Although we'd prefer to keep this class private, Velocity will only pick
  // it up, if it is public.
  public class VelocityAdapterItsFacade {

    private final ItsFacade its;

    private VelocityAdapterItsFacade(ItsFacade its) {
      this.its = its;
    }

    /**
     * Format a link to a URL in the used Its' syntax.
     *
     * @param url URL to link to
     * @param caption Text used to represent the link
     * @return Link to the given URL in the used Its' syntax.
     */
    public String formatLink(String url, String caption) {
      return its.createLinkForWebui(url, caption);
    }

    /**
     * Format a link to an URL.
     * <p>
     * The provided URL is used as caption for the formatted link.
     *
     * @param url URL to link to
     * @return Link to the given URL in the used Its' syntax.
     */
    public String formatLink(String url) {
      return its.createLinkForWebui(url, url);
    }
  }
}
