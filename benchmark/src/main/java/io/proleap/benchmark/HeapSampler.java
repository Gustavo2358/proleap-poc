package io.proleap.benchmark;

import java.util.concurrent.atomic.*;

final class HeapSampler implements AutoCloseable {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong peak = new AtomicLong();
    private final Thread thread;

    HeapSampler() {
        sample();
        thread = new Thread(() -> {
            while (running.get()) {
                sample();
                try { Thread.sleep(1); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
            }
        }, "heap-sampler");
        thread.setDaemon(true);
        thread.start();
    }

    private void sample() {
        Runtime r = Runtime.getRuntime();
        peak.accumulateAndGet(r.totalMemory() - r.freeMemory(), Math::max);
    }

    long peak() { sample(); return peak.get(); }
    @Override public void close() { running.set(false); thread.interrupt(); try { thread.join(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
