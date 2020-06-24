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

package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.cache.CacheModule;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.googlesource.gerrit.plugins.its.base.GlobalRulesFileName;
import com.googlesource.gerrit.plugins.its.base.PluginRulesFileName;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.jgit.lib.Config;

@Singleton
public class ItsRulesProjectCacheImpl implements ItsRulesProjectCache {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String CACHE_NAME = "its_rules_project";

  private final LoadingCache<String, List<Rule>> cache;

  @Inject
  ItsRulesProjectCacheImpl(@Named(CACHE_NAME) LoadingCache<String, List<Rule>> cache) {
    this.cache = cache;
  }

  @Override
  public List<Rule> get(String projectName) {
    try {
      return cache.get(projectName);
    } catch (ExecutionException e) {
      logger.atWarning().withCause(e).log(
          "Cannot get project specific rules for project %s", projectName);
      return ImmutableList.of();
    }
  }

  @Override
  public void evict(String projectName) {
    cache.invalidate(projectName);
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CACHE_NAME, String.class, new TypeLiteral<List<Rule>>() {}).loader(Loader.class);

        bind(ItsRulesProjectCacheImpl.class);
        bind(ItsRulesProjectCache.class).to(ItsRulesProjectCacheImpl.class);
        DynamicSet.bind(binder(), GitReferenceUpdatedListener.class)
            .to(ItsRulesProjectCacheRefresher.class);
      }
    };
  }

  static class Loader extends CacheLoader<String, List<Rule>> {
    private final String globalRulesFileName;
    private final String pluginRulesFileName;
    private final ProjectCache projectCache;
    private final RulesConfigReader rulesConfigReader;

    @Inject
    Loader(
        @GlobalRulesFileName String globalRulesFileName,
        @PluginRulesFileName String pluginRulesFileName,
        ProjectCache projectCache,
        RulesConfigReader rulesConfigReader) {
      this.globalRulesFileName = globalRulesFileName;
      this.pluginRulesFileName = pluginRulesFileName;
      this.projectCache = projectCache;
      this.rulesConfigReader = rulesConfigReader;
    }

    @Override
    public List<Rule> load(String projectName) throws IOException {
      ProjectState project =
          projectCache
              .get(Project.nameKey(projectName))
              .orElseThrow(() -> new IOException("Can't load " + projectName));
      List<Rule> projectRules = readRulesFrom(project);
      if (projectRules.isEmpty()) {
        for (ProjectState parent : project.parents()) {
          projectRules = readRulesFrom(parent);
          if (!projectRules.isEmpty()) {
            break;
          }
        }
      }
      return projectRules;
    }

    private List<Rule> readRulesFrom(ProjectState project) {
      Config general = project.getConfig(globalRulesFileName).get();
      Config pluginSpecific = project.getConfig(pluginRulesFileName).get();
      return new ImmutableList.Builder<Rule>()
          .addAll(rulesConfigReader.getRulesFromConfig(general))
          .addAll(rulesConfigReader.getRulesFromConfig(pluginSpecific))
          .build();
    }
  }
}
