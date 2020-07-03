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

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Suppliers;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.validation.ItsAssociationPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

public class ItsConfigTest extends LoggingMockingTestCase {
  private Injector injector;

  private ProjectCache projectCache;
  private PluginConfigFactory pluginConfigFactory;
  private Config serverConfig;

  @Retention(RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface GerritInstanceIdTest {
    public String value();
  }

  public void setupIsEnabled(
      String enabled, String itsProject, String parentEnabled, String[] branches) {
    ProjectState projectState = mock(ProjectState.class);

    when(projectCache.get(Project.nameKey("testProject"))).thenReturn(Optional.of(projectState));
    when(projectCache.get(Project.nameKey("parentProject"))).thenReturn(Optional.of(projectState));

    Iterable<ProjectState> parents;
    if (parentEnabled == null) {
      parents = Arrays.asList(projectState);
    } else {
      ProjectState parentProjectState = mock(ProjectState.class);

      PluginConfig parentPluginConfig = mock(PluginConfig.class);

      when(pluginConfigFactory.getFromProjectConfig(parentProjectState, "ItsTestName"))
          .thenReturn(parentPluginConfig);

      when(parentPluginConfig.getString("enabled", "false")).thenReturn(parentEnabled);

      PluginConfig parentPluginConfigWI = mock(PluginConfig.class);

      when(pluginConfigFactory.getFromProjectConfigWithInheritance(
              parentProjectState, "ItsTestName"))
          .thenReturn(parentPluginConfigWI);

      String[] parentBranches = {"refs/heads/testBranch"};
      when(parentPluginConfigWI.getStringList("branch")).thenReturn(parentBranches);

      parents = Arrays.asList(parentProjectState, projectState);
    }
    when(projectState.treeInOrder()).thenReturn(parents);

    PluginConfig pluginConfig = mock(PluginConfig.class);

    when(pluginConfigFactory.getFromProjectConfig(projectState, "ItsTestName"))
        .thenReturn(pluginConfig);

    when(pluginConfig.getString("enabled", "false")).thenReturn(enabled);
    when(pluginConfig.getString(eq("its-project"))).thenReturn(itsProject);

    PluginConfig pluginConfigWI = mock(PluginConfig.class);

    when(pluginConfigFactory.getFromProjectConfigWithInheritance(projectState, "ItsTestName"))
        .thenReturn(pluginConfigWI);

    when(pluginConfigWI.getString("enabled", "false")).thenReturn(enabled);

    when(pluginConfigWI.getStringList("branch")).thenReturn(branches);
  }

  public void testIsEnabledRefNoParentNoBranchEnabled() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNoBranchDisabled() {
    String[] branches = {};
    setupIsEnabled("false", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentNoBranchEnforced() {
    String[] branches = {};
    setupIsEnabled("enforced", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchEnabled() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("true", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentMatchingBranchDisabled() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("false", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchEnforced() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("enforced", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNonMatchingBranchEnabled() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("true", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNonMatchingBranchDisabled() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("false", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentNonMatchingBranchEnforced() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("enforced", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleEnabled() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("true", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefNoParentMatchingBranchMiddleDisabled() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("false", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefNoParentMatchingBranchMiddleEnforced() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*", "^refs/heads/baz.*"};
    setupIsEnabled("enforced", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefParentNoBranchEnabled() {
    String[] branches = {};
    setupIsEnabled("false", null, "true", branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void BROKEN_testIsEnabledRefParentNoBranchDisabled() {
    String[] branches = {};
    setupIsEnabled("false", null, "false", branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertFalse(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  public void testIsEnabledRefParentNoBranchEnforced() {
    String[] branches = {};
    setupIsEnabled("false", null, "enforced", branches);

    ItsConfig itsConfig = createItsConfig();

    Project.NameKey projectNK = Project.nameKey("testProject");
    assertTrue(itsConfig.isEnabled(projectNK, "refs/heads/testBranch"));
  }

  @GerritInstanceIdTest(value = "local-instance-id")
  public void testIsNotEnabledEventFromForeignInstanceId() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));
    event.instanceId = "foreign-instance-id";

    ItsConfig itsConfig = createItsConfig();

    assertFalse(itsConfig.isEnabled(event));
    assertLogMessageContains("is coming from a remote Gerrit instance-id");
  }

  @GerritInstanceIdTest(value = "local-instance-id")
  public void testIsEnabledEventFromLocalInstanceId() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));
    event.instanceId = "local-instance-id";

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventNoBranches() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchExact() {
    String[] branches = {"refs/heads/testBranch"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventSingleBranchRegExp() {
    String[] branches = {"^refs/heads/test.*"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void BROKEN_testIsEnabledEventSingleBranchNonMatchingRegExp() {
    String[] branches = {"^refs/heads/foo.*"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertFalse(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchExact() {
    String[] branches = {"refs/heads/foo", "refs/heads/testBranch"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchRegExp() {
    String[] branches = {"^refs/heads/foo.*", "^refs/heads/test.*"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchMixedMatchExact() {
    String[] branches = {"refs/heads/testBranch", "refs/heads/foo.*"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledEventMultiBranchMixedMatchRegExp() {
    String[] branches = {"refs/heads/foo", "^refs/heads/test.*"};
    setupIsEnabled("true", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void BROKEN_testIsEnabledEventDisabled() {
    String[] branches = {"^refs/heads/testBranch"};
    setupIsEnabled("false", null, null, branches);

    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertFalse(itsConfig.isEnabled(event));
  }

  public void testIsEnabledCommentAddedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    CommentAddedEvent event = new CommentAddedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledChangeMergedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    ChangeMergedEvent event = new ChangeMergedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledChangeAbandonedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    ChangeAbandonedEvent event = new ChangeAbandonedEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledChangeRestoredEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    ChangeRestoredEvent event = new ChangeRestoredEvent(testChange("testProject", "testBranch"));

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void testIsEnabledRefUpdatedEvent() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    RefUpdatedEvent event = new RefUpdatedEvent();
    RefUpdateAttribute refUpdateAttribute = new RefUpdateAttribute();
    refUpdateAttribute.project = "testProject";
    refUpdateAttribute.refName = "refs/heads/testBranch";
    event.refUpdate = Suppliers.ofInstance(refUpdateAttribute);

    ItsConfig itsConfig = createItsConfig();

    assertTrue(itsConfig.isEnabled(event));
  }

  public void BROKEN_testIsEnabledUnknownEvent() {
    RefEvent event = mock(RefEvent.class);

    ItsConfig itsConfig = createItsConfig();

    assertFalse(itsConfig.isEnabled(event));
    assertLogMessageContains("not recognised and ignored");
  }

  public void testGetItsProjectNull() {
    String[] branches = {};
    setupIsEnabled("true", null, null, branches);

    ItsConfig itsConfig = createItsConfig();

    assertFalse(itsConfig.getItsProjectName(Project.nameKey("testProject")).isPresent());
  }

  public void testGetItsProjectConfigured() {
    String[] branches = {};
    setupIsEnabled("true", "itsProject", null, branches);

    ItsConfig itsConfig = createItsConfig();

    Optional<String> itsProjectName = itsConfig.getItsProjectName(Project.nameKey("testProject"));
    assertTrue(itsProjectName.isPresent());
    assertEquals("itsProject", itsProjectName.get());
  }

  public void testGetIssuePatternNullMatch() {
    ItsConfig itsConfig = createItsConfig();

    assertNull("Pattern for null match is not null", itsConfig.getIssuePattern());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
  }

  public void testGetIssuePatternNullMatchWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("plugin", "ItsTestName", "commentlink")).thenReturn("foo");

    assertNull("Pattern for null match is not null", itsConfig.getIssuePattern());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "foo", "match");
  }

  public void testGetIssuePattern() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match")).thenReturn("TestPattern");

    assertEquals(
        "Expected and generated pattern are not equal",
        "TestPattern",
        itsConfig.getIssuePattern().pattern());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
  }

  public void testGetIssuePatternWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("plugin", "ItsTestName", "commentlink")).thenReturn("foo");
    when(serverConfig.getString("commentlink", "foo", "match")).thenReturn("TestPattern");

    assertEquals(
        "Expected and generated pattern are not equal",
        "TestPattern",
        itsConfig.getIssuePattern().pattern());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "foo", "match");
  }

  public void testGetIssuePatternGroupIndexGroupDefault() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .thenReturn("(foo)(bar)(baz)");
    when(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1)).thenReturn(1);

    assertEquals(
        "Expected and actual group index do not match", 1, itsConfig.getIssuePatternGroupIndex());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
    verifyOneOrMore(serverConfig).getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1);
  }

  public void testGetIssuePatternGroupIndexGroupDefaultGroupless() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match")).thenReturn("foo");
    when(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1)).thenReturn(1);

    assertEquals(
        "Expected and actual group index do not match", 0, itsConfig.getIssuePatternGroupIndex());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
    verifyOneOrMore(serverConfig).getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1);
  }

  public void testGetIssuePatternGroupIndexGroup1() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .thenReturn("(foo)(bar)(baz)");
    when(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1)).thenReturn(1);

    assertEquals(
        "Expected and actual group index do not match", 1, itsConfig.getIssuePatternGroupIndex());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
    verifyOneOrMore(serverConfig).getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1);
  }

  public void testGetIssuePatternGroupIndexGroup3() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .thenReturn("(foo)(bar)(baz)");
    when(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1)).thenReturn(3);

    assertEquals(
        "Expected and actual group index do not match", 3, itsConfig.getIssuePatternGroupIndex());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
    verifyOneOrMore(serverConfig).getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1);
  }

  public void testGetIssuePatternGroupIndexGroupTooHigh() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match"))
        .thenReturn("(foo)(bar)(baz)");
    when(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1)).thenReturn(5);

    assertEquals(
        "Expected and actual group index do not match", 1, itsConfig.getIssuePatternGroupIndex());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
    verifyOneOrMore(serverConfig).getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1);
  }

  public void testGetIssuePatternGroupIndexGroupTooHighGroupless() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("commentlink", "ItsTestName", "match")).thenReturn("foo");
    when(serverConfig.getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1)).thenReturn(5);

    assertEquals(
        "Expected and actual group index do not match", 0, itsConfig.getIssuePatternGroupIndex());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig).getString("commentlink", "ItsTestName", "match");
    verifyOneOrMore(serverConfig).getInt("plugin", "ItsTestName", "commentlinkGroupIndex", 1);
  }

  public void testGetItsAssociationPolicyOptional() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getEnum(
            "commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.OPTIONAL);
    when(serverConfig.getEnum(
            "plugin", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.OPTIONAL);

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.OPTIONAL,
        itsConfig.getItsAssociationPolicy());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig)
        .getEnum("commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL);
    verifyOneOrMore(serverConfig)
        .getEnum("plugin", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL);
  }

  public void testGetItsAssociationPolicyOptionalWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("plugin", "ItsTestName", "commentlink")).thenReturn("foo");
    when(serverConfig.getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.OPTIONAL);
    when(serverConfig.getEnum(
            "plugin", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.OPTIONAL);

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.OPTIONAL,
        itsConfig.getItsAssociationPolicy());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig)
        .getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL);
    verifyOneOrMore(serverConfig)
        .getEnum("plugin", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL);
  }

  public void testGetItsAssociationPolicySuggested() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getEnum(
            "commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(serverConfig.getEnum(
            "plugin", "ItsTestName", "association", ItsAssociationPolicy.SUGGESTED))
        .thenReturn(ItsAssociationPolicy.SUGGESTED);

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.SUGGESTED,
        itsConfig.getItsAssociationPolicy());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig)
        .getEnum("commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL);
    verifyOneOrMore(serverConfig)
        .getEnum("plugin", "ItsTestName", "association", ItsAssociationPolicy.SUGGESTED);
  }

  public void testGetItsAssociationPolicySuggestedWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("plugin", "ItsTestName", "commentlink")).thenReturn("foo");
    when(serverConfig.getEnum(
            "plugin", "ItsTestName", "association", ItsAssociationPolicy.SUGGESTED))
        .thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(serverConfig.getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.SUGGESTED);

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.SUGGESTED,
        itsConfig.getItsAssociationPolicy());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig)
        .getEnum("plugin", "ItsTestName", "association", ItsAssociationPolicy.SUGGESTED);
    verifyOneOrMore(serverConfig)
        .getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL);
  }

  public void testGetItsAssociationPolicyMandatory() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getEnum(
            "commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.MANDATORY);
    when(serverConfig.getEnum(
            "plugin", "ItsTestName", "association", ItsAssociationPolicy.MANDATORY))
        .thenReturn(ItsAssociationPolicy.MANDATORY);

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.MANDATORY,
        itsConfig.getItsAssociationPolicy());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig)
        .getEnum("commentlink", "ItsTestName", "association", ItsAssociationPolicy.OPTIONAL);
    verifyOneOrMore(serverConfig)
        .getEnum("plugin", "ItsTestName", "association", ItsAssociationPolicy.MANDATORY);
  }

  public void testGetItsAssociationPolicyMandatoryWCommentLink() {
    ItsConfig itsConfig = createItsConfig();

    when(serverConfig.getString("plugin", "ItsTestName", "commentlink")).thenReturn("foo");
    when(serverConfig.getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL))
        .thenReturn(ItsAssociationPolicy.MANDATORY);
    when(serverConfig.getEnum(
            "plugin", "ItsTestName", "association", ItsAssociationPolicy.MANDATORY))
        .thenReturn(ItsAssociationPolicy.MANDATORY);

    assertEquals(
        "Expected and generated associated policy do not match",
        ItsAssociationPolicy.MANDATORY,
        itsConfig.getItsAssociationPolicy());

    verifyOneOrMore(serverConfig).getString("plugin", "ItsTestName", "commentlink");
    verifyOneOrMore(serverConfig)
        .getEnum("commentlink", "foo", "association", ItsAssociationPolicy.OPTIONAL);
    verifyOneOrMore(serverConfig)
        .getEnum("plugin", "ItsTestName", "association", ItsAssociationPolicy.MANDATORY);
  }

  private ItsConfig createItsConfig() {
    return injector.getInstance(ItsConfig.class);
  }

  private <T> T verifyOneOrMore(T mock) {
    return verify(mock, atLeastOnce());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    Method testMethod = ItsConfigTest.class.getMethod(getName());
    Optional<String> gerritInstanceIdTest =
        Optional.ofNullable(testMethod.getAnnotation(GerritInstanceIdTest.class))
            .map(GerritInstanceIdTest::value);
    injector = Guice.createInjector(new TestModule(gerritInstanceIdTest));
  }

  private class TestModule extends FactoryModule {
    private final Optional<String> gerritInstanceId;

    public TestModule(Optional<String> gerritInstanceId) {
      this.gerritInstanceId = gerritInstanceId;
    }

    @Override
    protected void configure() {
      projectCache = mock(ProjectCache.class);
      bind(ProjectCache.class).toInstance(projectCache);

      pluginConfigFactory = mock(PluginConfigFactory.class);
      bind(PluginConfigFactory.class).toInstance(pluginConfigFactory);

      bind(String.class).annotatedWith(PluginName.class).toInstance("ItsTestName");

      serverConfig = mock(Config.class);
      bind(Config.class).annotatedWith(GerritServerConfig.class).toInstance(serverConfig);

      bind(String.class)
          .annotatedWith(GerritInstanceId.class)
          .toProvider(Providers.of(gerritInstanceId.orElse(null)));
    }
  }
}
