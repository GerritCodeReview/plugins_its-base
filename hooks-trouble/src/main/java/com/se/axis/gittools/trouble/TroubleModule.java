/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gwtorm.server.SchemaFactory;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.ItsHookModule;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;


/**
 * Plugin module used for registering the TroubleItsFacade.
 */
public class TroubleModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(TroubleModule.class);

  private final String pluginName;
  private final SchemaFactory<ReviewDb> reviewDbProvider;
  private final PluginConfig config;
  private final GitRepositoryManager repoManager;
  private final WorkQueue workQueue;
  private final ProjectCache projectCache;

  /**
   * Injected constructor.
   */
  @Inject
  public TroubleModule(@PluginName final String pluginName, final PluginConfigFactory configFactory, final WorkQueue workQueue,
      final SchemaFactory<ReviewDb> schema, final GitRepositoryManager repoManager, final ProjectCache projectCache) {
    this.pluginName = pluginName;
    this.config = configFactory.getFromGerritConfig(pluginName);
    this.reviewDbProvider = schema;
    this.repoManager = repoManager;
    this.workQueue = workQueue;
    this.projectCache = projectCache;
  }

  @Override
  protected final void configure() {
    if (config.getString("troubleUrl") != null) {
      LOG.info("Trouble is configured as ITS");
      TroubleItsFacade trouble = new TroubleItsFacade(pluginName, config, reviewDbProvider, repoManager, projectCache, workQueue);
      bind(ItsFacade.class).toInstance(trouble);
      DynamicSet.bind(binder(), LifecycleListener.class).toInstance(trouble);
      install(new ItsHookModule(pluginName)); // There must be a comment link with this name otherwise nothing will happen
    }
  }
}
