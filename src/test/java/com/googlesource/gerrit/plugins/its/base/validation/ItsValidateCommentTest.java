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
import static org.easymock.EasyMock.expect;

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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RevCommit.class})
public class ItsValidateCommentTest extends LoggingMockingTestCase {
  private Injector injector;
  private IssueExtractor issueExtractor;
  private ItsFacade itsFacade;
  private ItsConfig itsConfig;
  private ItsFacadeFactory itsFacadeFactory;

  private Project project = new Project(Project.nameKey("myProject"));

  public void testOptional() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.OPTIONAL)
        .atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);
  }

  public void testSuggestedNonMatching() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(itsConfig.getDummyIssuePattern()).andReturn(Optional.empty()).atLeastOnce();
    expect(commit.getFullMessage()).andReturn("TestMessage").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("TestMessage")).andReturn(new String[] {}).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain 'Missing " + "issue'",
        ret.get(0).getMessage().contains("Missing issue"));
  }

  public void testMandatoryNonMatching() {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(itsConfig.getDummyIssuePattern()).andReturn(Optional.empty()).atLeastOnce();
    expect(commit.getFullMessage()).andReturn("TestMessage").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("TestMessage")).andReturn(new String[] {}).atLeastOnce();

    replayMocks();

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Missing issue");
  }

  public void testOnlySkipMatching() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(itsConfig.getDummyIssuePattern())
        .andReturn(Optional.of(Pattern.compile("SKIP")))
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("TestMessage SKIP").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("TestMessage SKIP")).andReturn(new String[] {}).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);
  }

  public void testSuggestedMatchingSingleExisting() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);
    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711")).andReturn(new String[] {"4711"}).atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade);
    expect(itsFacade.exists("4711")).andReturn(true).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);
  }

  public void testMandatoryMatchingSingleExisting() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711")).andReturn(new String[] {"4711"}).atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade);
    expect(itsFacade.exists("4711")).andReturn(true).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);
  }

  public void testSuggestedMatchingSingleNonExisting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711")).andReturn(new String[] {"4711"}).atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade);
    expect(itsFacade.exists("4711")).andReturn(false).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEquals("Size of returned CommitValidationMessages does not match", 1, ret.size());
    assertTrue(
        "First CommitValidationMessages does not contain " + "'Non-existing'",
        ret.get(0).getMessage().contains("Non-existing"));
    assertTrue(
        "First CommitValidationMessages does not contain '4711'",
        ret.get(0).getMessage().contains("4711"));
  }

  public void testMandatoryMatchingSingleNonExisting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711")).andReturn(new String[] {"4711"}).atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade);
    expect(itsFacade.exists("4711")).andReturn(false).atLeastOnce();

    replayMocks();

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Non-existing");
  }

  public void testSuggestedMatchingMultiple() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andReturn(true).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(true).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);
  }

  public void testMandatoryMatchingMultiple() throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andReturn(true).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(true).atLeastOnce();

    replayMocks();

    ret = ivc.onCommitReceived(event);

    assertEmptyList(ret);
  }

  public void testSuggestedMatchingMultipleOneNonExsting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andReturn(false).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(true).atLeastOnce();
    replayMocks();

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
  }

  public void testMandatoryMatchingMultipleOneNonExsting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andReturn(false).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(true).atLeastOnce();

    replayMocks();

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Non-existing");
  }

  public void testSuggestedMatchingMultipleSomeNonExsting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andReturn(false).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(false).atLeastOnce();

    replayMocks();

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
  }

  public void testMandatoryMatchingMultipleSomeNonExsting() throws IOException {
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andReturn(false).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(true).atLeastOnce();

    replayMocks();

    CommitValidationException thrown =
        assertThrows(CommitValidationException.class, () -> ivc.onCommitReceived(event));
    assertThat(thrown).hasMessageThat().contains("Non-existing");
  }

  public void testSuggestedMatchingMultipleIOExceptionIsNonExsting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.SUGGESTED)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andThrow(new IOException("InjectedEx1")).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(false).atLeastOnce();

    replayMocks();

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
  }

  public void testMandatoryMatchingSingleIOException()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711")).andReturn(new String[] {"4711"}).atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andThrow(new IOException("InjectedEx1")).atLeastOnce();

    replayMocks();

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
  }

  public void testMandatoryMatchingMultipleIOExceptionIsNonExisting() throws IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andThrow(new IOException("InjectedEx1")).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(false).atLeastOnce();

    replayMocks();

    try {
      ivc.onCommitReceived(event);
      fail("onCommitReceived did not throw any exception");
    } catch (CommitValidationException e) {
      assertLogMessageContains("Failed to check whether or not issue 4711 exists");
      assertTrue(
          "Message of thrown CommitValidationException does not " + "contain 'Non-existing'",
          e.getMessage().contains("Non-existing"));
    }
  }

  public void testMandatoryMatchingMultipleIOExceptionExisting()
      throws CommitValidationException, IOException {
    List<CommitValidationMessage> ret;
    ItsValidateComment ivc = injector.getInstance(ItsValidateComment.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = newCommitReceivedEvent(command, project, null, commit, null);

    expect(itsConfig.getItsAssociationPolicy())
        .andReturn(ItsAssociationPolicy.MANDATORY)
        .atLeastOnce();
    expect(commit.getFullMessage()).andReturn("bug#4711, bug#42").atLeastOnce();
    expect(commit.getId()).andReturn(commit).anyTimes();
    expect(commit.getName()).andReturn("TestCommit").anyTimes();
    expect(issueExtractor.getIssueIds("bug#4711, bug#42"))
        .andReturn(new String[] {"4711", "42"})
        .atLeastOnce();
    expect(itsFacadeFactory.getFacade(project.getNameKey())).andReturn(itsFacade).anyTimes();
    expect(itsFacade.exists("4711")).andThrow(new IOException("InjectedEx1")).atLeastOnce();
    expect(itsFacade.exists("42")).andReturn(true).atLeastOnce();

    replayMocks();

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
    expect(itsConfig.getIssuePattern()).andReturn(Pattern.compile("bug#(\\d+)")).anyTimes();
    Project.NameKey projectNK = Project.nameKey("myProject");
    expect(itsConfig.isEnabled(projectNK, null)).andReturn(true).anyTimes();
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
    CommitReceivedEvent event = createMock(CommitReceivedEvent.class);
    event.command = command;
    event.project = project;
    event.refName = refName;
    event.commit = commit;
    event.user = user;
    expect(event.getProjectNameKey()).andReturn(project.getNameKey()).anyTimes();
    expect(event.getRefName()).andReturn(null).anyTimes();
    return event;
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      bind(String.class).annotatedWith(PluginName.class).toInstance("ItsTestName");

      issueExtractor = createMock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      itsFacade = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(itsFacade);

      itsConfig = createMock(ItsConfig.class);
      bind(ItsConfig.class).toInstance(itsConfig);

      itsFacadeFactory = createMock(ItsFacadeFactory.class);
      bind(ItsFacadeFactory.class).toInstance(itsFacadeFactory);
    }
  }
}
