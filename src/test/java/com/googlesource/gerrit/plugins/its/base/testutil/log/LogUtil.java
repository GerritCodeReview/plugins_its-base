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

/** Utility functions for dealing with various Loggers */
public class LogUtil {
  /**
   * Makes sure that Loggers for a given name add their events to a collection
   *
   * <p>This is useful for capturing logged events during testing and being able to run assertions
   * on them.
   *
   * <p>We will never be able to cover all possible backends of the Logging flavour of the day. For
   * now we cover Log4j and JUL. If you use a different default logging backend, please send in
   * patches.
   *
   * @param logName The name of the loggers to make log to the given collection.
   * @param collection The collection to add log events to.
   * @param level The level the logger should be set to.
   */
  public static CollectionAppender logToCollection(
      String logName, Collection<LogRecord> collection, org.apache.log4j.Level level) {

    CollectionAppender appender = new CollectionAppender(collection);

    logToCollectionLog4j(logName, appender, level);
    logToCollectionJul(logName, appender, julLevelFromLog4jLevel(level));

    return appender;
  }

  /**
   * Make a Log4j logger log to a given appender at a certain level
   *
   * @param logName The logger that should log to the appender.
   * @param appender The appender to log to.
   * @param level The level to user for the logger.
   */
  private static void logToCollectionLog4j(
      String logName, CollectionAppender appender, org.apache.log4j.Level level) {
    Logger log = LogManager.getLogger(logName);
    log.setLevel(level);
    log.removeAllAppenders();
    log.addAppender(appender);
  }

  /**
   * Make a java.util.logging logger log to a given appender at a certain level
   *
   * @param logName The logger that should log to the appender.
   * @param appender The appender to log to.
   * @param level The level to user for the logger.
   */
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

  /**
   * Converts a Log4j Logging Event to a JUL LogRecord
   *
   * <p>This is not a full conversion, but covers only the fields we care about. That is the logged
   * message and eventual thrown Throwables.
   *
   * @param event The Log4j LoggingEvent to convert
   * @return The corresponding JUL LogRecord
   */
  public static LogRecord logRecordFromLog4jLoggingEvent(LoggingEvent event) {
    LogRecord logRecord = new LogRecord(Level.ALL, event.getRenderedMessage());
    ThrowableInformation tInfo = event.getThrowableInformation();
    if (tInfo != null) {
      logRecord.setThrown(tInfo.getThrowable());
    }
    return logRecord;
  }

  /**
   * Convert a Log4j Level to a corresponding JUL Level
   *
   * @param level The Log4j Level to convert
   * @return The corresponding JUL Level
   */
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

  /**
   * Check if a JUL and a Log4j Level correspond
   *
   * @param left The JUL Level to check.
   * @param right The Log4j Level to check.
   * @return True, if and only if the levels correspond.
   */
  public static boolean equalLevels(Level left, org.apache.log4j.Level right) {
    Level julRight = julLevelFromLog4jLevel(right);
    return (left == null && right == null) || (left != null && left.equals(julRight));
  }
}
