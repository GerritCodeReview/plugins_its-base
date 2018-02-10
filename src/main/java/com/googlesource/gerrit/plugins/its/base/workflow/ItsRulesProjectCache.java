package com.googlesource.gerrit.plugins.its.base.workflow;

import java.util.List;

/** Cache of project defined ITS rules */
public interface ItsRulesProjectCache {

  /**
   * Get the cached ITS rules for a project
   *
   * @param projectName name of the project.
   * @return the cached rules; an empty list if no such project exists or projectName is null.
   */
  List<Rule> get(String projectName);

  /**
   * Invalidate the cached rules for the given project.
   *
   * @param projectName project for which the rules are being evicted
   */
  void evict(String projectName);
}
