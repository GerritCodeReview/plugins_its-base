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

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;

/**
 * Adds a fixed comment to an issue.
 *
 * The action requests parameters get concatenated and get added to the issue.
 */
public class AddComment implements Action {
  public interface Factory {
    AddComment create();
  }

  private final ItsFacade its;

  @Inject
  public AddComment(ItsFacade its) {
    this.its = its;
  }

  @Override
  public void execute(String issue, ActionRequest actionRequest,
      Set<Property> properties) throws IOException {
    String[] parameters = actionRequest.getParameters();
    String comment = StringUtils.join(parameters, " ");
    if (!Strings.isNullOrEmpty(comment)) {
      its.addComment(issue, comment);
    }
  }
}
