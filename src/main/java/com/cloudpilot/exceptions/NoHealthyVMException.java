package com.cloudpilot.exceptions;

/**
 * NoHealthyVMException
 * 
 * Custom unchecked exception thrown by the LoadBalancer when an incoming request 
 * cannot be routed because all virtual machines in the pool are marked UNHEALTHY.
 * 
 * Demonstrates custom exception handling and fault-tolerance concepts in Java.
 */
public class NoHealthyVMException extends RuntimeException {

    private final int totalVmsChecked;

    public NoHealthyVMException(String message) {
        super(message);
        this.totalVmsChecked = 0;
    }

    public NoHealthyVMException(int totalVmsChecked) {
        super(String.format("Load balancing failed: All %d virtual machines in the pool are UNHEALTHY or down. No target VM available to process incoming request.",
                totalVmsChecked));
        this.totalVmsChecked = totalVmsChecked;
    }

    public int getTotalVmsChecked() {
        return totalVmsChecked;
    }
}
