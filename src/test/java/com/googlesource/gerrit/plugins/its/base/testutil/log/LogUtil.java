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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class LogUtil {
  public static void logToCollection(
      String logName, Collection<LogRecord> collection, org.apache.log4j.Level level) {

    CollectionAppender appender = new CollectionAppender(collection);

    logToCollectionLog4j(logName, appender, level);
    logToCollectionJul(logName, appender, julLevelFromLog4jLevel(level));
  }

  private static void logToCollectionLog4j(
      String logName, CollectionAppender appender, org.apache.log4j.Level level) {
    Logger log = LogManager.getLogger(logName);
    log.setLevel(level);
    log.removeAllAppenders();
    log.addAppender(appender);
  }

  private static void logToCollectionJul(String logName, CollectionAppender appender, Level level) {
    java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(logName);
    julLogger.setLevel(level);

    julLogger.addHandler(
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            appender.append(record);
          }

          @Override
          public void flush() {}

          @Override
          public void close() throws SecurityException {}
        });
  }

  public static LogRecord logRecordFromLog4jLoggingEvent(LoggingEvent event) {
    LogRecord logRecord = new LogRecord(Level.ALL, event.getRenderedMessage());
    ThrowableInformation tInfo = event.getThrowableInformation();
    if (tInfo != null) {
      logRecord.setThrown(tInfo.getThrowable());
    }
    return logRecord;
  }

  public static Level julLevelFromLog4jLevel(org.apache.log4j.Level level) {
    final Level ret;
    if (level == org.apache.log4j.Level.OFF) {
      ret = Level.OFF;
    } else if (level == org.apache.log4j.Level.FATAL) {
      ret = Level.SEVERE;
    } else if (level == org.apache.log4j.Level.ERROR) {
      ret = Level.SEVERE;
    } else if (level == org.apache.log4j.Level.WARN) {
      ret = Level.WARNING;
    } else if (level == org.apache.log4j.Level.INFO) {
      ret = Level.INFO;
    } else if (level == org.apache.log4j.Level.DEBUG) {
      ret = Level.FINE;
    } else if (level == org.apache.log4j.Level.TRACE) {
      ret = Level.FINEST;
    } else if (level == org.apache.log4j.Level.ALL) {
      ret = Level.FINEST;
    } else {
      ret = null;
    }
    return ret;
  }

  public static boolean equalLevels(Level left, org.apache.log4j.Level right) {
    Level julRight = julLevelFromLog4jLevel(right);
    return (left == null && right == null) || left.equals(julRight);
  }
}
