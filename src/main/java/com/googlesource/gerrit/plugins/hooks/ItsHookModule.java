// Copyright (C) 2013 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.hooks;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.git.validators.CommitValidationListener;

import com.googlesource.gerrit.plugins.hooks.its.ItsConfig;
import com.googlesource.gerrit.plugins.hooks.its.ItsHookEnabledConfigEntry;
import com.googlesource.gerrit.plugins.hooks.validation.ItsValidateComment;
import com.googlesource.gerrit.plugins.hooks.workflow.ActionController;
import com.googlesource.gerrit.plugins.hooks.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.hooks.workflow.Condition;
import com.googlesource.gerrit.plugins.hooks.workflow.GerritHookFilterAddComment;
import com.googlesource.gerrit.plugins.hooks.workflow.GerritHookFilterAddRelatedLinkToChangeId;
import com.googlesource.gerrit.plugins.hooks.workflow.GerritHookFilterAddRelatedLinkToGitWeb;
import com.googlesource.gerrit.plugins.hooks.workflow.GerritHookFilterChangeState;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;
import com.googlesource.gerrit.plugins.hooks.workflow.Rule;
import com.googlesource.gerrit.plugins.hooks.workflow.action.AddComment;
import com.googlesource.gerrit.plugins.hooks.workflow.action.AddStandardComment;
import com.googlesource.gerrit.plugins.hooks.workflow.action.AddVelocityComment;
import com.googlesource.gerrit.plugins.hooks.workflow.action.LogEvent;

public class ItsHookModule extends FactoryModule {

  private final String pluginName;
  private final PluginConfigFactory pluginCfgFactory;

  public ItsHookModule(@PluginName String pluginName,
      PluginConfigFactory pluginCfgFactory) {
    this.pluginName = pluginName;
    this.pluginCfgFactory = pluginCfgFactory;
  }

  @Override
  protected void configure() {
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named("enabled"))
        .toInstance(new ItsHookEnabledConfigEntry(pluginName, pluginCfgFactory));
    bind(ItsConfig.class);
    DynamicSet.bind(binder(), EventListener.class).to(
        GerritHookFilterAddRelatedLinkToChangeId.class);
    DynamicSet.bind(binder(), EventListener.class).to(
        GerritHookFilterAddComment.class);
    DynamicSet.bind(binder(), EventListener.class).to(
        GerritHookFilterChangeState.class);
    DynamicSet.bind(binder(), EventListener.class).to(
        GerritHookFilterAddRelatedLinkToGitWeb.class);
    DynamicSet.bind(binder(), CommitValidationListener.class).to(
        ItsValidateComment.class);
    DynamicSet.bind(binder(), EventListener.class).to(
        ActionController.class);
    factory(ActionRequest.Factory.class);
    factory(Property.Factory.class);
    factory(Condition.Factory.class);
    factory(Rule.Factory.class);
    factory(AddComment.Factory.class);
    factory(AddStandardComment.Factory.class);
    factory(AddVelocityComment.Factory.class);
    factory(LogEvent.Factory.class);
  }
}
