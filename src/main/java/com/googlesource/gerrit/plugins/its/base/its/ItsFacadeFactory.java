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

import com.google.gerrit.reviewdb.client.Project;

/* An interface to get server information from child its-plugin embedded in the ItsFacade implementation */
public interface ItsFacadeFactory {

  /* Returns the object of type ItsFacade containing server info extracted from project.config if configured
   * or the default server configured in gerrit.config if project name is empty or null*/
  ItsFacade getFacade(Project.NameKey project);
}
