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

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;

public class PropertyTest  extends LoggingMockingTestCase {
  private Injector injector;

  public void testGetKeyNull() {
    Property property = new Property(null, "testValue");
    assertNull("Key is not null", property.getKey());
  }

  public void testGetKeyNonNull() {
    Property property = createProperty("testKey", "testValue");
    assertEquals("Key does not match", "testKey", property.getKey());
  }

  public void testGetValueNull() {
    Property property = createProperty("testKey", null);
    assertNull("Value is not null", property.getValue());
  }

  public void testGetValueNonNull() {
    Property property = createProperty("testKey", "testValue");
    assertEquals("Value does not match", "testValue", property.getValue());
  }

  public void testEqualsSelf() {
    Property property = createProperty("testKey", "testValue");
    assertTrue("Property not equal to itself", property.equals(property));
  }

  public void testEqualsSimilar() {
    Property propertyA = createProperty("testKey", "testValue");
    Property propertyB = createProperty("testKey", "testValue");
    assertTrue("Property is equal to similar", propertyA.equals(propertyB));
  }

  public void testEqualsNull() {
    Property property = createProperty("testKey", "testValue");
    assertFalse("Property is equal to null", property.equals(null));
  }

  public void testEqualsNull2() {
    Property property = new Property(null, null);
    assertFalse("Property is equal to null", property.equals(null));
  }

  public void testEqualsNulledKey() {
    Property propertyA = new Property(null, "testValue");
    Property propertyB = createProperty("testKey", "testValue");
    assertFalse("Single nulled key does match",
        propertyA.equals(propertyB));
  }

  public void testEqualsNulledKey2() {
    Property propertyA = createProperty("testKey", "testValue");
    Property propertyB = new Property(null, "testValue");
    assertFalse("Single nulled key does match",
        propertyA.equals(propertyB));
  }

  public void testEqualsNulledValue() {
    Property propertyA = createProperty("testKey", "testValue");
    Property propertyB = createProperty("testKey", null);
    assertFalse("Single nulled value does match",
        propertyA.equals(propertyB));
  }

  public void testEqualsNulledValue2() {
    Property propertyA = createProperty("testKey", null);
    Property propertyB = createProperty("testKey", "testValue");
    assertFalse("Single nulled value does match",
        propertyA.equals(propertyB));
  }

  public void testHashCodeEquals() {
    Property propertyA = createProperty("testKey", "testValue");
    Property propertyB = createProperty("testKey", "testValue");
    assertEquals("Hash codes do not match", propertyA.hashCode(),
        propertyB.hashCode());
  }

  public void testHashCodeNullKey() {
    Property property = new Property(null, "testValue");
    property.hashCode();
  }

  public void testHashCodeNullValue() {
    Property property = createProperty("testKey", null);
    property.hashCode();
  }

  private Property createProperty(String key, String value) {
    Property.Factory factory = injector.getInstance(Property.Factory.class);
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
      factory(Property.Factory.class);
    }
  }
}