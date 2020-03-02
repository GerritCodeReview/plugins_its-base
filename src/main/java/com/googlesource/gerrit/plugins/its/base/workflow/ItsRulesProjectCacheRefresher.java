package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItsRulesProjectCacheRefresher implements GitReferenceUpdatedListener {
  private static final Logger log = LoggerFactory.getLogger(ItsRulesProjectCacheRefresher.class);

  private final GerritApi gApi;
  private final ItsRulesProjectCache itsRuleProjectCache;

  @Inject
  ItsRulesProjectCacheRefresher(GerritApi gApi, ItsRulesProjectCache itsRuleProjectCache) {
    this.gApi = gApi;
    this.itsRuleProjectCache = itsRuleProjectCache;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    if (!event.getRefName().equals(RefNames.REFS_CONFIG)) {
      return;
    }
    String projectName = event.getProjectName();
    itsRuleProjectCache.evict(projectName);
    try {
      for (ProjectInfo childProject : gApi.projects().name(projectName).children()) {
        itsRuleProjectCache.evict(childProject.name);
      }
    } catch (RestApiException e) {
      log.warn("Unable to evict ITS rules cache", e);
    }
  }
}
