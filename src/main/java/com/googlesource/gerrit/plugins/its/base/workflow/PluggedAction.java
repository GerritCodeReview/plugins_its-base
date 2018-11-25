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

package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.gerrit.extensions.annotations.ExtensionPoint;

import java.io.IOException;
import java.util.Map;

/** Interface for actions plugged by its-* plugins * */
@ExtensionPoint
public interface PluggedAction extends Action {

  /**
   * Execute this action.
   *
   * @param target The target to execute on. Its kind will depend on the action type.
   * @param actionRequest The request to execute.
   * @param properties The properties for the execution.
   */
  void execute(String target, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException;
}
