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
package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.util.Map;

public class ConditionTest extends LoggingMockingTestCase {
  private Injector injector;

  public void testGetKeyNull() {
    Condition condition = new Condition(null, "testValues");
    assertNull("key is not null", condition.getKey());
  }

  public void testIsMetBySimple() {
    Condition condition = createCondition("testKey", "testValue");

    Map<String, String> properties = ImmutableMap.of("testKey", "testValue");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetBySimpleEmpty() {
    Condition condition = createCondition("testKey", "testValue");

    Map<String, String> properties = ImmutableMap.of();

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testIsMetByMismatchedKey() {
    Condition condition = createCondition("testKey", "testValue");

    Map<String, String> properties = ImmutableMap.of("otherKey", "testValue");

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testIsMetByMismatchedValue() {
    Condition condition = createCondition("testKey", "testValue");

    Map<String, String> properties = ImmutableMap.of("testKey", "otherValue");

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testIsMetByOredSingle() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value2");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredMultiple() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value1 value3");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredMultipleWithSpaces() {
    Condition condition = createCondition("testKey", "value1, value2 value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value1 value3");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredAll() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value1 value2 value3");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testIsMetByOredOvershoot() {
    Condition condition = createCondition("testKey", "value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "otherValue1 value2 otherValue3");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByEmpty() {
    Condition condition = createCondition("testKey", "!,testValue");

    Map<String, String> properties = ImmutableMap.of();

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByMismatchedKey() {
    Condition condition = createCondition("testKey", "!,testValue");

    Map<String, String> properties = ImmutableMap.of("otherKey", "testValue");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByMaMismatchedValue() {
    Condition condition = createCondition("testKey", "!,testValue");

    Map<String, String> properties = ImmutableMap.of("testKey", "otherValue");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByOredNoMatch() {
    Condition condition = createCondition("testKey", "!,value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "otherValue");

    assertTrue("isMetBy gave false", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByOredSingleMatch() {
    Condition condition = createCondition("testKey", "!,value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value1");

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByOredMultiple() {
    Condition condition = createCondition("testKey", "!,value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value1 value3");

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByOredAll() {
    Condition condition = createCondition("testKey", "!,value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "value1 value2 value3");

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  public void testNegatedIsMetByOredOvershoot() {
    Condition condition = createCondition("testKey", "!,value1,value2,value3");

    Map<String, String> properties = ImmutableMap.of("testKey", "otherValue1 value2 otherValue3");

    assertFalse("isMetBy gave true", condition.isMetBy(properties));
  }

  private Condition createCondition(String key, String value) {
    Condition.Factory factory = injector.getInstance(Condition.Factory.class);
    return factory.create(key, value);
  }

  @Override
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
