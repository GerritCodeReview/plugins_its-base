/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private static final int START_DELAY_MILLIS = 8000;

  private static final Logger LOG = LoggerFactory.getLogger(TroubleWorkQueue.class);

  private final ExecutorService[] executors;

  private final int retryLimitMillis;

  /**
   * Constructor.
   */
  public TroubleWorkQueue(final int numThreads, final int retryLimitSeconds) {
    retryLimitMillis = (int) TimeUnit.SECONDS.toMillis(retryLimitSeconds);
    executors = new ExecutorService[numThreads];

    // create the executors
    for (int i = 0; i < numThreads; i++) {
      executors[i] = Executors.newSingleThreadExecutor();
    }
  }

  /**
   * Stops all executors in an orderly fashion.
   */
  public final void shutdown() {
    for (ExecutorService executor : executors) {
      executor.shutdownNow();
    }
  }

  /**
   * Submit a callable to the work queue associated with the given ticket.
   */
  public final void submit(final Integer ticket, final Callable<Void> task) {
    Callable<Void> wrapper = new Callable<Void>() {
      private int delayMillis = START_DELAY_MILLIS;
      private long deadline = System.currentTimeMillis() + retryLimitMillis;
      public final Void call() throws Exception {
        try {
          while (System.currentTimeMillis() < deadline) {
            LOG.debug("starting task: {}", task);
            try {
              task.call();
              break;
            } catch (TroubleClient.HttpException e1) {
              throw e1; // re-throw unrecoverable error
            } catch (IOException e2) {
              LOG.info("task ({}) failed: {}", task, e2);
              LOG.info("retrying in {} millis ...", delayMillis);
              Thread.sleep(delayMillis);
              delayMillis *= 2; // wait more next time
            }
          }
          LOG.debug("task completed successfully: {}", task);
        } catch (Exception e) {
          LOG.error("task failed: " + task, e);
          throw e;
        }
        return null;
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
  private ExecutorService getExecutor(final Integer ticket) {
    return executors[ticket % executors.length];
  }
}
