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

import com.google.common.collect.Lists;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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
    String ref = properties.get("ref");
    String revision = properties.get("revision");

    return doCollect(projectName, ref, revision);
  }

  private List<String> doCollect(String projectName, String currentRef, String currentRevision)
      throws IOException {
    List<String> commits = Lists.newArrayList();

    try (Repository repo = repoManager.openRepository(Project.nameKey(projectName))) {
      List<ObjectId> allTagsRevisions =
          repo.getTags()
              .values()
              .stream()
              .filter(tagRef -> !tagRef.getName().equals(currentRef))
              .map(repo::peel)
              .map(
                  tagRef ->
                      tagRef.getPeeledObjectId() != null
                          ? tagRef.getPeeledObjectId()
                          : tagRef.getObjectId())
              .collect(Collectors.toList());

      try (RevWalk revWalk = new RevWalk(repo)) {
        ObjectId commitId = ObjectId.fromString(currentRevision);

        while (!ObjectId.zeroId().equals(commitId) && !allTagsRevisions.contains(commitId)) {
          RevCommit commit = revWalk.parseCommit(commitId);
          commits.add(ObjectId.toString(commitId));
          if (commit.getParentCount() == 0) {
            break;
          }
          commitId = commit.getParent(0).getId();
        }
      }
    }

    return commits;
  }
}
