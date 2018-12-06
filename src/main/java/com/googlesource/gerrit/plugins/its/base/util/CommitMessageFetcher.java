package com.googlesource.gerrit.plugins.its.base.util;

import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitMessageFetcher {
  private static final Logger log = LoggerFactory.getLogger(CommitMessageFetcher.class);

  private final GitRepositoryManager repoManager;

  @Inject
  CommitMessageFetcher(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  public String fetch(String projectName, String commitId) throws IOException {
    try (Repository repo = repoManager.openRepository(new NameKey(projectName))) {
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
        return commit.getFullMessage();
      }
    }
  }

  public String fetchGuarded(String projectName, String commitId) {
    String ret = "";
    try {
      if (!commitId.equals(ObjectId.zeroId().name())) {
        ret = fetch(projectName, commitId);
      }
    } catch (IOException e) {
      log.error(
          "Could not fetch commit message for commit " + commitId + " of project " + projectName,
          e);
    }
    return ret;
  }
}
