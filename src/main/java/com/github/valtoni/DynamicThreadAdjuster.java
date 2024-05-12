package com.github.valtoni;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.sleep;
import static com.github.valtoni.Collider.COLLISION_FOUND;

public class DynamicThreadAdjuster implements Runnable {

        private final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 4;

        private final AtomicReference<ExecutorService> executorRef;
        private final double targetCpuLoad;
        private final double adjustmentThreshold;
        private final SHA1Unit preimage;
        private final List<Future<?>> tasks = new ArrayList<>();

        public DynamicThreadAdjuster(AtomicReference<ExecutorService> executorRef, double targetCpuLoad, double adjustmentThreshold, SHA1Unit preimage) {
            this.executorRef = executorRef;
            this.targetCpuLoad = targetCpuLoad;
            this.adjustmentThreshold = adjustmentThreshold;
            this.preimage = preimage;
            System.out.println("Target CPU load: " + targetCpuLoad + "%, maxthreads: " + MAX_THREADS);
        }

        private void adjustExecutorService(int processors) {
            if (processors > tasks.size()) {
                // If the number of threads is increased, submit new CollisionTasks
                while (processors > tasks.size()) {
                    tasks.add(executorRef.get().submit(new CollisionTask(preimage)));
                }
            } else if (processors < tasks.size()) {
                // If the number of threads is decreased, cancel the last added tasks
                while (processors < tasks.size()) {
                    tasks.removeLast().cancel(true);
                }
            }
        }

        @Override
        public void run() {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            int processors = Runtime.getRuntime().availableProcessors();
            adjustExecutorService(processors);
            double thresholdHalf = adjustmentThreshold / 2;
            while (!COLLISION_FOUND.get()) {
                double cpuLoad = osBean.getSystemLoadAverage();
                if ((cpuLoad > targetCpuLoad - thresholdHalf) && processors > 1) {
                    // CPU load is too high, decrease the number of threads
                    processors--;
                    adjustExecutorService(processors);
                    System.out.println("Decreasing threads to " + processors + " due to high CPU load (" + cpuLoad*100 + "%)");
                } else if (cpuLoad < targetCpuLoad - thresholdHalf && processors < MAX_THREADS) {
                    // CPU load is too low, increase the number of threads
                    processors++;
                    adjustExecutorService(processors);
                    System.out.println("Increasing threads to " + processors + " due to low CPU load (" + cpuLoad*100 + "%)");
                }
                try {
                    //noinspection BusyWait
                    sleep(1000); // Adjust the sleep time as needed
                } catch (InterruptedException e) {
                    break;
                }
            }
            tasks.forEach(task -> task.cancel(true));
            executorRef.get().shutdown();
        }
    }