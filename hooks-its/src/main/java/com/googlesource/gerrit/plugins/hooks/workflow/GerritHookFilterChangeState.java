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

package com.googlesource.gerrit.plugins.hooks.workflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.server.config.SitePath;
import com.google.gerrit.server.events.ApprovalAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.InvalidTransitionException;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;

public class GerritHookFilterChangeState extends GerritHookFilter {
  private static final Logger log = LoggerFactory
      .getLogger(GerritHookFilterChangeState.class);

  @Inject
  private ItsFacade its;

  @Inject
  @SitePath
  private File sitePath;

  @Override
  public void doFilter(PatchSetCreatedEvent hook) throws IOException {
    performAction(hook.change, new Condition("change", "created"));
  }

  @Override
  public void doFilter(CommentAddedEvent hook) throws IOException {
    try {
      List<Condition> conditions = new ArrayList<Condition>();
      conditions.add(new Condition("change", "commented"));

      for (ApprovalAttribute approval : hook.approvals) {
        addApprovalCategoryCondition(conditions, approval.type, approval.value);
      };

      performAction(hook.change,
          conditions.toArray(new Condition[conditions.size()]));
    } catch (InvalidTransitionException ex) {
      log.warn(ex.getMessage());
    }
  }

  @Override
  public void doFilter(ChangeMergedEvent hook) throws IOException {
    performAction(hook.change, new Condition("change", "merged"));
  }

  @Override
  public void doFilter(ChangeAbandonedEvent hook) throws IOException {
    performAction(hook.change, new Condition("change", "abandoned"));
  }

  @Override
  public void doFilter(ChangeRestoredEvent hook) throws IOException {
    performAction(hook.change, new Condition("change", "restored"));
  }

  private void addApprovalCategoryCondition(List<Condition> conditions,
      String name, String value) {
    value = toConditionValue(value);
    if (value == null) return;

    conditions.add(new Condition(name, value));
  }

  private String toConditionValue(String text) {
    if (text == null) return null;

    try {
      int val = Integer.parseInt(text);
      if (val > 0)
        return "+" + val;
      else
        return text;
    } catch (Exception any) {
      return null;
    }
  }

  private void performAction(ChangeAttribute change, Condition... conditionArgs)
      throws IOException {

    List<Condition> conditions = Arrays.asList(conditionArgs);

    log.debug("Checking suitable transition for: " + conditions);

    Transition transition = null;
    List<Transition> transitions = loadTransitions();
    for (Transition tx : transitions) {

      log.debug("Checking transition: " + tx);
      if (tx.matches(conditions)) {
        log.debug("Transition FOUND > " + tx.getAction());
        transition = tx;
        break;
      }
    }

    if (transition == null) {
      log.debug("Nothing to perform, transition not found for conditions "
          + conditions);
      return;
    }

    String gitComment = change.subject;
    String[] issues = getIssueIds(gitComment);

    for (String issue : issues) {
      its.performAction(issue, transition.getAction());
    }
  }

  private List<Transition> loadTransitions() {
    File configFile = new File(sitePath, "etc/issue-state-transition.config");
    FileBasedConfig cfg = new FileBasedConfig(configFile, FS.DETECTED);
    try {
      cfg.load();
    } catch (IOException e) {
      log.error("Cannot load transitions configuration file " + cfg, e);
      return Collections.emptyList();
    } catch (ConfigInvalidException e) {
      log.error("Invalid transitions configuration file" + cfg, e);
      return Collections.emptyList();
    }

    List<Transition> transitions = new ArrayList<Transition>();
    Set<String> sections = cfg.getSubsections("action");
    for (String section : sections) {
      List<Condition> conditions = new ArrayList<Condition>();
      Set<String> keys = cfg.getNames("action", section);
      for (String key : keys) {
        String val = cfg.getString("action", section, key);
        conditions.add(new Condition(key.trim(), val.trim().split(",")));
      }
      transitions.add(new Transition(toAction(section), conditions));
    }
    return transitions;
  }

  private String toAction(String name) {
    name = name.trim();
    try {
      int i = name.lastIndexOf(' ');
      Integer.parseInt(name.substring(i + 1));
      name = name.substring(0, i);
    } catch (Exception ignore) {
    }
    return name;
  }

  public class Condition {
    private String key;
    private String[] val;

    public Condition(String key, String[] values) {
      super();
      this.key = key;
      this.val = values;
    }

    public Condition(String key, String value) {
      this(key, new String[] {value});
    }

    public String getKey() {
      return key;
    }

    public String[] getVal() {
      return val;
    }

    @Override
    public boolean equals(Object o) {
      try {
        Condition other = (Condition) o;
        if (!(key.equals(other.key))) return false;

        boolean valMatch = false;
        List<String> otherVals = Arrays.asList(other.val);
        for (String value : val) {
          if (otherVals.contains(value)) valMatch = true;
        }

        return valMatch;
      } catch (Exception any) {
        return false;
      }
    }

    @Override
    public String toString() {
      return key + "=" + Arrays.asList(val);
    }
  }

  public class Transition {
    private String action;
    private List<Condition> conditions;

    public Transition(String action, List<Condition> conditions) {
      super();
      this.action = action;
      this.conditions = conditions;
    }

    public String getAction() {
      return action;
    }

    public List<Condition> getCondition() {
      return conditions;
    }

    public boolean matches(List<Condition> eventConditions) {

      for (Condition condition : conditions) {
        if (!eventConditions.contains(condition)) return false;
      }

      return true;
    }

    @Override
    public String toString() {
      return "action=\"" + action + "\", conditions=" + conditions;
    }
  }
}
