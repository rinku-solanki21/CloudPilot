package com.cloudpilot.service;

import com.cloudpilot.autoscaler.AutoScalerThread;
import com.cloudpilot.balancer.LoadBalancer;
import com.cloudpilot.exceptions.MaxClusterCapacityException;
import com.cloudpilot.exceptions.NoHealthyVMException;
import com.cloudpilot.model.VirtualMachine;
import com.cloudpilot.pool.ResourcePool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SimulationService
 * 
 * Central coordinator connecting the generic ResourcePool, Round Robin LoadBalancer,
 * AutoScalerThread, and ActivityLogger.
 * 
 * Manages the lifecycle of the virtual machine pool and executes interactive
 * simulation commands from the frontend dashboard.
 */
@Service
public class SimulationService {

    public static final int MAX_CLUSTER_CAPACITY = 8;
    public static final int INITIAL_VM_COUNT = 3;

    private final ResourcePool<VirtualMachine> resourcePool;
    private final LoadBalancer loadBalancer;
    private final ActivityLogger activityLogger;
    private AutoScalerThread autoScalerThread;

    public SimulationService() {
        this.resourcePool = new ResourcePool<>(MAX_CLUSTER_CAPACITY);
        this.loadBalancer = new LoadBalancer();
        this.activityLogger = new ActivityLogger(100);
    }

    /**
     * Initializes the simulated cloud environment with 3 healthy baseline VMs
     * and kicks off the background AutoScaler monitoring thread.
     */
    @PostConstruct
    public synchronized void initializeCluster() {
        activityLogger.info("Initializing CloudPilot Cloud Infrastructure...");

        // Create 3 initial healthy virtual machines (VM-01, VM-02, VM-03)
        for (int i = 1; i <= INITIAL_VM_COUNT; i++) {
            String vmId = String.format("VM-%02d", i);
            VirtualMachine vm = new VirtualMachine(vmId, VirtualMachine.DEFAULT_BASELINE_CPU, VirtualMachine.DEFAULT_BASELINE_MEM, false);
            resourcePool.addResource(vm);
            loadBalancer.addVM(vm);
            activityLogger.info(String.format("Provisioned baseline virtual machine: %s (CPU: %.1f%%, RAM: %dMB).",
                    vmId, vm.getCpuUtilizationPercentage(), vm.getMemoryUsedMB()));
        }

        // Start AutoScaler background monitoring thread
        this.autoScalerThread = new AutoScalerThread(resourcePool, loadBalancer, activityLogger, MAX_CLUSTER_CAPACITY);
        this.autoScalerThread.start();

        activityLogger.success("CloudPilot cluster initialized with 3 healthy virtual machines.");
    }

    @PreDestroy
    public void cleanup() {
        if (autoScalerThread != null) {
            autoScalerThread.stopMonitoring();
        }
    }

    // =========================================================================
    // Simulation Controls
    // =========================================================================

    /**
     * Dispatches a single client request through the Round Robin load balancer.
     */
    public synchronized Map<String, Object> sendSingleRequest() {
        VirtualMachine assignedVm = loadBalancer.dispatchRequest();

        activityLogger.info(String.format("Request assigned to %s (Active Connections: %d, CPU: %.1f%%).",
                assignedVm.getVmId(), assignedVm.getActiveConnections(), assignedVm.getCpuUtilizationPercentage()));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("handledBy", assignedVm.getVmId());
        response.put("vmCpu", assignedVm.getCpuUtilizationPercentage());
        response.put("activeConnections", assignedVm.getActiveConnections());
        response.put("totalHandled", assignedVm.getTotalRequestsHandled());
        return response;
    }

    /**
     * Dispatches a batch of requests (e.g. 10 requests) evenly using Round Robin.
     */
    public synchronized Map<String, Object> sendBatchRequests(int count) {
        if (count <= 0) count = 10;
        Map<String, Integer> distribution = new LinkedHashMap<>();
        List<String> assignmentOrder = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            VirtualMachine vm = loadBalancer.dispatchRequest();
            distribution.put(vm.getVmId(), distribution.getOrDefault(vm.getVmId(), 0) + 1);
            assignmentOrder.add(vm.getVmId());
        }

        activityLogger.success(String.format("Batch of %d requests dispatched across cluster. Distribution: %s.",
                count, distribution));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("batchCount", count);
        result.put("distribution", distribution);
        result.put("assignmentOrder", assignmentOrder);
        return result;
    }

    /**
     * Simulates a sudden traffic surge.
     * Drives CPU load on all VMs above 80%, prompting AutoScalerThread to trigger scale-out.
     */
    public synchronized Map<String, Object> simulateTrafficSurge() {
        activityLogger.alert("SIMULATION: Sudden high-volume traffic surge incoming!");

        List<VirtualMachine> vms = loadBalancer.getVmList();
        int surgeRequests = 25;

        for (int i = 0; i < surgeRequests; i++) {
            try {
                loadBalancer.dispatchRequest();
            } catch (NoHealthyVMException ignored) {
                break;
            }
        }

        // Elevate VM CPU load to 82% - 94% to forcefully trigger the 80% threshold
        for (VirtualMachine vm : vms) {
            if (vm.isHealthy()) {
                double surgeCpu = 82.0 + (Math.random() * 12.0);
                vm.setCpuUtilizationPercentage(surgeCpu);
                vm.setActiveConnections(vm.getActiveConnections() + 3);
            }
        }

        activityLogger.alert(String.format("Traffic surge applied (%d requests). Average CPU spiked above 80%%. AutoScaler will trigger scale-out.",
                surgeRequests));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Traffic surge generated. Average CPU pushed above 80%.");
        result.put("averageCpu", getAverageCpu());
        return result;
    }

    /**
     * Toggles a VM to UNHEALTHY.
     */
    public synchronized Map<String, Object> markVmUnhealthy(String vmId) {
        VirtualMachine vm = loadBalancer.getVmById(vmId);
        if (vm == null) {
            throw new IllegalArgumentException("Virtual Machine not found: " + vmId);
        }

        vm.setHealthy(false);
        activityLogger.warn(String.format("Health Check: %s marked UNHEALTHY. Load Balancer will skip this node.", vmId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("vmId", vmId);
        result.put("isHealthy", false);
        return result;
    }

    /**
     * Restores a VM back to HEALTHY.
     */
    public synchronized Map<String, Object> markVmHealthy(String vmId) {
        VirtualMachine vm = loadBalancer.getVmById(vmId);
        if (vm == null) {
            throw new IllegalArgumentException("Virtual Machine not found: " + vmId);
        }

        vm.setHealthy(true);
        activityLogger.info(String.format("Health Check: %s restored to HEALTHY. Node back in rotation.", vmId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("vmId", vmId);
        result.put("isHealthy", true);
        return result;
    }

    /**
     * Resets the entire simulation back to initial clean state with 3 default VMs.
     */
    public synchronized Map<String, Object> resetSimulation() {
        activityLogger.warn("RESET: Reverting cluster to initial baseline state (3 VMs)...");

        // Clear existing pool and balancer
        resourcePool.clear();
        loadBalancer.clear();

        // Re-provision VM-01, VM-02, VM-03
        for (int i = 1; i <= INITIAL_VM_COUNT; i++) {
            String vmId = String.format("VM-%02d", i);
            VirtualMachine vm = new VirtualMachine(vmId, VirtualMachine.DEFAULT_BASELINE_CPU, VirtualMachine.DEFAULT_BASELINE_MEM, false);
            resourcePool.addResource(vm);
            loadBalancer.addVM(vm);
        }

        // Reset sequence counter in autoscaler
        if (autoScalerThread != null) {
            autoScalerThread.resetSequence(4);
            autoScalerThread.resumeMonitoring();
        }

        activityLogger.success("Simulation reset completed. Cluster restored with 3 healthy virtual machines.");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Simulation reset to baseline (3 healthy VMs).");
        return result;
    }

    public void pauseMonitoring() {
        if (autoScalerThread != null) {
            autoScalerThread.pauseMonitoring();
        }
    }

    public void resumeMonitoring() {
        if (autoScalerThread != null) {
            autoScalerThread.resumeMonitoring();
        }
    }

    // =========================================================================
    // Dashboard Telemetry
    // =========================================================================

    public synchronized Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        List<VirtualMachine> vms = loadBalancer.getVmList();

        int totalVms = vms.size();
        int healthyCount = 0;
        int unhealthyCount = 0;
        int totalActiveConnections = 0;
        double totalCpu = 0.0;
        long totalHandled = 0;

        for (VirtualMachine vm : vms) {
            if (vm.isHealthy()) {
                healthyCount++;
            } else {
                unhealthyCount++;
            }
            totalActiveConnections += vm.getActiveConnections();
            totalCpu += vm.getCpuUtilizationPercentage();
            totalHandled += vm.getTotalRequestsHandled();
        }

        double averageCpu = totalVms > 0 ? (totalCpu / totalVms) : 0.0;

        summary.put("totalVms", totalVms);
        summary.put("healthyVms", healthyCount);
        summary.put("unhealthyVms", unhealthyCount);
        summary.put("averageCpu", Math.round(averageCpu * 10.0) / 10.0);
        summary.put("activeConnections", totalActiveConnections);
        summary.put("totalRequestsHandled", totalHandled);
        summary.put("totalDispatched", loadBalancer.getTotalDispatchedRequests());
        summary.put("maxClusterCapacity", MAX_CLUSTER_CAPACITY);
        summary.put("cpuThreshold", AutoScalerThread.CPU_SCALING_THRESHOLD);
        summary.put("autoScalerRunning", autoScalerThread != null && autoScalerThread.isRunning());
        summary.put("autoScalerPaused", autoScalerThread != null && autoScalerThread.isPaused());
        summary.put("roundRobinPointer", loadBalancer.getCurrentIndex());

        return summary;
    }

    public List<VirtualMachine> getAllVms() {
        return loadBalancer.getVmList();
    }

    public double getAverageCpu() {
        List<VirtualMachine> vms = loadBalancer.getVmList();
        if (vms.isEmpty()) return 0.0;
        double sum = 0.0;
        for (VirtualMachine vm : vms) {
            sum += vm.getCpuUtilizationPercentage();
        }
        return Math.round((sum / vms.size()) * 10.0) / 10.0;
    }

    public List<ActivityLogger.LogEntry> getActivityLogs() {
        return activityLogger.getRecentLogs();
    }
}
