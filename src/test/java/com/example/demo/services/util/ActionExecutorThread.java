package com.example.demo.services.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class ActionExecutorThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(ActionExecutorThread.class);

    private final CountDownLatch latch;
    private final int numberOfCycles;
    private final double probability;
    private final Supplier<Void> action;
    private int operationsCount = 0;
    private long elapsedTimeInMillis = 0;

    public ActionExecutorThread(CountDownLatch latch, int numberOfCycles, double probability, Supplier<Void> action) {
        this.latch = latch;
        this.numberOfCycles = numberOfCycles;
        this.probability = probability;
        this.action = action;
    }

    @Override
    public void run() {
        for (int i = 0; i < numberOfCycles; i++) {
            if (Math.random() < probability) {
                long startTime = System.nanoTime();
                action.get();
                long endTime = System.nanoTime();
                elapsedTimeInMillis += (endTime - startTime) / 1_000_000;
                operationsCount++;
            }
        }
        LOG.info("{} had performed the number of operations - {}, elapsed time - {} milliseconds.",
                getName(), operationsCount, elapsedTimeInMillis);
        latch.countDown();
    }
}
