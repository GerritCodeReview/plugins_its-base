// Copyright (C) 2018 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.its.base.workflow.commit_collector;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Collects all commits between the last tag and HEAD */
public class SinceLastTagCommitCollector implements CommitCollector {

  private Logger log = LoggerFactory.getLogger(SinceLastTagCommitCollector.class);

  public interface Factory {
    SinceLastTagCommitCollector create();
  }

  private final GitRepositoryManager repoManager;

  @Inject
  public SinceLastTagCommitCollector(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  @Override
  public List<String> collect(Map<String, String> properties) throws IOException {
    String projectName = properties.get("project");
    String ref = properties.get("ref");
    String revision = properties.get("revision");

    return doCollect(projectName, ref, revision);
  }

  private List<String> doCollect(String projectName, String currentRef, String currentRevision)
      throws IOException {
    try (Repository repo = repoManager.openRepository(Project.nameKey(projectName))) {
      ObjectId previousTag = findPreviousTag(repo, currentRef, currentRevision);

      // The commits in range could be scattered across multiple branches (e.g. merge commits).
      // To cope with this, we use the log command which will walk the commit tree in a non-linear
      // fashion.
      LogCommand logCommand = Git.wrap(repo).log();
      if (previousTag != null) {
        log.debug("Using previous tag '{}' as the log command lower bound.", previousTag);
        logCommand = logCommand.not(previousTag);
      } else {
        log.debug("No previous tag found. The log command will be called without any lower bound.");
      }
      ObjectId currentRevisionId = ObjectId.fromString(currentRevision);
      log.debug("Using current revision '{}' as the log command upper bound.", currentRevisionId);
      logCommand = logCommand.add(currentRevisionId);

      Iterable<RevCommit> commits;
      try {
        commits = logCommand.call();
      } catch (GitAPIException e) {
        throw new IOException(e);
      }

      return StreamSupport.stream(commits.spliterator(), false)
          .map(AnyObjectId::name)
          .collect(Collectors.toList());
    }
  }

  /** Lookup for the last tag preceding {@code currentRevision} */
  private ObjectId findPreviousTag(Repository repo, String currentRef, String currentRevision)
      throws IOException {
    RefDatabase refDatabase = repo.getRefDatabase();

    ObjectId currentRefId = fetchObjectId(repo, refDatabase.exactRef(currentRef));

    try (RevWalk revWalk = new RevWalk(repo)) {
      ObjectId visitedCommit = ObjectId.fromString(currentRevision);

      while (true) {
        if (ObjectId.zeroId().equals(visitedCommit)) {
          return null;
        }
        // The current ref may be a tag. It must be ignored.
        if (!currentRefId.equals(visitedCommit) && isTag(repo, visitedCommit)) {
          return visitedCommit;
        }
        RevCommit commit = revWalk.parseCommit(visitedCommit);
        if (commit.getParentCount() == 0) {
          return null;
        }
        // We take the first parent because the lookup is linear. It is sufficient for finding the
        // previous tag.
        visitedCommit = commit.getParent(0).getId();
      }
    }
  }

  /** @return True if {@code commitId} is a tag */
  private boolean isTag(Repository repo, ObjectId commitId) throws IOException {
    return repo.getRefDatabase()
        .getTipsWithSha1(commitId)
        .stream()
        .map(Ref::getName)
        .anyMatch(Constants.R_TAGS::startsWith);
  }

  /** @return The ObjectId of {@code ref} */
  private ObjectId fetchObjectId(Repository repo, Ref ref) {
    RefDatabase refDatabase = repo.getRefDatabase();
    Ref peeledRef;
    try {
      peeledRef = refDatabase.peel(ref);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return peeledRef.getPeeledObjectId() != null
        ? peeledRef.getPeeledObjectId()
        : peeledRef.getObjectId();
  }
}
