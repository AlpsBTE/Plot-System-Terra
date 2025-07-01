package com.alpsbte.plotsystemterra.utils;

import java.lang.ref.WeakReference;
import java.util.*;

public class ExpiringCacheMap<K, V> extends HashMap<K, V> {

    static {
        // Plugin's caching thread
        // Detect expired cache and remove from the HashMap
        new ExpiryThread().start();
    }

    private final HashMap<K, Long> expiryTimes = new HashMap<>();
    private final long expiryDelay;

    public ExpiringCacheMap(long expiryDelayMillis) {
        this.expiryDelay = expiryDelayMillis;
        ExpiryThread.references.add(new WeakReference<>(this));
    }

    @Override
    public V put(K key, V value) {
        synchronized (expiryTimes) {
            expiryTimes.put(key, System.currentTimeMillis() + expiryDelay);
        }
        return super.put(key, value);
    }

    @SuppressWarnings("UnusedReturnValue")
    public V putNotExpiring(K key, V value) {
        return super.put(key, value);
    }

    public V putExpiring(K key, V value, long expiryTime) {
        if (expiryTime < System.currentTimeMillis()) throw new IllegalArgumentException("The expiry time must be in the future");
        synchronized (expiryTimes) {
            expiryTimes.put(key, expiryTime);
        }
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        synchronized (expiryTimes) {
            expiryTimes.remove(key);
        }
        return super.remove(key);
    }

    public long getExpiryTime(K key) {
        if (!containsKey(key)) throw new IllegalArgumentException("The given key is not in the map");
        return expiryTimes.get(key);
    }

    public void setExpiryTime(K key, long expiryTimeMillis) {
        if (!containsKey(key)) throw new IllegalArgumentException("The given key is not in the map");
        expiryTimes.put(key, expiryTimeMillis);
    }

    public long getExpiryDelay() {
        return expiryDelay;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void keyExpired(Object key) {
        remove(key);
        expiryTimes.remove(key);
    }

    public static class ExpiryThread extends Thread {

        private static final Set<WeakReference<ExpiringCacheMap<?, ?>>> references = new HashSet<>();

        private ExpiryThread() {
            super( "PlotSystem-Terra " + ExpiryThread.class.getSimpleName());
            Runtime.getRuntime().addShutdownHook(new Thread(this::interrupt));
        }

        @Override
        @SuppressWarnings("BusyWait")
        public void run() {
            while (!isInterrupted()) {
                long currentTime = System.currentTimeMillis();
                for (WeakReference<ExpiringCacheMap<?, ?>> reference : new HashSet<>(references)) {
                    final ExpiringCacheMap<?, ?> collection = reference.get();
                    if (collection == null) {
                        references.remove(reference);
                        continue;
                    }
                    Map<?, Long> expiryTimes;
                    synchronized (collection.expiryTimes) {
                        expiryTimes = new HashMap<>(collection.expiryTimes);
                    }
                    List<Object> removals = new ArrayList<>();
                    expiryTimes.entrySet().stream()
                            .filter(entry -> entry.getValue() < currentTime)
                            .forEach(entry -> removals.add(entry.getKey()));
                    synchronized (collection) {
                        removals.forEach(collection::keyExpired);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }
}
