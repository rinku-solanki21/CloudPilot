package com.cloudpilot.exceptions;

/**
 * MaxClusterCapacityException
 * 
 * Custom unchecked exception thrown when an auto-scaling attempt is made
 * but the cluster has already reached its configured maximum virtual machine limit.
 * 
 * Demonstrates custom exception hierarchy in Java OOP.
 */
public class MaxClusterCapacityException extends RuntimeException {

    private final int currentCapacity;
    private final int maxCapacity;

    public MaxClusterCapacityException(String message) {
        super(message);
        this.currentCapacity = 0;
        this.maxCapacity = 0;
    }

    public MaxClusterCapacityException(int currentCapacity, int maxCapacity) {
        super(String.format("Auto-scaling failed: Maximum cluster capacity reached (%d/%d VMs). Cannot provision more virtual machines.",
                currentCapacity, maxCapacity));
        this.currentCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
    }

    public int getCurrentCapacity() {
        return currentCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
