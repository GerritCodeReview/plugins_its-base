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

import com.googlesource.gerrit.plugins.hooks.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;

import java.io.IOException;
import java.util.Set;

/**
 * Interface for actions on an issue tracking system
 */
public interface Action {

  /**
   * Execute this action.
   *
   * @param issue The issue to execute on.
   * @param actionRequest The request to execute.
   * @param properties The properties for the execution.
   */
  public void execute(String issue, ActionRequest actionRequest,
      Set<Property> properties) throws IOException;
}
