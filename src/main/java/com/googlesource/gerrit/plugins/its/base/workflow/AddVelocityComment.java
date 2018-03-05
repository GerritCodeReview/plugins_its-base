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

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a short predefined comments to an issue.
 *
 * <p>Comments are added for merging, abandoning, restoring of changes and adding of patch sets.
 */
public class AddVelocityComment implements Action {
  private static final Logger log = LoggerFactory.getLogger(AddVelocityComment.class);

  public interface Factory {
    AddVelocityComment create();
  }

  private final ItsFacade its;
  private final Path templateDir;
  private final RuntimeInstance velocityRuntime;

  @Inject
  public AddVelocityComment(RuntimeInstance velocityRuntime, @ItsPath Path itsPath, ItsFacade its) {
    this.velocityRuntime = velocityRuntime;
    this.templateDir = itsPath.resolve("templates");
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

    velocityContext.put("its", new VelocityAdapterItsFacade(its));

    return velocityContext;
  }

  private String velocify(String template, Set<Property> properties) {
    VelocityContext context = getVelocityContext(properties);
    StringWriter w = new StringWriter();
    velocityRuntime.evaluate(context, w, "ItsComment", template);
    return w.toString();
  }

  @Override
  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    String template = null;
    String templateName = actionRequest.getParameter(1);
    if ("inline".equals(templateName)) {
      String[] allParameters = actionRequest.getParameters();
      String[] templateParameters = Arrays.copyOfRange(allParameters, 1, allParameters.length);
      template = StringUtils.join(templateParameters, " ");
    } else {
      if (templateName.isEmpty()) {
        log.error("No template name given in {}", actionRequest);
      } else {
        Path templatePath = templateDir.resolve(templateName + ".vm");
        if (Files.isReadable(templatePath)) {
          template = new String(Files.readAllBytes(templatePath));
        } else {
          log.error("Cannot read template {}", templatePath);
        }
      }
    }
    if (!Strings.isNullOrEmpty(template)) {
      String comment = velocify(template, properties);
      its.addComment(issue, comment);
    }
  }

  /** Adapter for ItsFacade to be used through Velocity */
  // Although we'd prefer to keep this class private, Velocity will only pick
  // it up, if it is public.
  public class VelocityAdapterItsFacade {

    private final ItsFacade facade;

    private VelocityAdapterItsFacade(ItsFacade facade) {
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
