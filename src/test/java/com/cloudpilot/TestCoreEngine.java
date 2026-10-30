package com.cloudpilot;

import com.cloudpilot.autoscaler.AutoScalerThread;
import com.cloudpilot.balancer.LoadBalancer;
import com.cloudpilot.exceptions.MaxClusterCapacityException;
import com.cloudpilot.exceptions.NoHealthyVMException;
import com.cloudpilot.model.VirtualMachine;
import com.cloudpilot.pool.ResourcePool;
import com.cloudpilot.service.ActivityLogger;

/**
 * TestCoreEngine
 * 
 * Standalone verification harness testing core Java OOP requirements:
 * 1. VirtualMachine state and encapsulation
 * 2. Generic ResourcePool<T> lifecycle
 * 3. Round Robin LoadBalancer algorithm and failover
 * 4. AutoScalerThread threshold evaluation and provisioning
 * 5. Custom exceptions (MaxClusterCapacityException, NoHealthyVMException)
 */
public class TestCoreEngine {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  CloudPilot - Core OOP Engine Verification Suite");
        System.out.println("=================================================");

        int passed = 0;
        int total = 5;

        // Test 1: ResourcePool and VM Creation
        try {
            System.out.println("\n[Test 1] Testing Generic ResourcePool<VirtualMachine>...");
            ResourcePool<VirtualMachine> pool = new ResourcePool<>(5);
            VirtualMachine vm1 = new VirtualMachine("VM-01", 40.0, 512, false);
            VirtualMachine vm2 = new VirtualMachine("VM-02", 40.0, 512, false);
            VirtualMachine vm3 = new VirtualMachine("VM-03", 40.0, 512, false);

            pool.addResource(vm1);
            pool.addResource(vm2);
            pool.addResource(vm3);

            assert pool.size() == 3 : "Pool should contain 3 VMs";
            assert pool.getAvailableCount() == 3 : "All 3 should be available";

            VirtualMachine acquired = pool.acquireResource();
            assert acquired.getVmId().equals("VM-01") : "Should acquire VM-01 first (FIFO)";
            assert pool.getActiveCount() == 1 : "Active count should be 1";

            pool.releaseResource(acquired);
            assert pool.getActiveCount() == 0 : "Active count should be 0 after release";

            System.out.println("  --> PASSED: Generic ResourcePool operations verified.");
            passed++;
        } catch (Exception e) {
            System.err.println("  --> FAILED: " + e.getMessage());
        }

        // Test 2: Round Robin Load Balancing
        try {
            System.out.println("\n[Test 2] Testing Round Robin LoadBalancer...");
            LoadBalancer lb = new LoadBalancer();
            VirtualMachine vm1 = new VirtualMachine("VM-01", 40.0, 512, false);
            VirtualMachine vm2 = new VirtualMachine("VM-02", 40.0, 512, false);
            VirtualMachine vm3 = new VirtualMachine("VM-03", 40.0, 512, false);

            lb.addVM(vm1);
            lb.addVM(vm2);
            lb.addVM(vm3);

            VirtualMachine r1 = lb.dispatchRequest();
            VirtualMachine r2 = lb.dispatchRequest();
            VirtualMachine r3 = lb.dispatchRequest();
            VirtualMachine r4 = lb.dispatchRequest();

            assert r1.getVmId().equals("VM-01") : "Request 1 should go to VM-01";
            assert r2.getVmId().equals("VM-02") : "Request 2 should go to VM-02";
            assert r3.getVmId().equals("VM-03") : "Request 3 should go to VM-03";
            assert r4.getVmId().equals("VM-01") : "Request 4 should wrap around to VM-01";

            System.out.println("  --> PASSED: Round Robin sequencing verified (VM-01 -> VM-02 -> VM-03 -> VM-01).");
            passed++;
        } catch (Exception e) {
            System.err.println("  --> FAILED: " + e.getMessage());
        }

        // Test 3: Unhealthy VM Skipping
        try {
            System.out.println("\n[Test 3] Testing Fault Tolerance & Unhealthy VM Skipping...");
            LoadBalancer lb = new LoadBalancer();
            VirtualMachine vm1 = new VirtualMachine("VM-01", 40.0, 512, false);
            VirtualMachine vm2 = new VirtualMachine("VM-02", 40.0, 512, false);
            VirtualMachine vm3 = new VirtualMachine("VM-03", 40.0, 512, false);

            lb.addVM(vm1);
            lb.addVM(vm2);
            lb.addVM(vm3);

            // Mark VM-02 UNHEALTHY
            vm2.setHealthy(false);

            VirtualMachine d1 = lb.dispatchRequest(); // VM-01
            VirtualMachine d2 = lb.dispatchRequest(); // skips VM-02, goes to VM-03
            VirtualMachine d3 = lb.dispatchRequest(); // VM-01

            assert d1.getVmId().equals("VM-01");
            assert d2.getVmId().equals("VM-03") : "Load balancer must skip unhealthy VM-02 and select VM-03";
            assert d3.getVmId().equals("VM-01");

            System.out.println("  --> PASSED: Unhealthy VM-02 successfully skipped.");
            passed++;
        } catch (Exception e) {
            System.err.println("  --> FAILED: " + e.getMessage());
        }

        // Test 4: NoHealthyVMException
        try {
            System.out.println("\n[Test 4] Testing NoHealthyVMException handling...");
            LoadBalancer lb = new LoadBalancer();
            VirtualMachine vm1 = new VirtualMachine("VM-01", 40.0, 512, false);
            lb.addVM(vm1);
            vm1.setHealthy(false); // All VMs down

            boolean caught = false;
            try {
                lb.dispatchRequest();
            } catch (NoHealthyVMException ex) {
                caught = true;
                System.out.println("  --> Caught expected exception: " + ex.getMessage());
            }

            assert caught : "NoHealthyVMException must be thrown when all VMs are down";
            System.out.println("  --> PASSED: NoHealthyVMException verified.");
            passed++;
        } catch (Exception e) {
            System.err.println("  --> FAILED: " + e.getMessage());
        }

        // Test 5: Auto-Scaling and MaxClusterCapacityException
        try {
            System.out.println("\n[Test 5] Testing AutoScaler provisioning and MaxClusterCapacityException...");
            ResourcePool<VirtualMachine> pool = new ResourcePool<>(4);
            LoadBalancer lb = new LoadBalancer();
            ActivityLogger logger = new ActivityLogger(10);
            AutoScalerThread scaler = new AutoScalerThread(pool, lb, logger, 4);

            // Add 3 baseline VMs
            for (int i = 1; i <= 3; i++) {
                VirtualMachine vm = new VirtualMachine("VM-0" + i, 85.0, 512, false);
                pool.addResource(vm);
                lb.addVM(vm);
            }

            // Provision 4th VM (reaches max capacity 4)
            VirtualMachine vm4 = scaler.scaleOut(85.0);
            assert vm4.getVmId().equals("VM-04") : "New VM should be VM-04";
            assert lb.getClusterSize() == 4 : "Cluster size should now be 4";

            // Attempt to provision 5th VM (must throw MaxClusterCapacityException)
            boolean caughtCap = false;
            try {
                scaler.scaleOut(90.0);
            } catch (MaxClusterCapacityException ex) {
                caughtCap = true;
                System.out.println("  --> Caught expected exception: " + ex.getMessage());
            }

            assert caughtCap : "MaxClusterCapacityException must be thrown when exceeding capacity";
            System.out.println("  --> PASSED: AutoScaler dynamic provisioning & MaxClusterCapacityException verified.");
            passed++;
        } catch (Exception e) {
            System.err.println("  --> FAILED: " + e.getMessage());
        }

        System.out.println("\n=================================================");
        System.out.printf("  Verification Summary: %d/%d Tests Passed\n", passed, total);
        System.out.println("=================================================");
        if (passed != total) {
            System.exit(1);
        }
    }
}
