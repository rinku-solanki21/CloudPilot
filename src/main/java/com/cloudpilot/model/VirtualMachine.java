package com.cloudpilot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * VirtualMachine (Model Component)
 * 
 * Demonstrates Object-Oriented Programming (OOP) Encapsulation by bundling 
 * VM state variables (CPU, Memory, Connections, Health) with appropriate 
 * getter/setter methods and business logic for handling simulated traffic.
 * 
 * B.Tech 3rd Semester OOP Project
 */
public class VirtualMachine {

    // 1. Mandatory Attributes
    private String vmId;
    private double cpuUtilizationPercentage; // 0.0% to 100.0%
    private int activeConnections;
    private int memoryUsedMB;
    private boolean isHealthy;

    // Additional telemetry attributes for a rich cloud dashboard
    private long totalRequestsHandled;
    private final int totalMemoryMB = 2048; // Max 2GB per simulated VM
    private String creationTime;
    private boolean isAutoScaled; // Flag to indicate if this VM was spawned by AutoScalerThread

    // Baseline stats
    public static final double DEFAULT_BASELINE_CPU = 40.0;
    public static final int DEFAULT_BASELINE_MEM = 512;

    /**
     * Default Constructor
     */
    public VirtualMachine() {
        this("VM-00", DEFAULT_BASELINE_CPU, DEFAULT_BASELINE_MEM, false);
    }

    /**
     * Parameterized Constructor
     * 
     * @param vmId Unique identifier (e.g. "VM-01")
     * @param initialCpu Initial CPU load percentage (~40%)
     * @param initialMemory Initial RAM usage in MB (~512MB)
     * @param isAutoScaled True if created dynamically by AutoScaler
     */
    public VirtualMachine(String vmId, double initialCpu, int initialMemory, boolean isAutoScaled) {
        this.vmId = vmId;
        this.cpuUtilizationPercentage = Math.max(0.0, Math.min(100.0, initialCpu));
        this.memoryUsedMB = initialMemory;
        this.activeConnections = 0;
        this.isHealthy = true;
        this.totalRequestsHandled = 0;
        this.isAutoScaled = isAutoScaled;
        this.creationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // =========================================================================
    // Core Business Operations (Synchronized for Thread-Safety)
    // =========================================================================

    /**
     * Assigns an incoming request to this VM.
     * Increases active connections, total requests handled, and transient CPU/Memory load.
     */
    public synchronized void assignRequest() {
        this.activeConnections++;
        this.totalRequestsHandled++;

        // Each active request adds between 3% and 7% temporary CPU load
        double cpuSpike = 4.0 + (Math.random() * 3.0);
        this.cpuUtilizationPercentage = Math.min(100.0, this.cpuUtilizationPercentage + cpuSpike);

        // Memory increases slightly with connection overhead (15MB - 30MB)
        int memSpike = 15 + (int)(Math.random() * 15);
        this.memoryUsedMB = Math.min(this.totalMemoryMB, this.memoryUsedMB + memSpike);
    }

    /**
     * Releases a completed request from this VM.
     */
    public synchronized void releaseRequest() {
        if (this.activeConnections > 0) {
            this.activeConnections--;
        }
    }

    /**
     * Updates CPU utilization directly with bounds validation [0.0, 100.0].
     */
    public synchronized void setCpuUtilizationPercentage(double cpuUtilizationPercentage) {
        this.cpuUtilizationPercentage = Math.max(0.0, Math.min(100.0, Math.round(cpuUtilizationPercentage * 10.0) / 10.0));
    }

    /**
     * Gradually cools down CPU utilization toward baseline when idle.
     * Called periodically by the monitoring/autoscaler routine.
     */
    public synchronized void coolDown(double decayAmount) {
        if (this.activeConnections == 0 && this.cpuUtilizationPercentage > DEFAULT_BASELINE_CPU) {
            this.cpuUtilizationPercentage = Math.max(DEFAULT_BASELINE_CPU, this.cpuUtilizationPercentage - decayAmount);
            if (this.memoryUsedMB > DEFAULT_BASELINE_MEM) {
                this.memoryUsedMB = Math.max(DEFAULT_BASELINE_MEM, this.memoryUsedMB - 20);
            }
        }
    }

    /**
     * Resets statistics to default baseline.
     */
    public synchronized void resetStats() {
        this.cpuUtilizationPercentage = DEFAULT_BASELINE_CPU;
        this.memoryUsedMB = DEFAULT_BASELINE_MEM;
        this.activeConnections = 0;
        this.totalRequestsHandled = 0;
        this.isHealthy = true;
    }

    // =========================================================================
    // Standard Getters & Setters
    // =========================================================================

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public double getCpuUtilizationPercentage() {
        return Math.round(cpuUtilizationPercentage * 10.0) / 10.0;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = Math.max(0, activeConnections);
    }

    public int getMemoryUsedMB() {
        return memoryUsedMB;
    }

    public void setMemoryUsedMB(int memoryUsedMB) {
        this.memoryUsedMB = Math.max(0, Math.min(totalMemoryMB, memoryUsedMB));
    }

    public int getTotalMemoryMB() {
        return totalMemoryMB;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public long getTotalRequestsHandled() {
        return totalRequestsHandled;
    }

    public void setTotalRequestsHandled(long totalRequestsHandled) {
        this.totalRequestsHandled = totalRequestsHandled;
    }

    public boolean isAutoScaled() {
        return isAutoScaled;
    }

    public void setAutoScaled(boolean autoScaled) {
        isAutoScaled = autoScaled;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public String toString() {
        return String.format("VirtualMachine[id=%s, cpu=%.1f%%, mem=%dMB, conns=%d, healthy=%b]",
                vmId, cpuUtilizationPercentage, memoryUsedMB, activeConnections, isHealthy);
    }
}
