package com.softwaremill;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class TestCaffeineAsync {
    public static void main(String[] args) throws InterruptedException {
        AtomicReference<Integer> db = new AtomicReference<>(1);

        AsyncLoadingCache<String, Integer> c = Caffeine.newBuilder().buildAsync((k, executor) -> CompletableFuture.supplyAsync(() -> {
            println("Reading from db ...");
            Integer v = db.get();
            doSleep(1000L);
            println("Read from db: " + v);
            return v;
        }));

        Thread t1 = new Thread(() -> {
            Integer g = getFromCache(c, "k");
            println("Got from cache: " + g);
        });

        Thread t2 = new Thread(() -> {
            doSleep(500L);
            println("Writing to db ...");
            db.set(2);
            println("Wrote to db");
            c.synchronous().invalidate("k");
            println("Invalidated cached");
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println();
        println("In cache: " + getFromCache(c, "k"));
    }

    private static void doSleep(Long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final long start = System.currentTimeMillis();
    private static void println(String msg) {
        System.out.printf("%s (%d): %s\n", Thread.currentThread().getName(), System.currentTimeMillis()-start, msg);
    }

    private static <K, V> V getFromCache(AsyncLoadingCache<K, V> c, K key) {
        try {
            return c.get(key).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
