package com.cloudpilot.autoscaler;

import com.cloudpilot.balancer.LoadBalancer;
import com.cloudpilot.exceptions.MaxClusterCapacityException;
import com.cloudpilot.model.VirtualMachine;
import com.cloudpilot.pool.ResourcePool;
import com.cloudpilot.service.ActivityLogger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AutoScalerThread (Multithreading & Autonomous Scaling Component)
 * 
 * Extends java.lang.Thread to continuously monitor the health and load of the 
 * virtual machine cluster in the background.
 * 
 * Key Responsibilities:
 * 1. Periodically (every 2.5s) measures average cluster CPU utilization.
 * 2. Compares average CPU against the scaling threshold (80.0%).
 * 3. Proactively creates and registers a new VM when the threshold is exceeded.
 * 4. Strictly enforces the maximum cluster capacity limit (throws MaxClusterCapacityException).
 * 5. Gradually cools down idle VM CPU loads back towards default baseline (40.0%).
 * 6. Supports pause, resume, and graceful shutdown controls.
 */
public class AutoScalerThread extends Thread {

    public static final double CPU_SCALING_THRESHOLD = 80.0;
    public static final int DEFAULT_MAX_CAPACITY = 8;
    private static final long MONITOR_INTERVAL_MS = 2500; // Check every 2.5 seconds

    private final ResourcePool<VirtualMachine> resourcePool;
    private final LoadBalancer loadBalancer;
    private final ActivityLogger activityLogger;
    private final int maxClusterCapacity;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicInteger vmSequenceCounter = new AtomicInteger(4); // Starts after VM-01, VM-02, VM-03

    private double lastRecordedAverageCpu = 40.0;

    /**
     * Constructor
     * 
     * @param resourcePool Shared generic resource pool
     * @param loadBalancer Shared round robin load balancer
     * @param activityLogger Central activity event logger
     * @param maxClusterCapacity Configured upper limit for VM count
     */
    public AutoScalerThread(ResourcePool<VirtualMachine> resourcePool,
                            LoadBalancer loadBalancer,
                            ActivityLogger activityLogger,
                            int maxClusterCapacity) {
        super("AutoScaler-Monitor-Thread");
        this.resourcePool = resourcePool;
        this.loadBalancer = loadBalancer;
        this.activityLogger = activityLogger;
        this.maxClusterCapacity = maxClusterCapacity;
        // Set as daemon so it does not block application shutdown
        this.setDaemon(true);
    }

    @Override
    public void run() {
        activityLogger.info("AutoScaler background monitoring thread started. Target threshold: 80.0% CPU.");

        while (running.get()) {
            try {
                Thread.sleep(MONITOR_INTERVAL_MS);

                // If paused by dashboard operator, skip evaluation
                if (paused.get()) {
                    continue;
                }

                // 1. Evaluate current cluster telemetry
                evaluateClusterAndScale();

                // 2. Perform natural cooling decay on idle VMs to simulate realistic workloads
                applyCpuCoolDown();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activityLogger.warn("AutoScaler thread interrupted. Exiting monitoring loop.");
                break;
            } catch (Exception e) {
                activityLogger.alert("Error in AutoScaler monitoring cycle: " + e.getMessage());
            }
        }
    }

    /**
     * Inspects average CPU across all healthy VMs and triggers scale-out if needed.
     */
    private synchronized void evaluateClusterAndScale() {
        List<VirtualMachine> vms = loadBalancer.getVmList();
        if (vms.isEmpty()) {
            return;
        }

        // Calculate average CPU across all VMs
        double totalCpu = 0.0;
        int activeCount = 0;
        for (VirtualMachine vm : vms) {
            totalCpu += vm.getCpuUtilizationPercentage();
            activeCount++;
        }

        double avgCpu = activeCount > 0 ? (totalCpu / activeCount) : 0.0;
        this.lastRecordedAverageCpu = Math.round(avgCpu * 10.0) / 10.0;

        // Check if CPU threshold has been exceeded
        if (avgCpu >= CPU_SCALING_THRESHOLD) {
            activityLogger.alert(String.format("High cluster load detected! Average CPU: %.1f%% (exceeds %.0f%% threshold).",
                    avgCpu, CPU_SCALING_THRESHOLD));

            // Check against maximum cluster capacity
            if (vms.size() >= maxClusterCapacity) {
                try {
                    throw new MaxClusterCapacityException(vms.size(), maxClusterCapacity);
                } catch (MaxClusterCapacityException ex) {
                    activityLogger.alert("MAX CAPACITY REACHED: " + ex.getMessage());
                }
                return;
            }

            // Provision a new Virtual Machine
            scaleOut(avgCpu);
        }
    }

    /**
     * Provisions a new VirtualMachine, assigns baseline metrics, and registers it
     * into both the generic ResourcePool and the Round Robin LoadBalancer.
     */
    public synchronized VirtualMachine scaleOut(double triggerCpu) {
        if (loadBalancer.getClusterSize() >= maxClusterCapacity) {
            throw new MaxClusterCapacityException(loadBalancer.getClusterSize(), maxClusterCapacity);
        }

        String newVmId = String.format("VM-%02d", vmSequenceCounter.getAndIncrement());
        VirtualMachine newVm = new VirtualMachine(newVmId, VirtualMachine.DEFAULT_BASELINE_CPU, VirtualMachine.DEFAULT_BASELINE_MEM, true);

        // Register with ResourcePool
        resourcePool.addResource(newVm);

        // Register with LoadBalancer
        loadBalancer.addVM(newVm);

        activityLogger.scaled(String.format("Auto-scaling triggered: %s provisioned and attached to pool & load balancer. Cluster capacity: %d/%d.",
                newVmId, loadBalancer.getClusterSize(), maxClusterCapacity));

        return newVm;
    }

    /**
     * Simulates natural cloud resource recovery:
     * When traffic subsides, idle VMs cool down back toward baseline (~40%).
     */
    private void applyCpuCoolDown() {
        List<VirtualMachine> vms = loadBalancer.getVmList();
        for (VirtualMachine vm : vms) {
            // Gradually decrement active connection counts and cool down CPU
            if (vm.getActiveConnections() > 0) {
                vm.releaseRequest();
            }
            vm.coolDown(3.0);
        }
    }

    // =========================================================================
    // Control & Telemetry Methods
    // =========================================================================

    public void pauseMonitoring() {
        paused.set(true);
        activityLogger.warn("Auto-scaler monitoring PAUSED by operator.");
    }

    public void resumeMonitoring() {
        paused.set(false);
        activityLogger.info("Auto-scaler monitoring RESUMED by operator.");
    }

    public boolean isPaused() {
        return paused.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void stopMonitoring() {
        running.set(false);
        this.interrupt();
    }

    public double getLastRecordedAverageCpu() {
        return lastRecordedAverageCpu;
    }

    public int getMaxClusterCapacity() {
        return maxClusterCapacity;
    }

    public void resetSequence(int nextId) {
        vmSequenceCounter.set(nextId);
    }
}
