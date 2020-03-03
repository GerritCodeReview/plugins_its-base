package com.googlesource.gerrit.plugins.its.base.workflow;

import static org.easymock.EasyMock.expect;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.MockingTestCase;
import java.util.Collections;
import java.util.Optional;

public class CreateVersionFromPropertyParametersExtractorTest extends MockingTestCase {

  private static final String ITS_PROJECT = "test-project";
  private static final String PROPERTY_ID = "propertyId";
  private static final String PROPERTY_VALUE = "propertyValue";

  private CreateVersionFromPropertyParametersExtractor extractor;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector injector = Guice.createInjector(new TestModule());
    extractor = injector.getInstance(CreateVersionFromPropertyParametersExtractor.class);
  }

  private class TestModule extends FactoryModule {}

  public void testNoParameter() {
    testWrongNumberOfReceivedParameters(new String[] {});
  }

  public void testTwoParameters() {
    testWrongNumberOfReceivedParameters(new String[] {PROPERTY_ID, PROPERTY_ID});
  }

  private void testWrongNumberOfReceivedParameters(String[] parameters) {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(parameters);

    replayMocks();

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testBlankPropertyId() {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {""});

    replayMocks();

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testUnknownPropertyId() {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    replayMocks();

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testHappyPath() {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameters()).andReturn(new String[] {PROPERTY_ID});

    replayMocks();

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE));
    if (!extractedParameters.isPresent()) {
      fail();
    }
    assertEquals(PROPERTY_VALUE, extractedParameters.get().getPropertyValue());
  }
}
