package com.googlesource.gerrit.plugins.its.base.workflow;

import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Collections;
import java.util.Optional;
import junit.framework.TestCase;

public class CreateVersionFromPropertyParametersExtractorTest extends TestCase {
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
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(parameters);

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testBlankPropertyId() {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {""});

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testUnknownPropertyId() {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {PROPERTY_ID});

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.emptyMap());
    assertFalse(extractedParameters.isPresent());
  }

  public void testHappyPath() {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getParameters()).thenReturn(new String[] {PROPERTY_ID});

    Optional<CreateVersionFromPropertyParameters> extractedParameters =
        extractor.extract(actionRequest, Collections.singletonMap(PROPERTY_ID, PROPERTY_VALUE));
    assertThat(extractedParameters).isPresent();
    assertEquals(PROPERTY_VALUE, extractedParameters.get().getPropertyValue());
  }
}
