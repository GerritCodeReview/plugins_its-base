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

import com.googlesource.gerrit.plugins.its.base.its.ItsFacade.Check;
import java.io.IOException;
import java.net.URL;

/** A facade to an issue tracking system (ITS) that supports multiple ITS servers */
public interface ItsFacadeMultiServer {

  void addComment(ItsServerInfo server, String issueId, String comment) throws IOException;

  void addRelatedLink(ItsServerInfo server, String issueId, URL relatedUrl, String description)
      throws IOException;

  String createLinkForWebui(String url, String text);

  boolean exists(ItsServerInfo server, final String issueId) throws IOException;

  String healthCheck(ItsServerInfo server, Check check) throws IOException;

  void performAction(ItsServerInfo server, String issueId, String actionName) throws IOException;
}
