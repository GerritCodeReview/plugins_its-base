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
package com.googlesource.gerrit.plugins.its.base.validation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacadeFactory;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.ReceiveCommand;

public class ItsValidateCommentTest extends LoggingMockingTestCase {
  private Injector injector;
  private IssueExtractor issueExtractor;
  private ItsFacade itsFacade;
  private ItsConfig itsConfig;
  private ItsFacadeFactory itsFacadeFactory;

  private Project.NameKey projectName = Project.nameKey("myProject");
  private Project project = Project.builder(projectName).build();

  public void testOptional() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit();
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.OPTIONAL);

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
  }

  public void testSuggestedNonMatching() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("TestMessage");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(itsConfig.getDummyIssuePattern()).thenReturn(Optional.empty());
    when(issueExtractor.getIssueIds("TestMessage")).thenReturn(new String[] {});

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain 'Missing " + "issue'",
        ret.get(0).getMessage().contains("Missing issue"));

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(itsConfig).getDummyIssuePattern();
    verifyOneOrMore(issueExtractor).getIssueIds("TestMessage");
  }

  public void testMandatoryNonMatching() {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("TestMessage");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(itsConfig.getDummyIssuePattern()).thenReturn(Optional.empty());
    when(issueExtractor.getIssueIds("TestMessage")).thenReturn(new String[] {});

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Missing issue");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(itsConfig).getDummyIssuePattern();
    verifyOneOrMore(issueExtractor).getIssueIds("TestMessage");
  }

  public void testOnlySkipMatching() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("TestMessage SKIP");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(itsConfig.getDummyIssuePattern()).thenReturn(Optional.of(Pattern.compile("SKIP")));
    when(issueExtractor.getIssueIds("TestMessage SKIP")).thenReturn(new String[] {});

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(itsConfig).getDummyIssuePattern();
    verifyOneOrMore(issueExtractor).getIssueIds("TestMessage SKIP");
  }

  public void testSuggestedMatchingSingleExisting() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);
    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(issueExtractor.getIssueIds("bug#4711")).thenReturn(new String[] {"4711"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(true);

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711");
    verifyOneOrMore(itsFacade).exists("4711");
  }

  public void testMandatoryMatchingSingleExisting() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711")).thenReturn(new String[] {"4711"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(true);

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711");
    verifyOneOrMore(itsFacade).exists("4711");
  }

  public void testSuggestedMatchingSingleNonExisting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(issueExtractor.getIssueIds("bug#4711")).thenReturn(new String[] {"4711"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(false);

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Non-existing'",
        ret.get(0).getMessage().contains("Non-existing"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711");
    verifyOneOrMore(itsFacade).exists("4711");
  }

  public void testMandatoryMatchingSingleNonExisting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711")).thenReturn(new String[] {"4711"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(false);

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Non-existing");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711");
    verifyOneOrMore(itsFacade).exists("4711");
  }

  public void testSuggestedMatchingMultiple() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(true);
    when(itsFacade.exists("42")).thenReturn(true);

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testMandatoryMatchingMultiple() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(true);
    when(itsFacade.exists("42")).thenReturn(true);

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testSuggestedMatchingMultipleOneNonExsting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(false);
    when(itsFacade.exists("42")).thenReturn(true);

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Non-existing'",
        ret.get(0).getMessage().contains("Non-existing"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));
    assertFalse(
        "First CommitValidationMessages contains '42', although " + "that bug exists",
        ret.get(0).getMessage().contains("42"));

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testMandatoryMatchingMultipleOneNonExsting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(false);
    when(itsFacade.exists("42")).thenReturn(true);

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Non-existing");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testSuggestedMatchingMultipleSomeNonExsting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(false);
    when(itsFacade.exists("42")).thenReturn(false);

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Non-existing'",
        ret.get(0).getMessage().contains("Non-existing"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));
    assertTrue(
        "First CommitValidationMessages does not contain '42'",
        ret.get(0).getMessage().contains("42"));

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testMandatoryMatchingMultipleSomeNonExsting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    when(itsFacade.exists("4711")).thenReturn(false);
    when(itsFacade.exists("42")).thenReturn(true);

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Non-existing");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testSuggestedMatchingMultipleIOExceptionIsNonExsting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.SUGGESTED);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    doThrow(new IOException("InjectedEx1")).when(itsFacade).exists("4711");
    when(itsFacade.exists("42")).thenReturn(false);

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 2, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Failed to check'",
        ret.get(0).getMessage().contains("Failed to check"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));
    assertTrue(
        "First CommitValidationMessages contains reason of failure",
        ret.get(0).getMessage().contains("InjectedEx1"));
    assertFalse(
        "First CommitValidationMessages contains '42', although " + "that bug exists",
        ret.get(0).getMessage().contains("42"));
    assertTrue(
        "Second CommitValidationMessages does not contain " + "'Non-existing'",
        ret.get(1).getMessage().contains("Non-existing"));

    assertTrue(
        "Second CommitValidationMessages does not contain '42'",
        ret.get(1).getMessage().contains("42"));

    assertLogMessageContains("4711");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testMandatoryMatchingSingleIOException()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711")).thenReturn(new String[] {"4711"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    doThrow(new IOException("InjectedEx1")).when(itsFacade).exists("4711");

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Failed to check'",
        ret.get(0).getMessage().contains("Failed to check"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));
    assertTrue(
        "First CommitValidationMessages contains reason of failure",
        ret.get(0).getMessage().contains("InjectedEx1"));

    assertLogMessageContains("4711");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711");
    verifyOneOrMore(itsFacade).exists("4711");
  }

  public void testMandatoryMatchingMultipleIOExceptionIsNonExisting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    doThrow(new IOException("InjectedEx1")).when(itsFacade).exists("4711");
    when(itsFacade.exists("42")).thenReturn(false);

    try {
      ivc.onCommitReceived(event);
      fail("onCommitReceived did not throw any exception");
    } catch (CommitValidationException e) {
      assertLogMessageContains("Failed to check whether or not issue 4711 exists");
      assertTrue(
          "Message of thrown CommitValidationException does not " + "contain 'Non-existing'",
          e.getMessage().contains("Non-existing"));
    }

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void testMandatoryMatchingMultipleIOExceptionExisting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = mock(ReceiveCommand.class);
    RevCommit commit = createCommit("bug#4711, bug#42");
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    when(itsConfig.getItsAssociationPolicy()).thenReturn(ItsAssociationPolicy.MANDATORY);
    when(issueExtractor.getIssueIds("bug#4711, bug#42")).thenReturn(new String[] {"4711", "42"});
    when(itsFacadeFactory.getFacade(projectName)).thenReturn(itsFacade);
    doThrow(new IOException("InjectedEx1")).when(itsFacade).exists("4711");
    when(itsFacade.exists("42")).thenReturn(true);

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Failed to check'",
        ret.get(0).getMessage().contains("Failed to check"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));
    assertTrue(
        "First CommitValidationMessages contains reason of failure",
        ret.get(0).getMessage().contains("InjectedEx1"));

    assertLogMessageContains("4711");

    verifyOneOrMore(itsConfig).getItsAssociationPolicy();
    verifyOneOrMore(issueExtractor).getIssueIds("bug#4711, bug#42");
    verifyOneOrMore(itsFacade).exists("4711");
    verifyOneOrMore(itsFacade).exists("42");
  }

  public void assertEmptyList(List<CommitValidationMessage> list) {
    if (!list.isEmpty()) {
      StringBuffer sb = new StringBuffer();
      sb.append("Commit Validation List is not emptyList is not empty, but contains:\n");
      for (CommitValidationMessage msg : list) {
        sb.append(msg.getMessage());
        sb.append("\n");
      }
      fail(sb.toString());
    }
  }

  private void setupCommonMocks() {
    when(itsConfig.getIssuePattern()).thenReturn(Pattern.compile("bug#(\\d+)"));
    Project.NameKey projectNK = Project.nameKey("myProject");
    when(itsConfig.isEnabled(projectNK, null)).thenReturn(true);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    injector = Guice.createInjector(new TestModule());

    setupCommonMocks();
  }

  private CommitReceivedEvent newCommitReceivedEvent(
      ReceiveCommand command,
      Project project,
      String refName,
      RevCommit commit,
      IdentifiedUser user) {
    CommitReceivedEvent event = mock(CommitReceivedEvent.class);
    event.command = command;
    event.project = project;
    event.refName = refName;
    event.commit = commit;
    event.user = user;
    when(event.getProjectNameKey()).thenReturn(project.getNameKey());
    when(event.getRefName()).thenReturn(null);
    return event;
  }

  private RevCommit createCommit() {
    return createCommit("Hello world");
  }

  private RevCommit createCommit(String fullMessage) {
    String parents = String.format("parent %040x\n", new java.util.Random().nextLong());
    String commitData =
        String.format(
            "tree %040x\n"
                + parents
                + "author John Doe <john@doe.com> %d +0100\n"
                + "committer John Doe <john@doe.com> %d +0100\n\n"
                + "%s",
            new Random().nextLong(),
            new Date().getTime(),
            new Date().getTime(),
            fullMessage);
    return RevCommit.parse(commitData.getBytes());
  }

  private <T> T verifyOneOrMore(T mock) {
    return verify(mock, atLeastOnce());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      bind(String.class).annotatedWith(PluginName.class).toInstance("ItsTestName");

      issueExtractor = mock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      itsFacade = mock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(itsFacade);

      itsConfig = mock(ItsConfig.class);
      bind(ItsConfig.class).toInstance(itsConfig);

      itsFacadeFactory = mock(ItsFacadeFactory.class);
      bind(ItsFacadeFactory.class).toInstance(itsFacadeFactory);
    }
  }
}
