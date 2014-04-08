/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TroubleClient.class, ByteArrayInputStream.class})
public class TroubleClientTest {
  private static final String TEST_IMPERSONATED_USER = "nisse";
  private static final int TEST_TICKET_ID = 50000;

  private static final String TEST_TICKET_JSON = "{\"id\":50000,\"created_at\":\"2013-06-04T12:23:48.000+02:00\",\"title\":\"AF type C\",\"reproduce\":\"\",\"frequency_id\":1,\"severity_id\":2,\"deleted_at\":null,\"corrected_at\":\"2013-12-05T16:05:30.000+01:00\",\"description\":\"Change request by Kent. See attached files for details.\",\"duplicate_of\":0,\"test_case_update\":false,\"release_note\":null,\"ticket_type_id\":2,\"top_level_project_id\":461,\"effort\":\"3\",\"delta\":true,\"development_mode\":\"embedded\",\"show_release_note\":true}";

  @Test()
  public void testGetTicket() throws Exception {
    // mock away TroubleClient.getRequest
    spy(TroubleClient.class);
    doReturn(TEST_TICKET_JSON).when(TroubleClient.class, "getRequest", anyString(), anyString(), anyString());

    TroubleClient client = TroubleClient.create("url", "username", "password", TEST_TICKET_ID);
    TroubleClient.Ticket ticket = client.getTicket();

    assertEquals("AF type C", ticket.title);
    assertEquals("Change request by Kent. See attached files for details.", ticket.description);
    assertEquals(new Integer(1), ticket.frequency);
    assertEquals(new Integer(2), ticket.severity);
  }

  @Test()
  public void testAuthHeader() throws Exception {
    String actual = Whitebox.invokeMethod(TroubleClient.class, "authHeader", "foo", "bar");
    assertEquals("Basic Zm9vOmJhcg==", actual);
  }

  @Test()
  public void testReadAll() throws Exception {
    String expected = "Hello World";
    ByteArrayInputStream mock = spy(new ByteArrayInputStream(expected.getBytes()));
    String actual = Whitebox.invokeMethod(TroubleClient.class, "readAll", mock);
    assertEquals(expected, actual);
    verify(mock, atLeast(1)).close(); // assert that close was called at least one time
  }
}
