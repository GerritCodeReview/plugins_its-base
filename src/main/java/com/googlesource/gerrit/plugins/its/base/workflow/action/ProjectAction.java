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

package com.googlesource.gerrit.plugins.its.base.workflow.action;

import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import java.io.IOException;
import java.util.Set;

public interface ProjectAction {

  /**
   * Execute this action.
   *
   * @param itsProject The its itsProject to execute on.
   * @param actionRequest The request to execute.
   * @param properties The properties for the execution.
   */
  void execute(String itsProject, ActionRequest actionRequest, Set<Property> properties)
      throws IOException;
}