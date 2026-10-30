package com.cloudpilot;

import com.cloudpilot.balancer.LoadBalancer;
import com.cloudpilot.model.VirtualMachine;
import com.cloudpilot.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CloudPilotApplicationTests {

    @Autowired
    private SimulationService simulationService;

    @Test
    void contextLoads() {
        assertNotNull(simulationService, "SimulationService should be injected successfully");
    }

    @Test
    void testInitialClusterConfiguration() {
        Map<String, Object> summary = simulationService.getDashboardSummary();
        assertNotNull(summary);
        assertEquals(3, summary.get("totalVms"), "Should start with 3 baseline VMs");
        assertEquals(3, summary.get("healthyVms"), "All 3 baseline VMs should start healthy");
        assertEquals(0, summary.get("unhealthyVms"), "Zero unhealthy VMs initially");
    }

    @Test
    void testRequestDispatching() {
        Map<String, Object> result = simulationService.sendSingleRequest();
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("handledBy"));
    }

    @Test
    void testBatchRequests() {
        Map<String, Object> result = simulationService.sendBatchRequests(6);
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals(6, result.get("batchCount"));
    }

    @Test
    void testHealthToggle() {
        simulationService.markVmUnhealthy("VM-01");
        Map<String, Object> summary = simulationService.getDashboardSummary();
        assertEquals(1, summary.get("unhealthyVms"));

        simulationService.markVmHealthy("VM-01");
        summary = simulationService.getDashboardSummary();
        assertEquals(0, summary.get("unhealthyVms"));
    }
}
