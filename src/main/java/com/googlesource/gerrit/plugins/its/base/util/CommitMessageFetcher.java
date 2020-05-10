package com.googlesource.gerrit.plugins.its.base.util;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class CommitMessageFetcher {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager repoManager;

  @Inject
  CommitMessageFetcher(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  public String fetch(String projectName, String commitId) throws IOException {
    try (Repository repo = repoManager.openRepository(Project.nameKey(projectName))) {
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
        return commit.getFullMessage();
      }
    }
  }

  public String fetchGuarded(String projectName, String commitId) {
    String ret = "";
    try {
      ret = fetch(projectName, commitId);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Could not fetch commit message for commit %s of project %s", commitId, projectName);
    }
    return ret;
  }
}
