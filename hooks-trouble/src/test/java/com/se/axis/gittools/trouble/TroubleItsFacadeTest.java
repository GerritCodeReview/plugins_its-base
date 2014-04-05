/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest()
public class TroubleItsFacadeTest {
  private static final String EXPECTED_REVISION = "f960de25c1f237d0f5a354c9458ca276a6afa374";
  private static final String EXPECTED_BRANCH = "user/zalanb/test";

  @Test()
  public void testParseRevision() throws Exception {
    String actual = Whitebox.invokeMethod(TroubleItsFacade.class, "parseRevision", EXPECTED_REVISION);
    assertEquals(EXPECTED_REVISION, actual);
    actual = Whitebox.invokeMethod(TroubleItsFacade.class, "parseRevision", EXPECTED_REVISION + " (" + EXPECTED_BRANCH + ")");
    assertEquals(EXPECTED_REVISION, actual);
    actual = Whitebox.invokeMethod(TroubleItsFacade.class, "parseRevision",
      EXPECTED_REVISION + " (" + EXPECTED_BRANCH + "/1, " + EXPECTED_BRANCH + ")");
    assertEquals(EXPECTED_REVISION, actual);
  }

  @Test()
  public void testParseSourceBranch() throws Exception {
    String actual = Whitebox.invokeMethod(TroubleItsFacade.class, "parseSourceBranch", EXPECTED_REVISION);
    assertEquals(null, actual);
    actual = Whitebox.invokeMethod(TroubleItsFacade.class, "parseSourceBranch", EXPECTED_REVISION + " (" + EXPECTED_BRANCH + ")");
    assertEquals(EXPECTED_BRANCH, actual);
    actual = Whitebox.invokeMethod(TroubleItsFacade.class, "parseSourceBranch",
      EXPECTED_REVISION + " (" + EXPECTED_BRANCH + "/1, " + EXPECTED_BRANCH + ")");
    assertEquals(EXPECTED_BRANCH, actual);
  }

}
