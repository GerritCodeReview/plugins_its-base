package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.inject.Inject;

public class ItsRulesProjectCacheRefresher implements GitReferenceUpdatedListener {

  private final ItsRulesProjectCache itsRuleProjectCache;

  @Inject
  ItsRulesProjectCacheRefresher(ItsRulesProjectCache itsRuleProjectCache) {
    this.itsRuleProjectCache = itsRuleProjectCache;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    if (event.getRefName().startsWith(RefNames.META_SUFFIX)) {
      itsRuleProjectCache.evict(event.getProjectName());
    }
  }
}
