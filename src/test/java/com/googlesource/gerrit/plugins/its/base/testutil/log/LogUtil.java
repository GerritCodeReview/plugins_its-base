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

package com.googlesource.gerrit.plugins.its.base.testutil.log;

import java.util.Collection;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class LogUtil {
  public static CollectionAppender logToCollection(
      String logName, Collection<LogEvent> collection) {
    LoggerConfig log = new LoggerConfig(logName, null, false);
    CollectionAppender listAppender = new CollectionAppender(collection);
    for (Appender appender : log.getAppenders().values()) {
      log.removeAppender(appender.toString());
    }
    log.addAppender(listAppender);
    return listAppender;
  }
}
