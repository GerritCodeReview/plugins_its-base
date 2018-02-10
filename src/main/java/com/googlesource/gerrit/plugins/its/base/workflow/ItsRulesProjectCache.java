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
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItsRulesProjectCache implements GitReferenceUpdatedListener {
  private static final Logger log = LoggerFactory.getLogger(ItsRulesProjectCache.class);
  private static final String CACHE_NAME = "itsRulesProjectCache";

  private final LoadingCache<Project.NameKey, List<Rule>> cache;

  @Inject
  ItsRulesProjectCache(@Named(CACHE_NAME) LoadingCache<Project.NameKey, List<Rule>> cache) {
    this.cache = cache;
  }

  List<Rule> get(Project.NameKey projectName) {
    try {
      return cache.get(projectName);
    } catch (ExecutionException e) {
      log.warn("Cannot get project specific rules for project {}", projectName, e);
      return ImmutableList.of();
    }
  }

  void evict(Project.NameKey projectName) {
    cache.invalidate(projectName);
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    if (event.getRefName().startsWith(RefNames.REFS_CHANGES)) {
      evict(new Project.NameKey(event.getProjectName()));
    }
  }

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CACHE_NAME, Project.NameKey.class, new TypeLiteral<List<Rule>>() {})
            .loader(Loader.class);

        DynamicSet.bind(binder(), GitReferenceUpdatedListener.class).to(ItsRulesProjectCache.class);
      }
    };
  }

  static class Loader extends CacheLoader<Project.NameKey, List<Rule>> {
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
    public List<Rule> load(Project.NameKey projectName) throws IOException {
      List<Rule> projectRules = new ArrayList<>();
      ProjectState project = projectCache.checkedGet(projectName);
      projectRules = readRulesFrom(project);
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
