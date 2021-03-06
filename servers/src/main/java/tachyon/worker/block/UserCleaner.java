/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.worker.block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tachyon.Constants;
import tachyon.conf.TachyonConf;
import tachyon.util.CommonUtils;

/**
 * UserCleaner periodically checks if any user have become zombies, removes the zombie user and
 * associated data when necessary. The syncing parameters (intervals) adopt directly from
 * worker-to-master heartbeat configurations.
 */
public class UserCleaner implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);
  /** Block data manager responsible for interacting with Tachyon and UFS storage */
  private final BlockDataManager mBlockDataManager;
  /** The configuration values */
  private final TachyonConf mTachyonConf;
  /** Milliseconds between each check */
  private final int mCheckIntervalMs;

  /** Flag to indicate if the checking should continue */
  private volatile boolean mRunning;

  /**
   * Constructor for UserCleaner
   *
   * @param blockDataManager the blockDataManager this checker is updating to
   * @param tachyonConf the configuration values to be used
   */
  public UserCleaner(BlockDataManager blockDataManager, TachyonConf tachyonConf) {
    mBlockDataManager = blockDataManager;
    mTachyonConf = tachyonConf;
    mCheckIntervalMs =
        mTachyonConf.getInt(Constants.WORKER_TO_MASTER_HEARTBEAT_INTERVAL_MS, Constants.SECOND_MS);

    mRunning = true;
  }


  /**
   * Main loop for the cleanup, continuously look for zombie users
   */
  @Override
  public void run() {
    long lastCheckMs = System.currentTimeMillis();
    while (mRunning) {
      // Check the time since last check, and wait until it is within check interval
      long lastIntervalMs = System.currentTimeMillis() - lastCheckMs;
      long toSleepMs = mCheckIntervalMs - lastIntervalMs;
      if (toSleepMs > 0) {
        CommonUtils.sleepMs(LOG, toSleepMs);
      } else {
        LOG.warn("User cleanup took: " + lastIntervalMs + ", expected: " + mCheckIntervalMs);
      }

      // Check if any users have become zombies, if so clean them up
      mBlockDataManager.cleanupUsers();
      lastCheckMs = System.currentTimeMillis();
    }
  }

  /**
   * Stops the checking, once this method is called, the object should be discarded
   */
  public void stop() {
    mRunning = false;
  }
}
