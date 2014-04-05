/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.ItsHookModule;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;

/**
 * Plugin module used for registering the TroubleItsFacade.
 */
public class TroubleModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(TroubleModule.class);

  private final SchemaFactory<ReviewDb> reviewDbProvider;
  private final Config gerritConfig;
  private final GitRepositoryManager repoManager;

  /**
   * Injected constructor.
   */
  @Inject
  public TroubleModule(@GerritServerConfig final Config config, final SchemaFactory<ReviewDb> schema,
      final GitRepositoryManager repoManager) {
    this.gerritConfig = config;
    this.reviewDbProvider = schema;
    this.repoManager = repoManager;
  }

  @Override
  protected final void configure() {
    if (gerritConfig.getString(TroubleItsFacade.ITS_NAME_TROUBLE, null, "url") != null) {
      LOG.info("Trouble is configured as ITS");
      bind(ItsFacade.class).toInstance(new TroubleItsFacade(gerritConfig, reviewDbProvider, repoManager));
      install(new ItsHookModule(TroubleItsFacade.ITS_NAME_TROUBLE));
    }
  }
}
