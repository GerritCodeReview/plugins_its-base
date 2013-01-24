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

import com.google.gerrit.pgm.init.InitStep;
import com.google.gerrit.pgm.init.Section;
import com.google.gerrit.pgm.util.ConsoleUI;

public class InitIts implements InitStep {

  public static String COMMENT_LINK_SECTION = "commentLink";

  public static enum YesNoEnum {
    Y, N;
  }

  public static enum TrueFalseEnum {
    TRUE, FALSE;
  }

  @Override
  public void run() throws Exception {
  }

  public boolean isConnectivityRequested(ConsoleUI ui, String url) {
    YesNoEnum wantToTest =
        ui.readEnum(YesNoEnum.N, "Test connectivity to %s", url);
    return wantToTest == YesNoEnum.Y;
  }

  public boolean enterSSLVerify(Section section) {
    return TrueFalseEnum.TRUE == section.select("Verify SSL Certificates",
        "sslVerify", TrueFalseEnum.TRUE);
  }
}
