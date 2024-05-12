package org.github.valtoni;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Collider {

    public static final AtomicBoolean COLLISION_FOUND = new AtomicBoolean(false);
    public static final AtomicLong TOTAL_TESTS = new AtomicLong(0);

    private static void SHA1ThreadedCollisionTest(int threads) {
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            SHA1Unit preimage = new SHA1Unit();
            for (int i = 0; i < threads; i++) {
                executor.submit(new CollisionTask(preimage));
            }
            executor.shutdown();
        }
    }

    private static void SHA1DynamicThreadedCollisionTest(double targetCpuLoad, double adjustmentThreshold) {
        SHA1Unit preimage = new SHA1Unit();
        int maxThreads = Runtime.getRuntime().availableProcessors() * 1024;
        AtomicReference<ExecutorService> executorRef = new AtomicReference<>(Executors.newFixedThreadPool(maxThreads));
        Thread adjusterThread = new Thread(new DynamicThreadAdjuster(executorRef, targetCpuLoad / 100, adjustmentThreshold / 100, preimage));
        adjusterThread.start();
        // Use executorRef.get() to get the current ExecutorService
    }

    public static void main(String[] args) {
        //SHA1ThreadedCollisionTest(Runtime.getRuntime().availableProcessors() * 2);
        SHA1DynamicThreadedCollisionTest(95, 10);
    }

}
