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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/** Log4j2 appender that logs into a list */
@Plugin(name="CollectionAppender", category="Core", elementType="appender", printObject=true)
public class CollectionAppender extends AbstractAppender {
  private Collection<LogEvent> events;

  public CollectionAppender() {
    events = new LinkedList<>();
  }

  public CollectionAppender(Collection<LogEvent> events) {
    this.events = events;
  }

  @Override
  public void append(LogEvent event) {
    if (!events.add(event)) {
      throw new RuntimeException("Could not append event " + event);
    }
  }

  @Override
  public void close() {}

  public Collection<LogEvent> getLoggedEvents() {
    return Lists.newLinkedList(events);
  }
}
