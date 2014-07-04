/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.google.gerrit.server.git.WorkQueue;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool for interfacing with Trouble.
 *
 * - It supports multiple threads.
 * - It supports delayed updates in case Trouble goes down.
 * - Each issue is handled by the same Thread so that there is no race condition
 *   between updates towards the same issue.
 */
public class TroubleWorkQueue {

  /**
   * Default number of worker threads.
   */
  public static final int DEFAULT_POOL_SIZE = 2;

  /**
   * Default retry limit in seconds.
   */
  public static final int DEFAULT_RETRY_LIMIT_SECONDS = 86400; // number of seconds before a task is discarded

  private static final int START_DELAY_MILLIS = 60000;

  private static final Logger LOG = LoggerFactory.getLogger(TroubleWorkQueue.class);

  private final WorkQueue.Executor[] executors;

  private final int retryLimitMillis;

  /**
   * Constructor.
   */
  public TroubleWorkQueue(final String pluginName, final WorkQueue workQueue, final int numThreads, final int retryLimitSeconds) {
    retryLimitMillis = (int) TimeUnit.SECONDS.toMillis(retryLimitSeconds);
    executors = new WorkQueue.Executor[numThreads];

    // create the executors
    for (int i = 0; i < numThreads; i++) {
      executors[i] = workQueue.createQueue(1, pluginName + "-" + i + "-");
    }
  }

  /**
   * Stops all executors in an orderly fashion.
   */
  public final void shutdown() {
    for (WorkQueue.Executor executor : executors) {
      executor.shutdownNow();
    }
  }

  /**
   * Submit a callable to the work queue associated with the given ticket.
   */
  public final void submit(final Integer ticket, final Callable<Void> task) {
    Runnable wrapper = new Runnable() {
      private int delayMillis = START_DELAY_MILLIS;
      private long deadline = System.currentTimeMillis() + retryLimitMillis;
      public final void run() {
        try {
          LOG.debug("starting task: {}", task);
          try {
            task.call();
            LOG.debug("task completed successfully: {}", task);
          } catch (TroubleClient.HttpException he) {
            throw he; // unrecoverable
          } catch (RepositoryNotFoundException rnfe) {
            throw rnfe; // unrecoverable
          } catch (IOException ioe) {
            if ((System.currentTimeMillis() + delayMillis) >= deadline) {
              throw ioe; // not enough time
            }
            LOG.info("task failed: " + task, ioe);
            LOG.info("retrying in {} millis ...", delayMillis);
            getExecutor(ticket).schedule(this, delayMillis, TimeUnit.MILLISECONDS);
            delayMillis += START_DELAY_MILLIS; // wait more next time
          }
        } catch (Exception e) {
          LOG.error("task failed: " + task, e);
        }
      }
    };
    getExecutor(ticket).submit(wrapper);
  }

  /**
   * Selects an executor to execute work for a given ticket.
   *
   * Its important that the SAME executor is used to do all the work
   * for the same ticket.
   */
  private WorkQueue.Executor getExecutor(final Integer ticket) {
    return executors[ticket % executors.length];
  }
}
