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

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dumps the event's properties to the log.
 *
 * <p>This event helps when developing rules as available properties become visible.
 */
public class LogEvent implements Action {
  private static final Logger log = LoggerFactory.getLogger(LogEvent.class);

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

  @Override
  public ActionType getType() {
    return ActionType.ISSUE;
  }

  private void logProperty(Level level, Entry<String, String> property) {
    String message = String.format("[%s = %s]", property.getKey(), property.getValue());
    switch (level) {
      case ERROR:
        log.error(message);
        break;
      case WARN:
        log.warn(message);
        break;
      case INFO:
        log.info(message);
        break;
      case DEBUG:
        log.debug(message);
        break;
      default:
        log.error("Undefined log level.");
    }
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
