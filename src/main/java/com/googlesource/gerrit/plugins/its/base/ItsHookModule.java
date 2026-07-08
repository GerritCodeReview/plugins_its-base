// Copyright (C) 2017 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.its.base;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.its.ItsHookEnabledConfigEntry;
import com.googlesource.gerrit.plugins.its.base.validation.ItsValidateComment;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionController;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.AddComment;
import com.googlesource.gerrit.plugins.its.base.workflow.AddPropertyToField;
import com.googlesource.gerrit.plugins.its.base.workflow.AddSoyComment;
import com.googlesource.gerrit.plugins.its.base.workflow.AddStandardComment;
import com.googlesource.gerrit.plugins.its.base.workflow.BoundedOrderedExecutor;
import com.googlesource.gerrit.plugins.its.base.workflow.Condition;
import com.googlesource.gerrit.plugins.its.base.workflow.CreateVersionFromProperty;
import com.googlesource.gerrit.plugins.its.base.workflow.CustomAction;
import com.googlesource.gerrit.plugins.its.base.workflow.FireEventOnCommits;
import com.googlesource.gerrit.plugins.its.base.workflow.ItsRulesProjectCacheImpl;
import com.googlesource.gerrit.plugins.its.base.workflow.LifecycleExecutor;
import com.googlesource.gerrit.plugins.its.base.workflow.LogEvent;
import com.googlesource.gerrit.plugins.its.base.workflow.Rule;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.SinceLastTagCommitCollector;
import java.nio.file.Path;

public class ItsHookModule extends FactoryModule {

  /** Rules configuration filename pattern */
  private static final String CONFIG_FILE_NAME = "actions%s.config";

  /** Folder where rules configuration files are located */
  private static final String ITS_FOLDER = "its";

  private static final String TRACKER_SECTION = "tracker";

  private static final String EVALUATION_SECTION = "evaluation";

  private static final String KEY_THREAD_POOL_SIZE = "threadPoolSize";

  private static final int DEFAULT_THREAD_POOL_SIZE = 0;

  private final String pluginName;
  private final PluginConfigFactory pluginCfgFactory;
  private final int evaluationThreadPoolSize;
  private final int trackerThreadPoolSize;

  public ItsHookModule(@PluginName String pluginName, PluginConfigFactory pluginCfgFactory) {
    this.pluginName = pluginName;
    this.pluginCfgFactory = pluginCfgFactory;
    this.evaluationThreadPoolSize = threadPoolSize(EVALUATION_SECTION);
    this.trackerThreadPoolSize = threadPoolSize(TRACKER_SECTION);
  }

  @Override
  protected void configure() {
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named("enabled"))
        .toInstance(new ItsHookEnabledConfigEntry(pluginName, pluginCfgFactory));
    bind(ItsConfig.class);
    DynamicSet.bind(binder(), CommitValidationListener.class).to(ItsValidateComment.class);
    DynamicSet.bind(binder(), EventListener.class).to(ActionController.class);
    checkThreadPoolConfig();
    DynamicSet.bind(binder(), LifecycleListener.class)
        .to(Key.get(LifecycleExecutor.class, Evaluation.class));
    DynamicSet.bind(binder(), LifecycleListener.class)
        .to(Key.get(LifecycleExecutor.class, Tracker.class));
    factory(ActionRequest.Factory.class);
    factory(Condition.Factory.class);
    factory(Rule.Factory.class);
    factory(AddComment.Factory.class);
    factory(AddSoyComment.Factory.class);
    factory(AddStandardComment.Factory.class);
    factory(CreateVersionFromProperty.Factory.class);
    factory(LogEvent.Factory.class);
    factory(AddPropertyToField.Factory.class);
    DynamicMap.mapOf(binder(), CustomAction.class);
    install(ItsRulesProjectCacheImpl.module());
    factory(FireEventOnCommits.Factory.class);
    factory(SinceLastTagCommitCollector.Factory.class);
  }

  @Provides
  @ItsPath
  @Inject
  Path itsPath(SitePaths sitePaths) {
    return sitePaths.etc_dir.normalize().resolve(ITS_FOLDER);
  }

  @Provides
  @GlobalRulesFileName
  String globalRulesFileName() {
    return String.format(CONFIG_FILE_NAME, "");
  }

  @Provides
  @PluginRulesFileName
  String pluginRulesFileName() {
    return String.format(CONFIG_FILE_NAME, "-" + pluginName);
  }

  @Provides
  @TrackerThreadPoolSize
  int trackerThreadPoolSize() {
    return trackerThreadPoolSize;
  }

  @Provides
  @EvaluationThreadPoolSize
  int evaluationThreadPoolSize() {
    return evaluationThreadPoolSize;
  }

  @Provides
  @Singleton
  @Evaluation
  LifecycleExecutor evaluationLifecycleExecutor(
      WorkQueue workQueue, @EvaluationThreadPoolSize int poolSize) {
    return new LifecycleExecutor(workQueue, poolSize, pluginName + "-evaluation");
  }

  @Provides
  @Singleton
  @Tracker
  LifecycleExecutor trackerLifecycleExecutor(
      WorkQueue workQueue, @TrackerThreadPoolSize int poolSize) {
    return new LifecycleExecutor(workQueue, poolSize, pluginName + "-tracker");
  }

  @Provides
  @Singleton
  @Tracker
  BoundedOrderedExecutor trackerExecutor(@Tracker LifecycleExecutor executor) {
    return new BoundedOrderedExecutor(executor);
  }

  @Provides
  @Singleton
  @Evaluation
  BoundedOrderedExecutor evaluationExecutor(@Evaluation LifecycleExecutor executor) {
    return new BoundedOrderedExecutor(executor);
  }

  private int threadPoolSize(String section) {
    return pluginCfgFactory
        .getGlobalPluginConfig(pluginName)
        .getInt(section, KEY_THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
  }

  private void checkThreadPoolConfig() {
    if (evaluationThreadPoolSize > 0 && trackerThreadPoolSize <= 0) {
      addError(
          "evaluation.threadPoolSize (%d) requires tracker.threadPoolSize > 0",
          evaluationThreadPoolSize);
    }
  }
}
