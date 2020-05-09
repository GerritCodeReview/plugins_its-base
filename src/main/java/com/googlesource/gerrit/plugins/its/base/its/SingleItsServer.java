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

import com.google.gerrit.entities.Project;
import com.google.inject.Inject;

/* An ItsServer implementation that should be bound
 * for backward compatibility */
public class SingleItsServer implements ItsFacadeFactory {
  private final ItsFacade its;

  @Inject
  public SingleItsServer(ItsFacade its) {
    this.its = its;
  }

  @Override
  public ItsFacade getFacade(Project.NameKey project) {
    return its;
  }
}
