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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.googlesource.gerrit.plugins.its.base.testutil.log.LogUtil;
import java.time.Instant;
import java.util.Iterator;
import java.util.logging.LogRecord;
import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.junit.After;

public abstract class LoggingMockingTestCase extends TestCase {

  protected final Change.Key testChangeKey =
      Change.key("Ic19f7bf6c8b4685c363a8204c32d827ffda52ec0");
  protected final Change.Id testChangeId = Change.id(1);
  protected final Account.Id testAccountId = Account.id(1);

  private java.util.Collection<LogRecord> records;

  protected final void assertLogMessageContains(String needle, Level level, int times) {
    // We do not support `times == 0`, as it's ambiguous if it means the message does not occur at
    // all, or message assertion should be skipped.
    assertThat(times).isGreaterThan(0);

    while (times-- > 0) {
      LogRecord hit = null;
      Iterator<LogRecord> iter = records.iterator();
      while (hit == null && iter.hasNext()) {
        LogRecord record = iter.next();
        if (record.getMessage().contains(needle)) {
          if (level == null || LogUtil.equalLevels(record.getLevel(), level)) {
            hit = record;
          }
        }
      }
      removeLogHit(hit, "containing '" + needle + "'");
    }
  }

  protected final void assertLogMessageContains(String needle, Level level) {
    assertLogMessageContains(needle, level, 1);
  }

  protected final void assertLogMessageContains(String needle) {
    assertLogMessageContains(needle, null);
  }

  protected final void assertLogMessageContains(String needle, int times) {
    assertLogMessageContains(needle, null, times);
  }

  protected final void assertLogThrowableMessageContains(String needle) {
    LogRecord hit = null;
    Iterator<LogRecord> iter = records.iterator();
    while (hit == null && iter.hasNext()) {
      LogRecord record = iter.next();

      Throwable t = record.getThrown();
      if (t != null && t.toString().contains(needle)) {
        hit = record;
      }
    }
    removeLogHit(hit, "with a Throwable containing '\" + needle + \"'");
  }

  private void removeLogHit(LogRecord hit, String description) {
    if (hit == null) {
      failWithUnassertedLogDump("Could not find log message " + description);
    }
    assertTrue("Could not remove log message " + description, records.remove(hit));
  }

  // As the PowerMock runner does not pass through runTest, we inject log
  // verification through @After
  @After
  public final void assertNoUnassertedLogEvents() {
    if (records.size() > 0) {
      failWithUnassertedLogDump("Found unasserted logged events.");
    }
  }

  public final void failWithUnassertedLogDump(String msg) {
    msg += "\n";
    if (records.size() == 0) {
      msg += "(All logged messages have already been asserted)";
    } else {
      msg += records.size() + " logged, but not yet asserted messages remain:";
      for (LogRecord record : records) {
        msg += "\n" + record.getMessage();
        Throwable t = record.getThrown();
        if (t != null) {
          msg += "\n   with thrown " + t;
        }
      }
    }
    fail(msg);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    records = Lists.newArrayList();

    // The logger we're interested is class name without the trailing "Test".
    // While this is not the most general approach it is sufficient for now,
    // and we can improve later to allow tests to specify which loggers are
    // to check.
    String logName = this.getClass().getCanonicalName();
    logName = logName.substring(0, logName.length() - 4);
    LogUtil.logToCollection(logName, records, Level.DEBUG);
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
        BranchNameKey.create(Project.nameKey(project), branch),
        Instant.now());
  }
}
