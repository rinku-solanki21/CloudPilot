package com.cloudpilot.controller;

import com.cloudpilot.model.VirtualMachine;
import com.cloudpilot.service.ActivityLogger;
import com.cloudpilot.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * CloudPilotController
 * 
 * Exposes Spring Boot REST API endpoints for communicating with the frontend
 * cloud management dashboard.
 * 
 * Provides endpoints for live telemetry, request dispatching, fault injection,
 * and autonomous scaling controls.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows local cross-origin development if needed
public class CloudPilotController {

    private final SimulationService simulationService;

    public CloudPilotController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * GET /api/dashboard
     * Returns consolidated telemetry for the entire cluster.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        return ResponseEntity.ok(simulationService.getDashboardSummary());
    }

    /**
     * GET /api/vms
     * Returns full status and statistics for all registered virtual machines.
     */
    @GetMapping("/vms")
    public ResponseEntity<List<VirtualMachine>> getAllVirtualMachines() {
        return ResponseEntity.ok(simulationService.getAllVms());
    }

    /**
     * POST /api/requests
     * Dispatches a single client request using Round Robin load balancing.
     */
    @PostMapping("/requests")
    public ResponseEntity<Map<String, Object>> sendSingleRequest() {
        return ResponseEntity.ok(simulationService.sendSingleRequest());
    }

    /**
     * POST /api/requests/batch
     * Dispatches a batch of requests across the cluster (default: 10).
     */
    @PostMapping("/requests/batch")
    public ResponseEntity<Map<String, Object>> sendBatchRequests(
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(simulationService.sendBatchRequests(count));
    }

    /**
     * POST /api/simulation/surge
     * Triggers sudden traffic surge pushing cluster CPU above 80% to demonstrate auto-scaling.
     */
    @PostMapping("/simulation/surge")
    public ResponseEntity<Map<String, Object>> simulateTrafficSurge() {
        return ResponseEntity.ok(simulationService.simulateTrafficSurge());
    }

    /**
     * POST /api/vms/{vmId}/unhealthy
     * Marks a specific VM as UNHEALTHY to test fault tolerance and failover.
     */
    @PostMapping("/vms/{vmId}/unhealthy")
    public ResponseEntity<Map<String, Object>> markVmUnhealthy(@PathVariable String vmId) {
        return ResponseEntity.ok(simulationService.markVmUnhealthy(vmId));
    }

    /**
     * POST /api/vms/{vmId}/heal
     * Restores a previously unhealthy VM back into rotation.
     */
    @PostMapping("/vms/{vmId}/heal")
    public ResponseEntity<Map<String, Object>> markVmHealthy(@PathVariable String vmId) {
        return ResponseEntity.ok(simulationService.markVmHealthy(vmId));
    }

    /**
     * POST /api/simulation/reset
     * Resets cluster state back to initial 3 VMs with baseline stats.
     */
    @PostMapping("/simulation/reset")
    public ResponseEntity<Map<String, Object>> resetSimulation() {
        return ResponseEntity.ok(simulationService.resetSimulation());
    }

    /**
     * POST /api/monitoring/pause
     * Pauses the background AutoScaler monitoring thread.
     */
    @PostMapping("/monitoring/pause")
    public ResponseEntity<Map<String, String>> pauseMonitoring() {
        simulationService.pauseMonitoring();
        return ResponseEntity.ok(Map.of("status", "PAUSED", "message", "Auto-scaler monitoring paused."));
    }

    /**
     * POST /api/monitoring/resume
     * Resumes the background AutoScaler monitoring thread.
     */
    @PostMapping("/monitoring/resume")
    public ResponseEntity<Map<String, String>> resumeMonitoring() {
        simulationService.resumeMonitoring();
        return ResponseEntity.ok(Map.of("status", "RESUMED", "message", "Auto-scaler monitoring resumed."));
    }

    /**
     * GET /api/logs
     * Returns recent activity logs recorded across the cluster.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<ActivityLogger.LogEntry>> getActivityLogs() {
        return ResponseEntity.ok(simulationService.getActivityLogs());
    }
}
