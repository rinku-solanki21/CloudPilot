package com.cloudpilot.balancer;

import com.cloudpilot.exceptions.NoHealthyVMException;
import com.cloudpilot.model.VirtualMachine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * LoadBalancer (Round Robin Scheduling Component)
 * 
 * Implements a thread-safe Round Robin load-balancing algorithm using a
 * synchronized LinkedList<VirtualMachine>.
 * 
 * Key Responsibilities:
 * 1. Distributes incoming client requests evenly across all healthy VMs.
 * 2. Advances pointer sequentially (Round Robin) to achieve fair load distribution.
 * 3. Skips any VM that is currently marked UNHEALTHY.
 * 4. Detects total failure and throws NoHealthyVMException when no healthy node exists.
 * 5. Records detailed request routing records for dashboard telemetry.
 */
public class LoadBalancer {

    // Synchronized LinkedList holding registered virtual machines
    private final LinkedList<VirtualMachine> vmList;

    // Pointer tracking the current position in the Round Robin cycle
    private int currentIndex = 0;

    // Total request counter processed by this load balancer instance
    private long totalDispatchedRequests = 0;

    /**
     * Constructor initializing the internal LinkedList.
     */
    public LoadBalancer() {
        this.vmList = new LinkedList<>();
    }

    /**
     * Registers a new VM into the load balancer's synchronized list.
     * 
     * @param vm The VirtualMachine instance to register
     */
    public synchronized void addVM(VirtualMachine vm) {
        if (vm != null && !vmList.contains(vm)) {
            vmList.add(vm);
        }
    }

    /**
     * Removes a VM from the load balancer.
     * 
     * @param vmId ID of the VM to remove
     * @return true if removed, false otherwise
     */
    public synchronized boolean removeVM(String vmId) {
        if (vmId == null) return false;
        boolean removed = vmList.removeIf(vm -> vm.getVmId().equalsIgnoreCase(vmId));
        if (currentIndex >= vmList.size() && !vmList.isEmpty()) {
            currentIndex = 0;
        }
        return removed;
    }

    /**
     * Dispatches an incoming request to the next healthy VM using Round Robin.
     * 
     * Algorithm Details:
     * - Checks if the cluster is completely empty or all VMs are down.
     * - Iterates in circular order starting from `currentIndex`.
     * - Skips any VM where `isHealthy() == false`.
     * - Once a healthy VM is selected, assigns the request and advances the index.
     * - If a full circular scan completes without finding a healthy VM, throws NoHealthyVMException.
     * 
     * @return The VirtualMachine that accepted and handled the request
     * @throws NoHealthyVMException if no healthy VM is available
     */
    public synchronized VirtualMachine dispatchRequest() throws NoHealthyVMException {
        if (vmList.isEmpty()) {
            throw new NoHealthyVMException("Load Balancer has no virtual machines registered in the cluster.");
        }

        int totalVms = vmList.size();
        int attempts = 0;

        // Circular scan through the list starting at currentIndex
        while (attempts < totalVms) {
            // Select candidate VM at current index
            VirtualMachine candidate = vmList.get(currentIndex);

            // Advance pointer to the next VM in circular order
            currentIndex = (currentIndex + 1) % totalVms;
            attempts++;

            // If the candidate VM is healthy, dispatch the request to it
            if (candidate.isHealthy()) {
                candidate.assignRequest();
                totalDispatchedRequests++;
                return candidate;
            }
        }

        // If we completed a full circle and every single VM was unhealthy:
        throw new NoHealthyVMException(totalVms);
    }

    /**
     * Returns an unmodifiable snapshot of the VM list for monitoring.
     */
    public synchronized List<VirtualMachine> getVmList() {
        return Collections.unmodifiableList(new ArrayList<>(vmList));
    }

    /**
     * Finds a VM by its unique ID.
     */
    public synchronized VirtualMachine getVmById(String vmId) {
        for (VirtualMachine vm : vmList) {
            if (vm.getVmId().equalsIgnoreCase(vmId)) {
                return vm;
            }
        }
        return null;
    }

    /**
     * Returns count of currently registered VMs.
     */
    public synchronized int getClusterSize() {
        return vmList.size();
    }

    /**
     * Returns count of healthy VMs.
     */
    public synchronized int getHealthyCount() {
        int count = 0;
        for (VirtualMachine vm : vmList) {
            if (vm.isHealthy()) count++;
        }
        return count;
    }

    /**
     * Returns count of unhealthy VMs.
     */
    public synchronized int getUnhealthyCount() {
        return getClusterSize() - getHealthyCount();
    }

    /**
     * Returns total requests dispatched.
     */
    public synchronized long getTotalDispatchedRequests() {
        return totalDispatchedRequests;
    }

    /**
     * Returns current Round Robin index pointer.
     */
    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Clears all VMs from the load balancer.
     */
    public synchronized void clear() {
        vmList.clear();
        currentIndex = 0;
        totalDispatchedRequests = 0;
    }
}
