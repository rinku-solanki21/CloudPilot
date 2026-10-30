/**
 * CloudPilot - Telemetry Chart Visualizations (Chart.js)
 * 
 * Manages:
 * 1. CPU Utilization Trend Line Chart with 80% threshold line.
 * 2. Round Robin Request Distribution Bar Chart.
 */

let cpuTrendChart = null;
let distributionChart = null;

const MAX_HISTORY_POINTS = 15;
const cpuHistoryLabels = [];
const cpuHistoryData = [];

/**
 * Initializes all dashboard charts.
 */
function initCharts() {
    if (typeof Chart === 'undefined') {
        console.warn("Chart.js not loaded. Visualizations will be disabled.");
        return;
    }

    // Default global Chart.js styling for dark theme
    Chart.defaults.color = '#94a3b8';
    Chart.defaults.font.family = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';

    initCpuTrendChart();
    initDistributionChart();
}

/**
 * Line chart for real-time CPU utilization trend.
 */
function initCpuTrendChart() {
    const ctx = document.getElementById('cpuTrendChart');
    if (!ctx) return;

    // Pre-populate with initial points
    const now = new Date();
    for (let i = 6; i >= 0; i--) {
        const timeStr = new Date(now.getTime() - i * 3000).toTimeString().split(' ')[0];
        cpuHistoryLabels.push(timeStr);
        cpuHistoryData.push(40.0);
    }

    cpuTrendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: cpuHistoryLabels,
            datasets: [
                {
                    label: 'Average Cluster CPU (%)',
                    data: cpuHistoryData,
                    borderColor: '#00d2ff',
                    backgroundColor: 'rgba(0, 210, 255, 0.12)',
                    borderWidth: 2.5,
                    fill: true,
                    tension: 0.35,
                    pointRadius: 3,
                    pointHoverRadius: 6,
                    pointBackgroundColor: '#00d2ff'
                },
                {
                    label: 'Scaling Threshold (80%)',
                    data: Array(cpuHistoryLabels.length).fill(80),
                    borderColor: '#ef4444',
                    borderWidth: 1.5,
                    borderDash: [6, 6],
                    fill: false,
                    pointRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 400 },
            scales: {
                y: {
                    min: 0,
                    max: 100,
                    grid: { color: 'rgba(255, 255, 255, 0.06)' },
                    ticks: {
                        callback: val => val + '%'
                    }
                },
                x: {
                    grid: { display: false }
                }
            },
            plugins: {
                legend: {
                    position: 'top',
                    labels: { boxWidth: 14, font: { size: 11 } }
                },
                tooltip: {
                    backgroundColor: '#162036',
                    borderColor: 'rgba(255, 255, 255, 0.15)',
                    borderWidth: 1,
                    titleColor: '#f8fafc',
                    bodyColor: '#00d2ff'
                }
            }
        }
    });
}

/**
 * Bar chart for Round Robin request distribution per VM.
 */
function initDistributionChart() {
    const ctx = document.getElementById('distributionChart');
    if (!ctx) return;

    distributionChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['VM-01', 'VM-02', 'VM-03'],
            datasets: [{
                label: 'Requests Handled',
                data: [0, 0, 0],
                backgroundColor: [
                    'rgba(56, 189, 248, 0.65)',
                    'rgba(167, 139, 250, 0.65)',
                    'rgba(52, 211, 153, 0.65)',
                    'rgba(244, 114, 182, 0.65)',
                    'rgba(251, 191, 36, 0.65)',
                    'rgba(96, 165, 250, 0.65)',
                    'rgba(192, 132, 252, 0.65)',
                    'rgba(45, 212, 191, 0.65)'
                ],
                borderColor: [
                    '#38bdf8', '#a78bfa', '#34d399', '#f472b6',
                    '#fbbf24', '#60a5fa', '#c084fc', '#2dd4bf'
                ],
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 1 },
                    grid: { color: 'rgba(255, 255, 255, 0.06)' }
                },
                x: {
                    grid: { display: false }
                }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });
}

/**
 * Adds a new CPU data point to the trend chart.
 */
function updateCpuChart(timestamp, cpuValue) {
    if (!cpuTrendChart) return;

    cpuHistoryLabels.push(timestamp);
    cpuHistoryData.push(cpuValue);

    if (cpuHistoryLabels.length > MAX_HISTORY_POINTS) {
        cpuHistoryLabels.shift();
        cpuHistoryData.shift();
    }

    // Keep threshold dataset aligned
    cpuTrendChart.data.datasets[1].data = Array(cpuHistoryLabels.length).fill(80);

    cpuTrendChart.update();
}

/**
 * Updates the bar chart with current VM request counts.
 */
function updateDistributionChart(vms) {
    if (!distributionChart || !Array.isArray(vms)) return;

    const labels = vms.map(vm => vm.vmId);
    const data = vms.map(vm => vm.totalRequestsHandled || 0);

    distributionChart.data.labels = labels;
    distributionChart.data.datasets[0].data = data;
    distributionChart.update();
}
