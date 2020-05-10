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

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class AddPropertyToField extends IssueAction {
  public interface Factory {
    AddPropertyToField create();
  }

  private final AddPropertyToFieldParametersExtractor parametersExtractor;

  @Inject
  public AddPropertyToField(AddPropertyToFieldParametersExtractor parametersExtractor) {
    this.parametersExtractor = parametersExtractor;
  }

  @Override
  public void execute(
      ItsFacade its, String issue, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    Optional<AddPropertyToFieldParameters> parameters =
        parametersExtractor.extract(actionRequest, properties);
    if (!parameters.isPresent()) {
      return;
    }
    its.addValueToField(issue, parameters.get().getPropertyValue(), parameters.get().getFieldId());
  }
}
