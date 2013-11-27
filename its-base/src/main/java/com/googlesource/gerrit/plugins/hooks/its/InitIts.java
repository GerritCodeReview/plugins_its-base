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

import com.google.gerrit.pgm.init.AllProjectsConfig;
import com.google.gerrit.pgm.init.InitStep;
import com.google.gerrit.pgm.init.Section;
import com.google.gerrit.pgm.util.ConsoleUI;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

import java.io.IOException;

public class InitIts implements InitStep {

  public static String COMMENT_LINK_SECTION = "commentLink";

  public static enum TrueFalseEnum {
    TRUE, FALSE;
  }

  private final String pluginName;
  private final String itsDisplayName;
  protected final ConsoleUI ui;
  private final AllProjectsConfig allProjectsConfig;

  public InitIts(String pluginName, String itsDisplayName, ConsoleUI ui,
      AllProjectsConfig allProjectsConfig) {
    this.pluginName = pluginName;
    this.itsDisplayName = itsDisplayName;
    this.ui = ui;
    this.allProjectsConfig = allProjectsConfig;
  }

  @Override
  public void run() {
  }

  @Override
  public void postRun() throws IOException, ConfigInvalidException {
    ui.message("\n");
    ui.header(itsDisplayName + " Integration");
    boolean enabled = ui.yesno(false, "By default enabled for all projects");
    Config cfg = allProjectsConfig.load();
    if (enabled) {
      cfg.setBoolean("plugin", pluginName, "enabled", enabled);
    } else {
      cfg.unset("plugin", pluginName, "enabled");
    }
    allProjectsConfig.save(pluginName, "Initialize " + itsDisplayName + " Integration");
  }

  public boolean isConnectivityRequested(String url) {
    return ui.yesno(false, "Test connectivity to %s", url);
  }

  public boolean enterSSLVerify(Section section) {
    return TrueFalseEnum.TRUE == section.select("Verify SSL Certificates",
        "sslVerify", TrueFalseEnum.TRUE);
  }
}
