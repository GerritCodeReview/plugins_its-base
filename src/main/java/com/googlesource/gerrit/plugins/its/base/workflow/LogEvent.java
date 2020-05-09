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

package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Dumps the event's properties to the log.
 *
 * <p>This event helps when developing rules as available properties become visible.
 */
public class LogEvent extends IssueAction {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private enum Level {
    ERROR,
    WARN,
    INFO,
    DEBUG;

    static Level fromString(String s) {
      if (s != null) {
        for (Level level : Level.values()) {
          if (s.toUpperCase().equals(level.toString())) {
            return level;
          }
        }
      }
      return INFO;
    }
  }

  public interface Factory {
    LogEvent create();
  }

  @Inject
  public LogEvent() {}

  private void logProperty(Level level, Entry<String, String> property) {
    final java.util.logging.Level logLevel;
    switch (level) {
      case ERROR:
        logLevel = java.util.logging.Level.SEVERE;
        break;
      case WARN:
        logLevel = java.util.logging.Level.WARNING;
        break;
      case INFO:
        logLevel = java.util.logging.Level.INFO;
        break;
      case DEBUG:
        logLevel = java.util.logging.Level.FINE;
        break;
      default:
        logger.atSevere().log("Undefined log level.");
        return;
    }
    logger.at(logLevel).log("[%s = %s]", property.getKey(), property.getValue());
  }

  @Override
  public void execute(
      ItsFacade its, String issue, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    Level level = Level.fromString(actionRequest.getParameter(1));
    for (Entry<String, String> property : properties.entrySet()) {
      logProperty(level, property);
    }
  }
}
