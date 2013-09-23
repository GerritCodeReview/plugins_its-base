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
package com.googlesource.gerrit.plugins.hooks.workflow;

import static org.easymock.EasyMock.expect;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

public class ConditionTest  extends LoggingMockingTestCase {
  private Injector injector;

  public void testSimpleValue() {
    Condition condition = createCondition("testKey", "testValue");
    assertEquals("key not matching 'testKey'", "testKey", condition.getKey());
    Set<String> expectedValues = Sets.newHashSet();
    expectedValues.add("testValue");
    assertEquals("values do not match", expectedValues, condition.getValues());
  }

  public void testGetKeyNull() {
    Condition condition = new Condition(null, "testValues");
    assertNull("key is not null", condition.getKey());
  }

  public void testGetValuesNull() {
    Condition condition = createCondition("testKey", null);
    Set<String> values = condition.getValues();
    assertNotNull("values is null", values);
    assertTrue("values is not empty", values.isEmpty());
  }

  public void testOredValue() {
    Condition condition = createCondition("testKey", "value1,value2,value3");
    assertEquals("key not matching 'testKey'", "testKey", condition.getKey());
    Set<String> expectedValues = Sets.newLinkedHashSet();
    expectedValues.add("value1");
    expectedValues.add("value2");
    expectedValues.add("value3");
    assertEquals("values do not match", expectedValues, condition.getValues());
  }

  public void testIsMetBySimple() {
    Condition condition = createCondition("testKey", "testValue");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("testKey").anyTimes();
    expect(property1.getValue()).andReturn("testValue").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(1);
    properties.add(property1);

    replayMocks();

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetBySimpleEmpty() {
    Condition condition = createCondition("testKey", "testValue");

    Collection<Property> properties = Collections.emptySet();

    replayMocks();

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testIsMetByMismatchedKey() {
    Condition condition = createCondition("testKey", "testValue");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("otherKey").anyTimes();
    expect(property1.getValue()).andReturn("testValue").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(1);
    properties.add(property1);

    replayMocks();

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testIsMetByMismatchedValue() {
    Condition condition = createCondition("testKey", "testValue");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("testKey").anyTimes();
    expect(property1.getValue()).andReturn("otherValue").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(1);
    properties.add(property1);

    replayMocks();

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testIsMetByOredSingle() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("testKey").anyTimes();
    expect(property1.getValue()).andReturn("value2").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(1);
    properties.add(property1);

    replayMocks();

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredMultiple() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("testKey").anyTimes();
    expect(property1.getValue()).andReturn("value1").anyTimes();

    Property property2 = createMock(Property.class);
    expect(property2.getKey()).andReturn("testKey").anyTimes();
    expect(property2.getValue()).andReturn("value3").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(2);
    properties.add(property1);
    properties.add(property2);

    replayMocks();

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredAll() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("testKey").anyTimes();
    expect(property1.getValue()).andReturn("value1").anyTimes();

    Property property2 = createMock(Property.class);
    expect(property2.getKey()).andReturn("testKey").anyTimes();
    expect(property2.getValue()).andReturn("value2").anyTimes();

    Property property3 = createMock(Property.class);
    expect(property3.getKey()).andReturn("testKey").anyTimes();
    expect(property3.getValue()).andReturn("value3").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(1);
    properties.add(property1);
    properties.add(property2);
    properties.add(property3);

    replayMocks();

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredOvershoot() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Property property1 = createMock(Property.class);
    expect(property1.getKey()).andReturn("testKey").anyTimes();
    expect(property1.getValue()).andReturn("otherValue1").anyTimes();

    Property property2 = createMock(Property.class);
    expect(property2.getKey()).andReturn("testKey").anyTimes();
    expect(property2.getValue()).andReturn("value2").anyTimes();

    Property property3 = createMock(Property.class);
    expect(property3.getKey()).andReturn("testKey").anyTimes();
    expect(property3.getValue()).andReturn("otherValue3").anyTimes();

    Collection<Property> properties = Lists.newArrayListWithCapacity(3);
    properties.add(property1);
    properties.add(property2);
    properties.add(property3);

    replayMocks();

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testUnmodifiableValue() {
    Condition condition = createCondition("testKey", "testValue");
    Set<String> values = condition.getValues();
    try {
      values.add("value2");
      fail("value is not unmodifyable");
    } catch (UnsupportedOperationException e) {
    }
  }

  private Condition createCondition(String key, String value) {
    Condition.Factory factory = injector.getInstance(Condition.Factory.class);
    return factory.create(key, value);
  }

  public void setUp() throws Exception {
    super.setUp();

    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      factory(Condition.Factory.class);
    }
  }
}