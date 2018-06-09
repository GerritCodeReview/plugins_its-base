package com.googlesource.gerrit.plugins.its.base.workflow;

import static org.easymock.EasyMock.expect;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.MockingTestCase;
import java.io.IOException;
import java.util.Collections;
import org.easymock.EasyMock;

/**
 * Created on 03/06/18.
 *
 * @author Reda.Housni-Alaoui
 */
public class MarkPropertyAsReleasedVersionTest extends MockingTestCase {

  private static final String ITS_PROJECT = "test-project";
  private static final String PROPERTY_ID = "propertyId";
  private static final String PROPERTY_VALUE = "propertyValue";

  private Injector injector;
  private ItsFacade its;

  public void testNoParameter() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testTwoParameters() throws IOException {
    testWrongNumberOfReceivedParameters(new String[] {PROPERTY_ID, PROPERTY_ID});
  }

  public void testBlankPropertyId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {""});

    replayMocks();

    MarkPropertyAsReleasedVersion markPropertyAsReleasedVersion =
        createMarkPropertyAsReleasedVersion();
    markPropertyAsReleasedVersion.execute(ITS_PROJECT, actionRequest, Collections.emptyMap());
  }

  public void testUnknownPropertyId() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    replayMocks();

    MarkPropertyAsReleasedVersion markPropertyAsReleasedVersion =
        createMarkPropertyAsReleasedVersion();
    markPropertyAsReleasedVersion.execute(ITS_PROJECT, actionRequest, Collections.emptyMap());
  }

  public void testHappyPath() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    its.markVersionAsReleased(ITS_PROJECT, PROPERTY_VALUE);
    EasyMock.expectLastCall().once();

    replayMocks();

    MarkPropertyAsReleasedVersion markPropertyAsReleasedVersion =
        createMarkPropertyAsReleasedVersion();
    markPropertyAsReleasedVersion.execute(
        ITS_PROJECT, actionRequest, Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE));
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(parameters);

    replayMocks();

    MarkPropertyAsReleasedVersion markPropertyAsReleasedVersion =
        createMarkPropertyAsReleasedVersion();
    markPropertyAsReleasedVersion.execute(ITS_PROJECT, actionRequest, Collections.emptyMap());
  }

  private MarkPropertyAsReleasedVersion createMarkPropertyAsReleasedVersion() {
    return injector.getInstance(MarkPropertyAsReleasedVersion.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      its = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);
    }
  }
}
