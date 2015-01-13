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

import com.google.common.base.Strings;
import com.google.gerrit.common.data.RefConfigSection;
import com.google.gerrit.pgm.init.api.AllProjectsConfig;
import com.google.gerrit.pgm.init.api.AllProjectsNameOnInitProvider;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.Section;
import com.google.gerrit.pgm.init.api.ConsoleUI;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

import java.io.IOException;

public class InitIts implements InitStep {

  public static String COMMENT_LINK_SECTION = "commentLink";

  public static enum TrueFalseEnum {
    TRUE, FALSE;
  }

  public static enum ItsIntegration {
    ENABLED, DISABLED, ENFORCED;
  }

  private final String pluginName;
  private final String itsDisplayName;
  protected final ConsoleUI ui;
  private final AllProjectsConfig allProjectsConfig;
  private final AllProjectsNameOnInitProvider allProjects;

  public InitIts(String pluginName, String itsDisplayName, ConsoleUI ui,
      AllProjectsConfig allProjectsConfig,
      AllProjectsNameOnInitProvider allProjects) {
    this.pluginName = pluginName;
    this.itsDisplayName = itsDisplayName;
    this.ui = ui;
    this.allProjectsConfig = allProjectsConfig;
    this.allProjects = allProjects;
  }

  @Override
  public void run() throws IOException, ConfigInvalidException {
  }

  @Override
  public void postRun() throws IOException, ConfigInvalidException {
    Config cfg = allProjectsConfig.load().getConfig();
    ui.message("\n");
    ui.header(itsDisplayName + " Integration");

    ItsIntegration itsintegration;
    String enabled = cfg.getString("plugin", pluginName, "enabled");
    if (ItsIntegration.ENFORCED.name().equalsIgnoreCase(enabled)) {
      itsintegration = ItsIntegration.ENFORCED;
    } else if (Boolean.parseBoolean(enabled)) {
      itsintegration = ItsIntegration.ENABLED;
    } else {
      itsintegration = ItsIntegration.DISABLED;
    }
    itsintegration =
        ui.readEnum(itsintegration,
            "Issue tracker integration for all projects?");
    switch (itsintegration) {
      case ENFORCED:
        cfg.setString("plugin", pluginName, "enabled", "enforced");
        break;
      case ENABLED:
        cfg.setBoolean("plugin", pluginName, "enabled", true);
        break;
      case DISABLED:
        cfg.unset("plugin", pluginName, "enabled");
        break;
      default:
        throw new IOException("Unsupported value for issue track integration: "
            + itsintegration.name());
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
