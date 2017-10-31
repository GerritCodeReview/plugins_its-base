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
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.ProjectConfigEntry;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.its.ItsHookEnabledConfigEntry;
import com.googlesource.gerrit.plugins.its.base.validation.ItsValidateComment;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionController;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Condition;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import com.googlesource.gerrit.plugins.its.base.workflow.Rule;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddSoyComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddStandardComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.LogEvent;

public class ItsHookModule extends FactoryModule {

  private final String pluginName;
  private final PluginConfigFactory pluginCfgFactory;

  public ItsHookModule(@PluginName String pluginName, PluginConfigFactory pluginCfgFactory) {
    this.pluginName = pluginName;
    this.pluginCfgFactory = pluginCfgFactory;
  }

  @Override
  protected void configure() {
    bind(ProjectConfigEntry.class)
        .annotatedWith(Exports.named("enabled"))
        .toInstance(new ItsHookEnabledConfigEntry(pluginName, pluginCfgFactory));
    bind(ItsConfig.class);
    DynamicSet.bind(binder(), CommitValidationListener.class).to(ItsValidateComment.class);
    DynamicSet.bind(binder(), EventListener.class).to(ActionController.class);
    factory(ActionRequest.Factory.class);
    factory(Property.Factory.class);
    factory(Condition.Factory.class);
    factory(Rule.Factory.class);
    factory(AddComment.Factory.class);
    factory(AddSoyComment.Factory.class);
    factory(AddStandardComment.Factory.class);
    factory(LogEvent.Factory.class);
  }
}
