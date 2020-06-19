// Copyright (C) 2020 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.its.base.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class CommitMessageFetcherTest extends LoggingMockingTestCase {
  private GitRepositoryManager repoManager;
  private Repository repo;
  private String objectIdBlob = "24c5735c3e8ce8fd18d312e9e58149a62236c01a";
  private String objectIdTree = "3faaefce19558dfc8d9c976f09ae4897f45cb242";
  private String objectIdCommit = "95aed53c03b6d3df0912bdd9bb1d0c6eaf619f58";
  private String objectIdMissing = "0123456789012345678901234567890123456789";

  private byte rawBlob[] = "def\n".getBytes();
  private byte rawTree[] = sha1append("100644 abc\000", objectIdBlob);
  private byte rawCommit[] =
      ("tree\0003faaefce19558dfc8d9c976f09ae4897f45cb242\n"
              + "author Author <author@example.org> 1592579853 +0200\n"
              + "committer Committer <committer@example.org> 1592579853 +0200\n"
              + "\n"
              + "CommitMsg\n")
          .getBytes();

  private static byte[] sha1append(String left, String sha1sum) {
    int leftLen = left.length();
    byte[] right = (new BigInteger(sha1sum, 16)).toByteArray();
    byte[] ret = new byte[leftLen + right.length];
    System.arraycopy(left.getBytes(), 0, ret, 0, leftLen);
    System.arraycopy(right, 0, ret, leftLen, right.length);
    return ret;
  }

  @Test
  public void testFetchBlob() throws IOException {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetch("ProjectFoo", objectIdBlob);

    assertThat(commitMessage).isEmpty();
  }

  @Test
  public void testFetchTree() throws IOException {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetch("ProjectFoo", objectIdTree);

    assertThat(commitMessage).isEmpty();
  }

  @Test
  public void testFetchCommit() throws IOException {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetch("ProjectFoo", objectIdCommit);

    assertThat(commitMessage).isEqualTo("CommitMsg\n");
  }

  @Test
  public void testFetchGuardedBlob() {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetchGuarded("ProjectFoo", objectIdBlob);

    assertThat(commitMessage).isEmpty();
  }

  @Test
  public void testFetchGuardedTree() {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetchGuarded("ProjectFoo", objectIdTree);

    assertThat(commitMessage).isEmpty();
  }

  @Test
  public void testFetchGuardedCommit() {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetchGuarded("ProjectFoo", objectIdCommit);

    assertThat(commitMessage).isEqualTo("CommitMsg\n");
  }

  @Test
  public void testFetchGuardedMissing() {
    CommitMessageFetcher fetcher = createCommitMessageFetcher();
    String commitMessage = fetcher.fetchGuarded("ProjectFoo", objectIdMissing);

    assertThat(commitMessage).isEmpty();

    assertLogMessageContains(objectIdMissing);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    ObjectLoader objectLoaderBlob = mock(ObjectLoader.class);
    when(objectLoaderBlob.getCachedBytes(anyInt())).thenReturn(rawBlob);
    when(objectLoaderBlob.getType()).thenReturn(Constants.OBJ_BLOB);

    ObjectLoader objectLoaderTree = mock(ObjectLoader.class);
    when(objectLoaderTree.getCachedBytes(anyInt())).thenReturn(rawTree);
    when(objectLoaderTree.getType()).thenReturn(Constants.OBJ_TREE);

    ObjectLoader objectLoaderCommit = mock(ObjectLoader.class);
    when(objectLoaderCommit.getCachedBytes(anyInt())).thenReturn(rawCommit);
    when(objectLoaderCommit.getType()).thenReturn(Constants.OBJ_COMMIT);

    Set<ObjectId> shallowCommits = new HashSet<>();
    shallowCommits.add(ObjectId.fromString(objectIdCommit));

    ObjectReader objectReader = mock(ObjectReader.class);
    when(objectReader.getShallowCommits()).thenReturn(shallowCommits);
    when(objectReader.open(ObjectId.fromString(objectIdBlob))).thenReturn(objectLoaderBlob);
    when(objectReader.open(ObjectId.fromString(objectIdTree))).thenReturn(objectLoaderTree);
    when(objectReader.open(ObjectId.fromString(objectIdCommit))).thenReturn(objectLoaderCommit);
    when(objectReader.open(ObjectId.fromString(objectIdMissing)))
        .thenThrow(
            new MissingObjectException(ObjectId.fromString(objectIdMissing), Constants.OBJ_COMMIT));

    repo = mock(Repository.class);
    when(repo.newObjectReader()).thenReturn(objectReader);

    repoManager = mock(GitRepositoryManager.class);
    when(repoManager.openRepository(eq(new Project.NameKey("ProjectFoo")))).thenReturn(repo);
  }

  private CommitMessageFetcher createCommitMessageFetcher() {
    return new CommitMessageFetcher(repoManager);
  }
}
