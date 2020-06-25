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

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

class CreateVersionFromPropertyParametersExtractor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject
  public CreateVersionFromPropertyParametersExtractor() {}

  public Optional<CreateVersionFromPropertyParameters> extract(
      ActionRequest actionRequest, Map<String, String> properties) {
    String[] parameters = actionRequest.getParameters();
    if (parameters.length != 1) {
      logger.atSevere().log(
          "Wrong number of received parameters. Received parameters are %s. Only one parameter is expected, the property id.",
          Arrays.toString(parameters));
      return Optional.empty();
    }

    String propertyId = parameters[0];
    if (Strings.isNullOrEmpty(propertyId)) {
      logger.atSevere().log("Received property id is blank");
      return Optional.empty();
    }

    if (!properties.containsKey(propertyId)) {
      logger.atSevere().log("No event property found for id %s", propertyId);
      return Optional.empty();
    }

    String propertyValue = properties.get(propertyId);
    return Optional.of(new CreateVersionFromPropertyParameters(propertyValue));
  }
}
