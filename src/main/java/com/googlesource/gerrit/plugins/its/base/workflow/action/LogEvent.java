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

package com.googlesource.gerrit.plugins.its.base.workflow.action;

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsServerInfo;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import java.io.IOException;
import java.util.Set;
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
    ERROR {
      @Override
      void logProperty(Property property) {
        log.error(property.toString());
      }
    },
    WARN {
      @Override
      void logProperty(Property property) {
        log.warn(property.toString());
      }
    },
    INFO {
      @Override
      void logProperty(Property property) {
        log.info(property.toString());
      }
    },
    DEBUG {
      @Override
      void logProperty(Property property) {
        log.debug(property.toString());
      }
    };

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

    abstract void logProperty(Property property);
  }

  public interface Factory {
    LogEvent create();
  }

  @Inject
  public LogEvent() {}

  @Override
  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    Level level = Level.fromString(actionRequest.getParameter(1));
    for (Property property : properties) {
      level.logProperty(property);
    }
  }

  @Override
  public void execute(
      ItsServerInfo server, String issue, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    Level level = Level.fromString(actionRequest.getParameter(1));
    for (Property property : properties) {
      level.logProperty(property);
    }
  }
}
