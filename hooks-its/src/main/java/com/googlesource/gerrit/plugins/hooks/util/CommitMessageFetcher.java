package com.googlesource.gerrit.plugins.hooks.util;

import java.io.IOException;

import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitMessageFetcher {
  private static final Logger log = LoggerFactory.getLogger(
      CommitMessageFetcher.class);

  private final GitRepositoryManager repoManager;

  @Inject
  CommitMessageFetcher(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  public String fetch(String projectName, String commitId) throws IOException {
    final Repository repo =
        repoManager.openRepository(new NameKey(projectName));
    try {
      RevWalk revWalk = new RevWalk(repo);
      RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));

      return commit.getFullMessage();
    } finally {
      repo.close();
    }
  }

  public String fetchGuarded(String projectName, String commitId) {
    String ret = "";
    try {
      ret = fetch(projectName, commitId);
    } catch (IOException e) {
      log.error("Could not fetch commit message for commit " + commitId +
          " of project " + projectName, e);
    }
    return ret;
  }
}
