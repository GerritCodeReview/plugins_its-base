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

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.net.URL;

/** An ITS facade doing nothing, it's configured when no ITS are referenced in config */
public class NoopItsFacade implements ItsFacade {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void addComment(String issueId, String comment) throws IOException {
    logger.atFine().log("addComment(%s,%s)", issueId, comment);
  }

  @Override
  public void addValueToField(String issueId, String value, String fieldId) throws IOException {
    logger.atFine().log("addValueToField(%s,%s,%s)", issueId, fieldId, value);
  }

  @Override
  public void addRelatedLink(String issueId, URL relatedUrl, String description)
      throws IOException {
    logger.atFine().log("addRelatedLink(%s,%s,%s)", issueId, relatedUrl, description);
  }

  @Override
  public boolean exists(String issueId) throws IOException {
    logger.atFine().log("exists(%s)", issueId);
    return false;
  }

  @Override
  public void performAction(String issueId, String actionName) throws IOException {
    logger.atFine().log("performAction(%s,%s)", issueId, actionName);
  }

  @Override
  public void createVersion(String itsProject, String version) {
    logger.atFine().log("createVersion(%s,%s)", itsProject, version);
  }

  @Override
  public String healthCheck(Check check) throws IOException {
    logger.atFine().log("healthCheck()");
    return "{\"status\"=\"ok\",\"system\"=\"not configured\",}";
  }

  @Override
  public String createLinkForWebui(String url, String text) {
    logger.atFine().log("createLinkForWebui(%s,%s)", url, text);
    return "";
  }
}
