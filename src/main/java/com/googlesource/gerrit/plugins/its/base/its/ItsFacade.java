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

/** A simple facade to an issue tracking system (its) */
public interface ItsFacade {

  public enum Check {
    SYSINFO,
    ACCESS
  }

  public String healthCheck(Check check) throws IOException;

  public void addRelatedLink(String issueId, URL relatedUrl, String description) throws IOException;

  public void addComment(String issueId, String comment) throws IOException;

  default void addValueToField(String issueId, String value, String fieldId) throws IOException {
    throw new UnsupportedOperationException(
        "add-value-to-field is not currently implemented by " + getClass());
  }

  public void performAction(String issueId, String actionName) throws IOException;

  public boolean exists(final String issueId) throws IOException;

  public String createLinkForWebui(String url, String text);
}
