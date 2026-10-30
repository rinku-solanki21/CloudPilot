/**
 * CloudPilot - Frontend REST API Controller & Dashboard Client
 * 
 * B.Tech 3rd Semester OOP Academic Project
 * Connects frontend dashboard controls to Spring Boot REST endpoints.
 */

// Global State
let pollingInterval = null;
let isAutoScalerPaused = false;
let currentVms = [];

// =========================================================================
// Initialization
// =========================================================================

document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Chart.js visualizers
    initCharts();

    // 2. Wire up all button event handlers
    setupEventHandlers();

    // 3. Initial data fetch
    fetchClusterData();

    // 4. Start live telemetry polling loop (every 2.0 seconds)
    startPollingLoop(2000);
});

// =========================================================================
// Event Listeners Setup
// =========================================================================

function setupEventHandlers() {
    // Send 1 Request
    document.getElementById('btnSendOne').addEventListener('click', async () => {
        try {
            const res = await apiPost('/api/requests');
            showToast(`Request dispatched to ${res.handledBy} (CPU: ${res.vmCpu}%)`, 'success');
            fetchClusterData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Send 10 Requests Batch
    document.getElementById('btnSendTen').addEventListener('click', async () => {
        try {
            const res = await apiPost('/api/requests/batch?count=10');
            const distStr = Object.entries(res.distribution || {})
                .map(([vm, count]) => `${vm}: ${count}`)
                .join(', ');
            showToast(`Batch of 10 requests distributed: ${distStr}`, 'success');
            fetchClusterData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Simulate Traffic Surge
    document.getElementById('btnSurge').addEventListener('click', async () => {
        try {
            showToast('Simulating traffic surge... CPU spiking above 80%!', 'warning');
            const res = await apiPost('/api/simulation/surge');
            fetchClusterData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Dropdown Toggle Health Button
    document.getElementById('btnToggleHealth').addEventListener('click', async () => {
        const select = document.getElementById('selectVmFault');
        const vmId = select.value;
        if (!vmId) {
            showToast('Please select a Virtual Machine first.', 'warning');
            return;
        }

        const vm = currentVms.find(v => v.vmId === vmId);
        if (!vm) return;

        try {
            if (vm.healthy) {
                await apiPost(`/api/vms/${vmId}/unhealthy`);
                showToast(`${vmId} marked UNHEALTHY (Round Robin will skip it).`, 'warning');
            } else {
                await apiPost(`/api/vms/${vmId}/heal`);
                showToast(`${vmId} restored to HEALTHY.`, 'success');
            }
            fetchClusterData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Pause / Resume AutoScaler Monitoring
    document.getElementById('btnToggleMonitoring').addEventListener('click', async () => {
        try {
            if (isAutoScalerPaused) {
                await apiPost('/api/monitoring/resume');
                showToast('AutoScaler monitoring thread resumed.', 'success');
            } else {
                await apiPost('/api/monitoring/pause');
                showToast('AutoScaler monitoring thread paused.', 'warning');
            }
            fetchClusterData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Reset Simulation
    document.getElementById('btnReset').addEventListener('click', async () => {
        if (!confirm('Reset simulation back to 3 healthy virtual machines?')) {
            return;
        }
        try {
            await apiPost('/api/simulation/reset');
            showToast('Simulation reset to baseline (3 healthy VMs).', 'success');
            fetchClusterData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });
}

// =========================================================================
// API Communication Helpers
// =========================================================================

async function apiGet(endpoint) {
    const response = await fetch(endpoint);
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.message || `API error (${response.status})`);
    }
    return data;
}

async function apiPost(endpoint) {
    const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    });
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.message || `API error (${response.status})`);
    }
    return data;
}

// =========================================================================
// Real-time Data Sync & Rendering
// =========================================================================

async function fetchClusterData() {
    try {
        const [dashboard, vms, logs] = await Promise.all([
            apiGet('/api/dashboard'),
            apiGet('/api/vms'),
            apiGet('/api/logs')
        ]);

        currentVms = vms;
        updateDashboardKPIs(dashboard);
        renderVmCards(vms, dashboard.roundRobinPointer);
        updateVmDropdown(vms);
        renderActivityLogs(logs);

        // Update charts
        const timeStr = new Date().toTimeString().split(' ')[0];
        updateCpuChart(timeStr, dashboard.averageCpu);
        updateDistributionChart(vms);

        setSystemOnline(true);
    } catch (err) {
        console.error("Telemetry sync error:", err);
        setSystemOnline(false);
    }
}

function startPollingLoop(intervalMs) {
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(fetchClusterData, intervalMs);
}

// =========================================================================
// DOM Renderers
// =========================================================================

function updateDashboardKPIs(data) {
    document.getElementById('kpiTotalVms').textContent = data.totalVms;
    document.getElementById('kpiHealthyVms').textContent = data.healthyVms;
    document.getElementById('kpiUnhealthyVms').textContent = data.unhealthyVms;
    document.getElementById('kpiAvgCpu').textContent = `${data.averageCpu}%`;
    document.getElementById('kpiActiveConnections').textContent = data.activeConnections;
    document.getElementById('kpiTotalRequests').textContent = data.totalRequestsHandled || data.totalDispatched || 0;
    document.getElementById('capacityRatio').textContent = `${data.totalVms} / ${data.maxClusterCapacity} VMs`;

    // Highlight high average CPU
    const cpuEl = document.getElementById('kpiAvgCpu');
    if (data.averageCpu >= 80.0) {
        cpuEl.style.color = 'var(--danger)';
    } else if (data.averageCpu >= 60.0) {
        cpuEl.style.color = 'var(--warning)';
    } else {
        cpuEl.style.color = 'var(--text-primary)';
    }

    // AutoScaler State
    isAutoScalerPaused = data.autoScalerPaused;
    const pill = document.getElementById('autoScalerPill');
    const pillText = document.getElementById('autoScalerStatusText');
    const btnText = document.getElementById('textPauseResume');

    if (data.autoScalerPaused) {
        pill.classList.add('paused');
        pillText.textContent = 'AUTOSCALER: PAUSED';
        btnText.textContent = 'Resume Monitoring';
    } else {
        pill.classList.remove('paused');
        pillText.textContent = 'AUTOSCALER: RUNNING';
        btnText.textContent = 'Pause Monitoring';
    }
}

function renderVmCards(vms, roundRobinPointer) {
    const container = document.getElementById('vmGridContainer');
    const badge = document.getElementById('vmCountBadge');
    if (badge) badge.textContent = `${vms.length} Instances`;

    // Determine next target VM for Round Robin
    if (vms.length > 0 && typeof roundRobinPointer === 'number') {
        const nextVm = vms[roundRobinPointer % vms.length];
        const nextEl = document.getElementById('nextTargetVm');
        if (nextEl && nextVm) {
            nextEl.textContent = `${nextVm.vmId} ${nextVm.healthy ? '' : '(UNHEALTHY)'}`;
        }
    }

    container.innerHTML = vms.map(vm => {
        const isHealthy = vm.healthy;
        const isScaled = vm.autoScaled;
        const cpu = vm.cpuUtilizationPercentage || 0;
        const mem = vm.memoryUsedMB || 512;
        const totalMem = vm.totalMemoryMB || 2048;
        const memPercent = Math.round((mem / totalMem) * 100);
        const isHighCpu = cpu >= 80.0;

        return `
            <div class="vm-card ${isScaled ? 'scaled-vm' : ''} ${!isHealthy ? 'unhealthy-vm' : ''}">
                <div class="vm-card-top">
                    <div class="vm-card-id">
                        <h4>${vm.vmId}</h4>
                        ${isScaled ? '<span class="badge-scaled">AUTOSCALED</span>' : ''}
                    </div>
                    <span class="vm-status-badge ${isHealthy ? 'status-healthy' : 'status-unhealthy'}">
                        ${isHealthy ? 'HEALTHY' : 'UNHEALTHY'}
                    </span>
                </div>

                <!-- CPU Meter -->
                <div class="meter-block">
                    <div class="meter-header">
                        <span>CPU Utilization</span>
                        <strong>${cpu}%</strong>
                    </div>
                    <div class="progress-track">
                        <div class="progress-bar progress-cpu ${isHighCpu ? 'high-load' : ''}" style="width: ${cpu}%"></div>
                    </div>
                </div>

                <!-- Memory Meter -->
                <div class="meter-block">
                    <div class="meter-header">
                        <span>Memory (RAM)</span>
                        <strong>${mem} / ${totalMem} MB (${memPercent}%)</strong>
                    </div>
                    <div class="progress-track">
                        <div class="progress-bar progress-mem" style="width: ${memPercent}%"></div>
                    </div>
                </div>

                <!-- Metrics Row -->
                <div class="vm-metrics-row">
                    <div class="metric-item">
                        <span>Active Conns</span>
                        <strong>${vm.activeConnections || 0}</strong>
                    </div>
                    <div class="metric-item">
                        <span>Total Requests</span>
                        <strong>${vm.totalRequestsHandled || 0}</strong>
                    </div>
                    <div class="metric-item">
                        <span>Created At</span>
                        <strong>${vm.creationTime || '00:00:00'}</strong>
                    </div>
                </div>

                <!-- Action Button -->
                <div class="vm-card-actions">
                    <button class="btn ${isHealthy ? 'btn-danger' : 'btn-primary'}" onclick="toggleSingleVmHealth('${vm.vmId}', ${isHealthy})">
                        ${isHealthy ? 'Mark Unhealthy' : 'Restore Healthy'}
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function updateVmDropdown(vms) {
    const select = document.getElementById('selectVmFault');
    const currentValue = select.value;

    select.innerHTML = '<option value="">Select VM for Health Toggle...</option>' +
        vms.map(vm => `<option value="${vm.vmId}" ${vm.vmId === currentValue ? 'selected' : ''}>${vm.vmId} (${vm.healthy ? 'Healthy' : 'Unhealthy'})</option>`).join('');
}

function renderActivityLogs(logs) {
    const consoleEl = document.getElementById('activityLogConsole');
    const countEl = document.getElementById('logCountText');
    if (countEl) countEl.textContent = `${logs.length} events recorded`;

    if (!logs || logs.length === 0) {
        consoleEl.innerHTML = `
            <div class="console-line info">
                <span class="timestamp">[--:--:--]</span>
                <span class="tag tag-info">INFO</span>
                <span class="msg">Awaiting cluster simulation events...</span>
            </div>
        `;
        return;
    }

    consoleEl.innerHTML = logs.map(entry => {
        const typeClass = (entry.type || 'INFO').toLowerCase();
        return `
            <div class="console-line ${typeClass}">
                <span class="timestamp">[${entry.timestamp}]</span>
                <span class="tag tag-${typeClass}">${entry.type}</span>
                <span class="msg">${escapeHtml(entry.message)}</span>
            </div>
        `;
    }).join('');
}

// Global toggle invoked from card buttons
window.toggleSingleVmHealth = async function(vmId, currentIsHealthy) {
    try {
        if (currentIsHealthy) {
            await apiPost(`/api/vms/${vmId}/unhealthy`);
            showToast(`${vmId} marked UNHEALTHY.`, 'warning');
        } else {
            await apiPost(`/api/vms/${vmId}/heal`);
            showToast(`${vmId} restored to HEALTHY.`, 'success');
        }
        fetchClusterData();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

// =========================================================================
// Utilities & UI Feedback
// =========================================================================

function setSystemOnline(isOnline) {
    const pulse = document.getElementById('systemPulse');
    const statusText = document.getElementById('systemStatusText');
    if (pulse && statusText) {
        if (isOnline) {
            pulse.style.backgroundColor = 'var(--success)';
            pulse.style.boxShadow = '0 0 10px var(--success)';
            statusText.textContent = 'SYSTEM ACTIVE';
        } else {
            pulse.style.backgroundColor = 'var(--danger)';
            pulse.style.boxShadow = '0 0 10px var(--danger)';
            statusText.textContent = 'OFFLINE / DISCONNECTED';
        }
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 400);
    }, 4500);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/"/g, "&quot;")
              .replace(/'/g, "&#039;");
}
