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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

/** Collects all commits between the last tag and HEAD */
public class SinceLastTagCommitCollector implements CommitCollector {
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
    String revision = properties.get("revision");

    try (Repository repo = repoManager.openRepository(Project.nameKey(projectName))) {
      return collect(repo, revision);
    }
  }

  private List<String> collect(Repository repo, String currentRevision) throws IOException {
    try (RevWalk revWalk = new RevWalk(repo)) {
      RevCommit currentCommit = revWalk.parseCommit(repo.resolve(currentRevision));
      revWalk.markStart(currentCommit);
      List<String> commitIds = new ArrayList<>();
      for (RevCommit commit : revWalk) {
        if (!currentCommit.getId().equals(commit.getId()) && isTagged(repo, commit)) {
          break;
        }
        commitIds.add(ObjectId.toString(commit.getId()));
      }
      return commitIds;
    }
  }

  /** @return True if {@code commit} is tagged */
  private boolean isTagged(Repository repo, RevCommit commit) throws IOException {
    ObjectId commitId = commit.getId();
    try (RevWalk revWalk = new RevWalk(repo)) {
      return repo.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).stream()
          .map(Ref::getObjectId)
          .map(
              refObjectId -> {
                try {
                  return revWalk.parseTag(refObjectId);
                } catch (IncorrectObjectTypeException e) {
                  return null;
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              })
          .filter(Objects::nonNull)
          .map(RevTag::getObject)
          .map(RevObject::getId)
          .anyMatch(commitId::equals);
    }
  }
}
