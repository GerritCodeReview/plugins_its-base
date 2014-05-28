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

package com.googlesource.gerrit.plugins.hooks.its;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.project.ProjectState;

import java.util.Arrays;

public class ItsHookEnabledConfigEntry extends ProjectConfigEntry {
  private final String pluginName;
  private final PluginConfigFactory pluginCfgFactory;

  public ItsHookEnabledConfigEntry(String pluginName,
      PluginConfigFactory pluginCfgFactory) {
    super("Enable " + pluginName + " integration", "false",
        Arrays.asList(new String[] {"false", "true", "enforced"}), true);
    this.pluginName = pluginName;
    this.pluginCfgFactory = pluginCfgFactory;
  }

  @Override
  public boolean isEditable(ProjectState project) {
    for (ProjectState parentState : project.parents()) {
      PluginConfig parentCfg =
          pluginCfgFactory.getFromProjectConfig(parentState, pluginName);
      if ("enforced".equals(parentCfg.getString("enabled"))) {
        return false;
      }
    }
    return true;
  }
}
