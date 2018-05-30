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

package com.googlesource.gerrit.plugins.its.base.workflow.action;

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

/**
 * Created on 30/05/18.
 *
 * @author Reda.Housni-Alaoui
 */
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
  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    String[] parameters = actionRequest.getParameters();
    if (parameters.length != 2) {
      log.error(
          "Wrong number of received parameters. Received parameters are {}. Exactly two parameters are expected. The first one is the ITS field id, the second one is the event property id",
          Arrays.toString(parameters));
      return;
    }

    String propertyId = parameters[0];
    if (StringUtils.isBlank(propertyId)) {
      log.error("Received property id is blank");
      return;
    }

    String fieldId = parameters[1];
    if (StringUtils.isBlank(fieldId)) {
      log.error("Received field id is blank");
      return;
    }

    Property property =
            properties
                    .stream()
                    .filter(prop -> propertyId.equals(prop.getKey()))
                    .findFirst()
                    .orElse(null);
    if (property == null) {
      log.error("No event property found for id {}", propertyId);
      return;
    }

    String propertyValue = property.getValue();
    its.addValueToField(issue, propertyValue, fieldId);
  }
}
