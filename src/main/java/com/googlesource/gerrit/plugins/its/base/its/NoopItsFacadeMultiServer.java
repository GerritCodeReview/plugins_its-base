// Copyright (C) 2018 The Android Open Source Project
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

import com.googlesource.gerrit.plugins.its.base.its.ItsFacade.Check;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A NoOp ITS multiserver facade configured when no ITS are referenced in config */
public class NoopItsFacadeMultiServer implements ItsFacadeMultiServer {
  private Logger log = LoggerFactory.getLogger(NoopItsFacadeMultiServer.class);

  @Override
  public String createLinkForWebui(String url, String text) {
    if (log.isDebugEnabled()) {
      log.debug("createLinkForWebui({},{})", url, text);
    }
    return "";
  }

  @Override
  public String healthCheck(ItsServerInfo server, Check check) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("healthCheck()");
    }
    return "{\"status\"=\"ok\",\"system\"=\"not configured\"}";
  }

  @Override
  public void addComment(ItsServerInfo server, String issueId, String comment) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("addComment({},{}) in server {}", issueId, comment, server);
    }
  }

  @Override
  public void addRelatedLink(
      ItsServerInfo server, String issueId, URL relatedUrl, String description) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("addRelatedLink({},{},{}) to server {}", issueId, relatedUrl, description, server);
    }
  }

  @Override
  public boolean exists(ItsServerInfo server, String issueId) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("exists({} in server {})", issueId, server);
    }
    return false;
  }

  @Override
  public void performAction(ItsServerInfo server, String issueId, String actionName)
      throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("performAction({},{}) on server {}", issueId, actionName, server);
    }
  }
}
