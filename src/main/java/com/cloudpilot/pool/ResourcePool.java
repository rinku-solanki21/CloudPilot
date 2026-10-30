package com.cloudpilot.pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * ResourcePool<T> (Generic Resource Management Component)
 * 
 * Demonstrates:
 * 1. Java Generics (<T>): Enables type-safe reuse for any resource type 
 *    (Virtual Machines, Database Connections, Thread Workers, etc.).
 * 2. Resource Lifecycle Management: Tracks available vs. active (in-use) resources.
 * 3. Thread Synchronization: Ensures thread-safe acquisition and release in concurrent environments.
 * 
 * @param <T> The type of cloud resource managed by this pool
 */
public class ResourcePool<T> {

    // Queue of resources currently available to be acquired
    private final Queue<T> availableResources;

    // Set of resources currently in active use
    private final Set<T> activeResources;

    // Master list of all registered resources in the pool
    private final List<T> allResources;

    // Maximum allowed capacity for this pool
    private final int maxCapacity;

    /**
     * Constructor initializing the generic pool with a maximum capacity.
     * 
     * @param maxCapacity Upper bound of resources allowed in the pool
     */
    public ResourcePool(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Resource pool capacity must be greater than zero.");
        }
        this.maxCapacity = maxCapacity;
        this.availableResources = new LinkedList<>();
        this.activeResources = new HashSet<>();
        this.allResources = new ArrayList<>();
    }

    /**
     * Adds a new resource to the pool.
     * 
     * @param resource The resource to register
     * @return true if added, false if maximum capacity was reached
     */
    public synchronized boolean addResource(T resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Cannot add a null resource to the pool.");
        }
        if (allResources.size() >= maxCapacity) {
            return false;
        }
        allResources.add(resource);
        availableResources.offer(resource);
        return true;
    }

    /**
     * Removes a resource entirely from the pool.
     * 
     * @param resource The resource to remove
     * @return true if removed, false otherwise
     */
    public synchronized boolean removeResource(T resource) {
        if (resource == null) return false;
        boolean removed = allResources.remove(resource);
        availableResources.remove(resource);
        activeResources.remove(resource);
        return removed;
    }

    /**
     * Acquires an available resource from the pool.
     * Moves the resource from the available queue to the active set.
     * 
     * @return An available resource, or null if all resources are currently active
     */
    public synchronized T acquireResource() {
        T resource = availableResources.poll();
        if (resource != null) {
            activeResources.add(resource);
        }
        return resource;
    }

    /**
     * Releases a previously acquired resource back into the available pool.
     * 
     * @param resource The resource to return
     * @return true if successfully returned, false if resource was not active
     */
    public synchronized boolean releaseResource(T resource) {
        if (resource != null && activeResources.remove(resource)) {
            availableResources.offer(resource);
            return true;
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of all registered resources.
     */
    public synchronized List<T> getAllResources() {
        return Collections.unmodifiableList(new ArrayList<>(allResources));
    }

    /**
     * Returns count of currently available (idle) resources.
     */
    public synchronized int getAvailableCount() {
        return availableResources.size();
    }

    /**
     * Returns count of currently active (busy) resources.
     */
    public synchronized int getActiveCount() {
        return activeResources.size();
    }

    /**
     * Returns total number of resources registered in this pool.
     */
    public synchronized int size() {
        return allResources.size();
    }

    /**
     * Returns configured max capacity.
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Checks if the pool has reached maximum capacity.
     */
    public synchronized boolean isFull() {
        return allResources.size() >= maxCapacity;
    }

    /**
     * Clears all resources from the pool.
     */
    public synchronized void clear() {
        availableResources.clear();
        activeResources.clear();
        allResources.clear();
    }
}
