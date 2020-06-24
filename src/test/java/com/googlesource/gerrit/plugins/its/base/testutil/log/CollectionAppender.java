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

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.LogRecord;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/** Log4j appender that logs into a list */
public class CollectionAppender extends AppenderSkeleton {
  private Collection<LogRecord> events;

  public CollectionAppender() {
    events = new LinkedList<>();
  }

  public CollectionAppender(Collection<LogRecord> events) {
    this.events = events;
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  @Override
  protected void append(LoggingEvent event) {
    LogRecord logRecord = LogUtil.logRecordFromLog4jLoggingEvent(event);
    append(logRecord);
  }

  public void append(LogRecord record) {
    if (!events.add(record)) {
      throw new RuntimeException("Could not append event " + record);
    }
  }

  @Override
  public void close() {}

  public Collection<LogRecord> getLoggedEvents() {
    return Lists.newLinkedList(events);
  }
}
