package com.codelry.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterruptHandler {
  private static final Logger LOGGER = LogManager.getLogger(InterruptHandler.class);
  private static final Object coordinator = new Object();

  public void run() throws InterruptedException {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOGGER.info("Break received");
      synchronized (coordinator) {
        coordinator.notify();
      }
    }));

    synchronized (coordinator) {
      coordinator.wait();
    }
  }
}
