package edu.cmu.cc.webtier;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Wrapper class for cache.
 */
public class Cache {

    /**
     * Internal cache implementation.
     */
    LinkedHashMap<String, RowSet<Row>> internalCache;
    int capacity;
    int threshold;
    int limit;

    Cache(int capacity, int limit) {
        internalCache = new LinkedHashMap<>(capacity);
        this.capacity = capacity;
        this.limit = limit;
        threshold = 10000;
    }


    /**
     * Returns the value to which the specified key is mapped,
     * or null if this cache contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         null if this cache contains no mapping for the key
     */
    public synchronized RowSet<Row> get(String key) {
        RowSet<Row> ret = internalCache.get(key);
        return ret;
    }

    /**
     * Puts key-value pair in the cache.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public synchronized void put(String key, RowSet<Row> value) {
        if (value.size() <= limit) {
            if(internalCache.size() >= capacity) {
                Map.Entry<String, RowSet<Row>> entry = internalCache.entrySet().iterator().next();
                internalCache.remove(entry.getKey());
            }
            internalCache.put(key, value);
        }
    }

}

