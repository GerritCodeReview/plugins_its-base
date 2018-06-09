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
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddPropertyToField implements Action {

  private static final Logger log = LoggerFactory.getLogger(AddPropertyToField.class);

  public interface Factory {
    AddPropertyToField create();
  }

  private final ItsFacade its;

  @Inject
  public AddPropertyToField(ItsFacade its) {
    this.its = its;
  }

  @Override
  public void execute(String issue, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    String[] parameters = actionRequest.getParameters();
    if (parameters.length != 2) {
      log.error(
          "Wrong number of received parameters. Received parameters are {}. Exactly two parameters are expected. The first one is the ITS field id, the second one is the event property id",
          Arrays.toString(parameters));
      return;
    }

    String propertyId = parameters[0];
    if (Strings.isNullOrEmpty(propertyId)) {
      log.error("Received property id is blank");
      return;
    }

    String fieldId = parameters[1];
    if (Strings.isNullOrEmpty(fieldId)) {
      log.error("Received field id is blank");
      return;
    }

    if (!properties.containsKey(propertyId)) {
      log.error("No event property found for id {}", propertyId);
      return;
    }

    String propertyValue = properties.get(propertyId);
    its.addValueToField(issue, propertyValue, fieldId);
  }
}