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

package com.googlesource.gerrit.plugins.its.base.its;

import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An ITS facade doing nothing, it's configured when no ITS are referenced in config */
public class NoopItsFacade implements ItsFacade {

  private Logger log = LoggerFactory.getLogger(NoopItsFacade.class);

  @Override
  public void addComment(String issueId, String comment) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("addComment({},{})", issueId, comment);
    }
  }

  @Override
  public void addValueToField(String issueId, String fieldId, String value) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("addValueToField({},{},{})", issueId, fieldId, value);
    }
  }

  @Override
  public void addRelatedLink(String issueId, URL relatedUrl, String description)
      throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("addRelatedLink({},{},{})", issueId, relatedUrl, description);
    }
  }

  @Override
  public boolean exists(String issueId) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("exists({})", issueId);
    }
    return false;
  }

  @Override
  public void performAction(String issueId, String actionName) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("performAction({},{})", issueId, actionName);
    }
  }

  @Override
  public String healthCheck(Check check) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("healthCheck()");
    }
    return "{\"status\"=\"ok\",\"system\"=\"not configured\",}";
  }

  @Override
  public String createLinkForWebui(String url, String text) {
    if (log.isDebugEnabled()) {
      log.debug("createLinkForWebui({},{})", url, text);
    }
    return "";
  }
}
