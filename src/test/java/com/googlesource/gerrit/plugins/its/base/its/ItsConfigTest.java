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

package com.googlesource.gerrit.plugins.its.base.its;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.validation.ItsAssociationPolicy;
import java.util.Arrays;
import org.eclipse.jgit.lib.Config;

public class ItsConfigTest extends LoggingMockingTestCase {
  private Injector injector;

  private ProjectCache projectCache;
  private PluginConfigFactory pluginConfigFactory;
  private Config serverConfig;

  public void setupIsEnabled(String enabled, String parentEnabled, String[] branches) {
    ProjectState projectState = createMock(ProjectState.class);

    expect(projectCache.get(new Project.NameKey("testProject"))).andReturn(projectState).anyTimes();
    expect(projectCache.get(new Project.NameKey("parentProject")))
        .andReturn(projectState)
        .anyTimes();

    Iterable<ProjectState> parents;
    if (parentEnabled == null) {
      parents = Arrays.asList(projectState);
    } else {
      ProjectState parentProjectState = createMock(ProjectState.class);

      PluginConfig parentPluginConfig = createMock(PluginConfig.class);

      expect(pluginConfigFactory.getFromProjectConfig(parentProjectState, "ItsTestName"))
          .andReturn(parentPluginConfig);

      expect(parentPluginConfig.getString("enabled", "false")).andReturn(parentEnabled).anyTimes();

      PluginConfig parentPluginConfigWI = createMock(PluginConfig.class);

      expect(
              pluginConfigFactory.getFromProjectConfigWithInheritance(
                  parentProjectState, "ItsTestName"))
          .andReturn(parentPluginConfigWI)
          .anyTimes();

      String[] parentBranches = {"refs/heads/testBranch"};
      expect(parentPluginConfigWI.getStringList("branch")).andReturn(parentBranches).anyTimes();

      parents = Arrays.asList(parentProjectState, projectState);
    }
    expect(projectState.treeInOrder()).andReturn(parents);

    PluginConfig pluginConfig = createMock(PluginConfig.class);

    expect(pluginConfigFactory.getFromProjectConfig(projectState, "ItsTestName"))
        .andReturn(pluginConfig)
        .anyTimes();

    expect(pluginConfig.getString("enabled", "false")).andReturn(enabled).anyTimes();

    PluginConfig pluginConfigWI = createMock(PluginConfig.class);

    expect(pluginConfigFactory.getFromProjectConfigWithInheritance(projectState, "ItsTestName"))
        .andReturn(pluginConfigWI)
        .anyTimes();

    expect(pluginConfigWI.getString("enabled", "false")).andReturn(enabled).anyTimes();

    expect(pluginConfigWI.getStringList("branch")).andReturn(branches).anyTimes();
  }

  public void testIsEnabledRefNoParentNoBranchEnabled() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNoBranchDisabled() {
    String[] branches = {};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNoBranchEnforced() {
    String[] branches = {};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchEnabled() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentMatchingBranchDisabled() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchEnforced() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNonMatchingBranchEnabled() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNonMatchingBranchDisabled() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNonMatchingBranchEnforced() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleEnabled() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("true", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentMatchingBranchMiddleDisabled() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("false", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleEnforced() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("enforced", null, branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefParentNoBranchEnabled() {
    String[] branches = {};
    setupIsEnabled("false", "true", branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefParentNoBranchDisabled() {
    String[] branches = {};
    setupIsEnabled("false", "false", branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefParentNoBranchEnforced() {
    String[] branches = {};
    setupIsEnabled("false", "enforced", branches);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    Project.NameKey projectNK = new Project.NameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledEventNoBranches() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchExact() {
    String[] branches = {"refs/heads/testBranch"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchRegExp() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void BROKEN_testIsEnabledEventSingleBranchNonMatchingRegExp() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchExact() {
    String[] branches = {"refs/heads/foo", "refs/heads/testBranch"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchRegExp() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchMixedMatchExact() {
    String[] branches = {"refs/heads/testBranch", "refs/heads/foo.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchMixedMatchRegExp() {
    String[] branches = {"refs/heads/foo", "^refs/heads/test.*"};
    setupIsEnabled("true", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void BROKEN_testIsEnabledEventDisabled() {
    String[] branches = {"^refs/heads/testBranch"};
    setupIsEnabled("false", null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled(event));
  }

  public void testIsEnabledCommentAddedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    CommentAddedEvent event = new CommentAddedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledChangeMergedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    ChangeMergedEvent event = new ChangeMergedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledChangeAbandonedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    ChangeAbandonedEvent event = new ChangeAbandonedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledChangeRestoredEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    ChangeRestoredEvent event = new ChangeRestoredEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

<<<<<<< HEAD
=======
  public void testIsEnabledDraftPublishedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    DraftPublishedEvent event = new DraftPublishedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

>>>>>>> stable-2.14
  public void testIsEnabledRefUpdatedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, branches);

    RefUpdatedEvent event = new RefUpdatedEvent();
    RefUpdateAttribute refUpdateAttribute = new RefUpdateAttribute();
    refUpdateAttribute.project = "testProject";
    refUpdateAttribute.refName = "refs/heads/testBranch";
    event.refUpdate = Suppliers.ofInstance(refUpdateAttribute);

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void BROKEN_testIsEnabledUnknownEvent() {
    Event event = new Event("foo") {};

    ItsConfig itsConfig = createItsConfig();

    replayMocks();

    assertFalse(itsConfig.isEnabled(event));
    assertLogMessageContains("not recognised and ignored");
  }

  public void testGetIssuePatternNullMatch() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn(null)
        .atLeastOnce();

    replayMocks();

    assertNull("Pattern for null match is not null", itsConfig.getIssuePattern());
  }

  public void testGetIssuePatternNullMatchWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn("foo")
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "foo", "match")).andReturn(null).atLeastOnce();

    replayMocks();

    assertNull("Pattern for null match is not null", itsConfig.getIssuePattern());
  }

  public void testGetIssuePattern() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("TestPattern")
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated pattern are not equal",
        "TestPattern",
        itsConfig.getIssuePattern().pattern());
  }

  public void testGetIssuePatternWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn("foo")
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "foo", "match"))
        .andReturn("TestPattern")
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated pattern are not equal",
        "TestPattern",
        itsConfig.getIssuePattern().pattern());
  }

  public void testGetIssuePatternGroupIndexGroupDefault() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("(foo)(bar)(baz)")
        .atLeastOnce();
    expect(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1))
        .andReturn(1)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and actual group index do not match", 1, itsConfig.getIssuePatternGroupIndex());
  }

  public void testGetIssuePatternGroupIndexGroupDefaultGroupless() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("foo")
        .atLeastOnce();
    expect(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1))
        .andReturn(1)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and actual group index do not match", 0, itsConfig.getIssuePatternGroupIndex());
  }

  public void testGetIssuePatternGroupIndexGroup1() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("(foo)(bar)(baz)")
        .atLeastOnce();
    expect(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1))
        .andReturn(1)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and actual group index do not match", 1, itsConfig.getIssuePatternGroupIndex());
  }

  public void testGetIssuePatternGroupIndexGroup3() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("(foo)(bar)(baz)")
        .atLeastOnce();
    expect(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1))
        .andReturn(3)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and actual group index do not match", 3, itsConfig.getIssuePatternGroupIndex());
  }

  public void testGetIssuePatternGroupIndexGroupTooHigh() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("(foo)(bar)(baz)")
        .atLeastOnce();
    expect(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1))
        .andReturn(5)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and actual group index do not match", 1, itsConfig.getIssuePatternGroupIndex());
  }

  public void testGetIssuePatternGroupIndexGroupTooHighGroupless() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .andReturn("foo")
        .atLeastOnce();
    expect(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1))
        .andReturn(5)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and actual group index do not match", 0, itsConfig.getIssuePatternGroupIndex());
  }

  public void testGetItsAssociationPolicyOptional() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.OPTIONAL)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "plugin", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.OPTIONAL)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.OPTIONAL,
        itsConfig.getItsAssociationPolicy());
  }

  public void testGetItsAssociationPolicyOptionalWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn("foo")
        .atLeastOnce();
    expect(serverConfig.getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.OPTIONAL)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "plugin", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.OPTIONAL)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.OPTIONAL,
        itsConfig.getItsAssociationPolicy());
  }

  public void testGetItsAssociationPolicySuggested() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "plugin", "ItsTestName", "association", ItsAssociationPolicy.SUGGESTED))
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    replayMocks();

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.SUGGESTED,
        itsConfig.getItsAssociationPolicy());
  }

  public void testGetItsAssociationPolicySuggestedWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn("foo")
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "plugin", "ItsTestName", "association", ItsAssociationPolicy.SUGGESTED))
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(serverConfig.getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.SUGGESTED,
        itsConfig.getItsAssociationPolicy());
  }

  public void testGetItsAssociationPolicyMandatory() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn(null)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "plugin", "ItsTestName", "association", ItsAssociationPolicy.MANDATORY))
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.MANDATORY,
        itsConfig.getItsAssociationPolicy());
  }

  public void testGetItsAssociationPolicyMandatoryWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    expect(serverConfig.getString("plugin", "ItsTestName", "commentlink"))
        .andReturn("foo")
        .atLeastOnce();
    expect(serverConfig.getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL))
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(
            serverConfig.getEnum(
                "plugin", "ItsTestName", "association", ItsAssociationPolicy.MANDATORY))
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();

    replayMocks();

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.MANDATORY,
        itsConfig.getItsAssociationPolicy());
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

      bind(String.class).annotatedWith(PluginName.class).toInstance("ItsTestName");

      serverConfig = createMock(Config.class);
      bind(Config.class).annotatedWith(GerritServerConfig.class).toInstance(serverConfig);
    }
  }
}
