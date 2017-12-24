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

package com.googlesource.gerrit.plugins.its.base.testutil;

import com.google.common.collect.Lists;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project;
import com.googlesource.gerrit.plugins.its.base.testutil.log.LogUtil;
import java.sql.Timestamp;
import java.util.Iterator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;

public abstract class LoggingMockingTestCase extends MockingTestCase {

  protected final Change.Key testChangeKey =
      new Change.Key("Ic19f7bf6c8b4685c363a8204c32d827ffda52ec0");
  protected final Change.Id testChangeId = new Change.Id(1);
  protected final Account.Id testAccountId = new Account.Id(1);

  private java.util.Collection<LogEvent> loggedEvents;

  protected final void assertLogMessageContains(String needle, Level level) {
    LogEvent hit = null;
    Iterator<LogEvent> iter = loggedEvents.iterator();
    while (hit == null && iter.hasNext()) {
      LogEvent event = iter.next();
      if (event.getMessage().contains(needle)) {
        if (level == null || level.equals(event.getLevel())) {
          hit = event;
        }
      }
    }
    assertNotNull("Could not find log message containing '" + needle + "'", hit);
    assertTrue(
        "Could not remove log message containing '" + needle + "'", loggedEvents.remove(hit));
  }

  protected final void assertLogMessageContains(String needle) {
    assertLogMessageContains(needle, null);
  }

  protected final void assertLogThrowableMessageContains(String needle) {
    LogEvent hit = null;
    Iterator<LogEvent> iter = loggedEvents.iterator();
    while (hit == null && iter.hasNext()) {
      LogEvent event = iter.next();

      if (event.getThrown().toString().contains(needle)) {
        hit = event;
      }
    }
    assertNotNull("Could not find log message with a Throwable containing '" + needle + "'", hit);
    assertTrue(
        "Could not remove log message with a Throwable containing '" + needle + "'",
        loggedEvents.remove(hit));
  }

  // As the PowerMock runner does not pass through runTest, we inject log
  // verification through @After
  @After
  public final void assertNoUnassertedLogEvents() {
    if (loggedEvents.size() > 0) {
      LogEvent event = loggedEvents.iterator().next();
      String msg = "Found untreated logged events. First one is:\n";
      msg += event.getMessage();
      if (event.getThrown() != null) {
        msg += "\n" + event.getThrown();
      }
      fail(msg);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    loggedEvents = Lists.newArrayList();

    // The logger we're interested is class name without the trailing "Test".
    // While this is not the most general approach it is sufficient for now,
    // and we can improve later to allow tests to specify which loggers are
    // to check.
    String logName = this.getClass().getCanonicalName();
    logName = logName.substring(0, logName.length() - 4);
    LogUtil.logToCollection(logName, loggedEvents);
  }

  @Override
  protected void runTest() throws Throwable {
    super.runTest();
    // Plain JUnit runner does not pick up @After, so we add it here
    // explicitly. Note, that we cannot put this into tearDown, as failure
    // to verify mocks would bail out and might leave open resources from
    // subclasses open.
    assertNoUnassertedLogEvents();
  }

  protected Change testChange(String project, String branch) {
    return new Change(
        testChangeKey,
        testChangeId,
        testAccountId,
        new Branch.NameKey(new Project.NameKey(project), branch),
        new Timestamp(System.currentTimeMillis()));
  }
}
