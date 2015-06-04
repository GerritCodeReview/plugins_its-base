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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.hooks.validation.ItsAssociationPolicy;

import org.eclipse.jgit.lib.Config;

import java.util.Arrays;

public class ItsConfigTest extends LoggingMockingTestCase {
  private Injector injector;

  private ProjectCache projectCache;
  private PluginConfigFactory pluginConfigFactory;
  private Config serverConfig;

  public void setupIsEnabled(String enabled, String parentEnabled,
      String[] branches) {
    ProjectState projectState = createMock(ProjectState.class);

    expect(projectCache.get(new Project.NameKey("testProject")))
        .andReturn(projectState).anyTimes();
    expect(projectCache.get(new Project.NameKey("parentProject")))
        .andReturn(projectState).anyTimes();

    Iterable<ProjectState> parents;
    if (parentEnabled == null) {
      parents = Arrays.asList(projectState);
    } else {
      ProjectState parentProjectState = createMock(ProjectState.class);

      PluginConfig parentPluginConfig = createMock(PluginConfig.class);

      expect(pluginConfigFactory.getFromProjectConfig(
          parentProjectState, "ItsTestName")).andReturn(parentPluginConfig);

      expect(parentPluginConfig.getString("enabled")).andReturn(parentEnabled)
          .anyTimes();

      PluginConfig parentPluginConfigWI = createMock(PluginConfig.class);

      expect(pluginConfigFactory.getFromProjectConfigWithInheritance(
          parentProjectState, "ItsTestName")).andReturn(parentPluginConfigWI)
          .anyTimes();

      String[] parentBranches = { "refs/heads/testBranch" };
      expect(parentPluginConfigWI.getStringList("branch"))
          .andReturn(parentBranches).anyTimes();

      parents = Arrays.asList(parentProjectState, projectState);
    }
    expect(projectState.treeInOrder()).andReturn(parents);

    PluginConfig pluginConfig = createMock(PluginConfig.class);

    expect(pluginConfigFactory.getFromProjectConfig(
        projectState, "ItsTestName")).andReturn(pluginConfig).anyTimes();

    expect(pluginConfig.getString("enabled")).andReturn(enabled).anyTimes();

    PluginConfig pluginConfigWI = createMock(PluginConfig.class);

    expect(pluginConfigFactory.getFromProjectConfigWithInheritance(
        projectState, "ItsTestName")).andReturn(pluginConfigWI).anyTimes();

    expect(pluginConfigWI.getBoolean("enabled", false))
        .andReturn("true".equals(enabled)).anyTimes();

    expect(pluginConfigWI.getStringList("branch")).andReturn(branches)
        .anyTimes();
  }

  public void testIsEnabledRefNoParentNoBranchEnabled() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNoBranchDisabled() {
    String[] branches = {};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNoBranchEnforced() {
    String[] branches = {};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchEnabled() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchDisabled() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchEnforced() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNonMatchingBranchEnabled() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNonMatchingBranchDisabled() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNonMatchingBranchEnforced() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleEnabled() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleDisabled() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleEnforced() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefParentNoBranchEnabled() {
    String[] branches = {};
    setupIsEnabled("false", "true", branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefParentNoBranchDisabled() {
    String[] branches = {};
    setupIsEnabled("false", "false", branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledRefParentNoBranchEnforced() {
    String[] branches = {};
    setupIsEnabled("false", "enforced", branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled("testProject", "refs/heads/testBranch"));
  }

  public void testIsEnabledEventNoBranches() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchExact() {
    String[] branches = {"refs/heads/testBranch"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchRegExp() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchNonMatchingRegExp() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchExact() {
    String[] branches = {"refs/heads/foo", "refs/heads/testBranch"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchRegExp() {
    String[] branches = {"refs/heads/foo.*", "refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchMixedMatchExact() {
    String[] branches = {"refs/heads/testBranch", "refs/heads/foo.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

public void testIsEnabledEventMultiBranchMixedMatchRegExp() {
    String[] branches = {"refs/heads/foo", "refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventDisabled() {
    String[] branches = {"^refs/heads/testBranch"};
    setupIsEnabled("false", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent();
    event.change = new ChangeAttribute();
    event.change.project = "testProject";
    event.change.branch = "testBranch";

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled(event));
  }

  public void testPatternNullMatch() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn(null).atLeastOnce();

    replayMocks();

    assertNull("Pattern for null match is not null",
        itsConfig.getIssuePattern());
  }

  public void testPattern() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("commentLink", "ItsTestName", "match"))
        .andReturn("TestPattern").atLeastOnce();

    replayMocks();

    assertEquals("Expected and generated pattern are not equal",
        "TestPattern", itsConfig.getIssuePattern().pattern());
  }

  public void testItsAssociationPolicyOptional() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getEnum("commentLink", "ItsTestName", "association",
        ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.OPTIONAL)
        .atLeastOnce();

    replayMocks();

    assertEquals("Expected and generated associated policy do not match",
        ItsAssociationPolicy.OPTIONAL, itsConfig.getItsAssociationPolicy());
  }

  public void testItsAssociationPolicySuggested() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getEnum("commentLink", "ItsTestName", "association",
        ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();

    replayMocks();

    assertEquals("Expected and generated associated policy do not match",
        ItsAssociationPolicy.SUGGESTED, itsConfig.getItsAssociationPolicy());
  }

  public void testItsAssociationPolicyMandatory() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getEnum("commentLink", "ItsTestName", "association",
        ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();

    replayMocks();

    assertEquals("Expected and generated associated policy do not match",
        ItsAssociationPolicy.MANDATORY, itsConfig.getItsAssociationPolicy());
  }

  private ItsConfig createItsConfig() {
    return injector.getInstance(ItsConfig.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      projectCache = createMock(ProjectCache.class);
      bind(ProjectCache.class).toInstance(projectCache);

      pluginConfigFactory = createMock(PluginConfigFactory.class);
      bind(PluginConfigFactory.class).toInstance(pluginConfigFactory);

      bind(String.class).annotatedWith(PluginName.class)
        .toInstance("ItsTestName");

      serverConfig = createMock(Config.class);
      bind(Config.class).annotatedWith(GerritServerConfig.class)
          .toInstance(serverConfig);
    }
  }
}
