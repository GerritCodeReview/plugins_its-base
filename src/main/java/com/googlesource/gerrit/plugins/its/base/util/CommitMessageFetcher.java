package com.googlesource.gerrit.plugins.its.base.util;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;

public class CommitMessageFetcher {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager repoManager;

  @Inject
  CommitMessageFetcher(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  public String fetch(String projectName, String objectId) throws IOException {
    try (Repository repo = repoManager.openRepository(Project.nameKey(projectName))) {
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevObject obj = revWalk.peel(revWalk.parseAny(ObjectId.fromString(objectId)));
        if (obj instanceof RevCommit) {
          return ((RevCommit) obj).getFullMessage();
        }
        // objectId was found, but it's not a commit.
        // Since the objectId was found, it's nothing to worry about and we do not need to alert the
        // user. We silently return the empty string as blobs, trees, ... do not have a proper
        // commit message.
        //
        // Parsing a non-commit objectId (and reaching this point) will happen for example on NoteDB
        // sites when Gerrit updates `refs/sequences/changes` (which does not point at a commit, but
        // a blob) on All-Projects and the corresponding RefUpdatedEvent gets processed.
        return "";
      }
    }
  }

  public String fetchGuarded(String projectName, String objectId) {
    String ret = "";
    try {
      ret = fetch(projectName, objectId);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Could not fetch commit message for commit %s of project %s", objectId, projectName);
    }
    return ret;
  }
}
