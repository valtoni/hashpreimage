package org.github.valtoni;

import java.lang.management.ManagementFactory;

import static org.github.valtoni.Collider.COLLISION_FOUND;
import static org.github.valtoni.Collider.TOTAL_TESTS;

public class CollisionTask implements Runnable {

    private final SHA1Unit preimage;
    private Long tests = 0L;

    public CollisionTask(SHA1Unit preimage) {
        this.preimage = preimage;
    }

    @Override
    public void run() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        String pid = jvmName.split("@")[0];
        System.out.println("PID: " + pid + ", thread: " + Thread.currentThread().getName());
        SHA1Unit image;
        boolean collided = false;
        do {
            image = new SHA1Unit();
            tests++;
            if (Thread.currentThread().isInterrupted()) {
                // The task has been cancelled, exit the loop
                System.out.println("*** CANCELLED - PID: " + pid + ", thread: " + Thread.currentThread().getName());
                break;
            }
            collided = preimage.equals(image);
            if (collided) {
                COLLISION_FOUND.set(true);
            }
            if (tests % 1000000 == 0) {
                TOTAL_TESTS.set(TOTAL_TESTS.get() + tests);
                System.out.println("PID: " + pid + ", thread: " + Thread.currentThread().getName() + ", tests: " + tests + ", total tests: " + TOTAL_TESTS.get() + ", collision found: " + COLLISION_FOUND.get());
            }
        } while (!COLLISION_FOUND.get());
        if (collided) {
            System.out.println("Collision found! " + preimage.hexDigest() + " == " + image.hexDigest());
            System.out.println("Input1: " + preimage.hexInput());
            System.out.println("Input2: " + image.hexInput());
        }
    }
}