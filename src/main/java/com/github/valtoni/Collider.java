package com.github.valtoni;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Collider {

    public static final AtomicBoolean COLLISION_FOUND = new AtomicBoolean(false);
    public static final AtomicLong TOTAL_TESTS = new AtomicLong(0);

    private static void SHA1ThreadedCollisionTest(int threads) {
        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            SHA1Unit preimage = new SHA1Unit();
            for (int i = 0; i < threads; i++) {
                executor.submit(new CollisionTask(preimage));
            }
            executor.shutdown();
        }
    }

    private static void SHA1DynamicThreadedCollisionTest(double targetCpuLoad, double adjustmentThreshold) {
        SHA1Unit preimage = new SHA1Unit();
        AtomicReference<ExecutorService> executorRef = new AtomicReference<>(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        Thread adjusterThread = new Thread(new DynamicThreadAdjuster(executorRef, targetCpuLoad / 100, adjustmentThreshold / 100, preimage));
        adjusterThread.start();
        // Use executorRef.get() to get the current ExecutorService
    }

    private static void SHA1ThreadedVirtualCollisionTest(int threads) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            SHA1Unit preimage = new SHA1Unit();
            for (int i = 0; i < threads; i++) {
                executor.submit(new CollisionTask(preimage));
            }
            executor.shutdown();
        }
    }

    private static void SHA1DynamicThreadedVirtualCollisionTest(double targetCpuLoad, double adjustmentThreshold) {
        SHA1Unit preimage = new SHA1Unit();
        AtomicReference<ExecutorService> executorRef = new AtomicReference<>(Executors.newVirtualThreadPerTaskExecutor());
        Thread adjusterThread = new Thread(new DynamicThreadAdjuster(executorRef, targetCpuLoad / 100, adjustmentThreshold / 100, preimage));
        adjusterThread.start();
        // Use executorRef.get() to get the current ExecutorService
    }

    public static void main(String[] args) {
        //SHA1ThreadedCollisionTest(Runtime.getRuntime().availableProcessors() * 2);
        //SHA1DynamicThreadedCollisionTest(95, 10);
        //SHA1ThreadedVirtualCollisionTest(Runtime.getRuntime().availableProcessors() * 2);
        SHA1DynamicThreadedVirtualCollisionTest(95, 10);
    }

}
