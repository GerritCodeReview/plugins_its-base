package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.inject.Inject;

public class ItsRulesProjectCacheRefresher implements GitReferenceUpdatedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

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
      logger.atWarning().withCause(e).log("Unable to evict ITS rules cache");
    }
  }
}
