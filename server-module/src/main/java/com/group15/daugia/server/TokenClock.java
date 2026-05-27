package com.group15.daugia.server;

import com.group15.daugia.server.DAO.UserDAO;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Scheduler dọn token hết hạn theo idle-timeout. */
public class TokenClock {
  public static final long SWEEP_INTERVAL_MINUTES = 15;

  private static TokenClock instance;

  private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<?> sweepJob;
  private final UserDAO dao = UserDAO.getUserDao();

  private TokenClock() {}

  public static TokenClock getInstance() {
    if (instance == null) {
      synchronized (TokenClock.class) {
        if (instance == null) {
          instance = new TokenClock();
        }
      }
    }
    return instance;
  }

  public synchronized void start() {
    if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
      scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    if (sweepJob != null && !sweepJob.isCancelled() && !sweepJob.isDone()) {
      return;
    }
    sweepJob =
        scheduler.scheduleAtFixedRate(
            this::sweepExpiredTokens,
            SWEEP_INTERVAL_MINUTES,
            SWEEP_INTERVAL_MINUTES,
            TimeUnit.MINUTES);
  }

  public synchronized void shutdownAndClearTokens() {
    if (sweepJob != null) {
      sweepJob.cancel(false);
    }
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    dao.deleteAllTokens();
  }

  private void sweepExpiredTokens() {
    dao.deleteExpiredTokens();
  }
}
