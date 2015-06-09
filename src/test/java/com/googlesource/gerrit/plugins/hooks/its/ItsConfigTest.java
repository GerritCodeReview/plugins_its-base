// Copyright (C) 2015 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.hooks.its;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;

import com.googlesource.gerrit.plugins.its.ItsConfig;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class ItsConfigTest {
  private EasyMockSupport easyMock;
  private String pluginName = "its-base";
  private PluginConfig pluginConfig;
  private PluginConfig pluginConfigWithInheritance;
  private ItsConfig itsConfig;
  private PatchSetCreatedEvent pscEvent;

  @Before
  public void setup() {
    easyMock = new EasyMockSupport();
    ProjectCache projectCache = easyMock.createNiceMock(ProjectCache.class);
    PluginConfigFactory pluginCfgFactory = easyMock.createNiceMock(PluginConfigFactory.class);
    ProjectState projectState = easyMock.createNiceMock(ProjectState.class);
    pluginConfig = easyMock.createNiceMock(PluginConfig.class);
    pluginConfigWithInheritance = easyMock.createNiceMock(PluginConfig.class);
    itsConfig = new ItsConfig(pluginName, projectCache, pluginCfgFactory);

    pscEvent = new PatchSetCreatedEvent();
    pscEvent.change = new ChangeAttribute();
    pscEvent.change.project = "testProject";
    pscEvent.change.branch = "testBranch";
    ArrayList<ProjectState> listProjectState = new ArrayList<>(1);
    listProjectState.add(projectState);

    expect(projectCache.get(new Project.NameKey("testProject"))).andReturn(
        projectState).once();
    expect(projectState.treeInOrder()).andReturn(listProjectState);
    expect(pluginCfgFactory.getFromProjectConfig(projectState, pluginName))
    .andStubReturn(pluginConfig);
    expect(
        pluginCfgFactory.getFromProjectConfigWithInheritance(projectState,
            pluginName)).andStubReturn(pluginConfigWithInheritance);
  }

  @Test
  public void testEnforcedWithRegex() {
    String[] refPatterns = {"^refs/heads/test.*"};
    setUpEnforced(refPatterns);

    assertTrue(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  @Test
  public void testEnforcedWithExact() {
    String[] refPatterns = {"refs/heads/testBranch"};
    setUpEnforced(refPatterns);

    assertTrue(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  @Test
  public void testEnforcedForAll() {
    String[] refPatterns = new String[0];
    setUpEnforced(refPatterns);

    assertTrue(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  @Test
  public void testEnabledWithRegex() {
    String[] refPatterns = {"^refs/heads/test.*"};
    setUpEnabled(refPatterns);

    assertTrue(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  @Test
  public void testEnabledWithExact() {
    String[] refPatterns = {"refs/heads/testBranch"};
    setUpEnabled(refPatterns);

    assertTrue(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  @Test
  public void testDisabled() {
    String[] refPatterns = {"refs/heads/testBranch1"};
    setUpEnabled(refPatterns);

    assertFalse(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  @Test
  public void testInvalidPattern() {
    String[] refPatterns = {"testBranch"};
    setUpEnabled(refPatterns);

    assertFalse(itsConfig.isEnabled(pscEvent));
    easyMock.verifyAll();
  }

  private void setUpEnforced(String[] refPatterns) {
    expect(pluginConfig.getString("enabled")).andReturn("enforced").once();
    expect(pluginConfigWithInheritance.getStringList("branch")).andReturn(refPatterns);
    easyMock.replayAll();
  }

  private void setUpEnabled(String[] refPatterns) {
    expect(pluginConfig.getString("enabled")).andReturn("true");
    expect(pluginConfigWithInheritance.getBoolean("enabled", false)).andReturn(true);
    expect(pluginConfigWithInheritance.getStringList("branch")).andReturn(refPatterns);
    easyMock.replayAll();
  }
}
