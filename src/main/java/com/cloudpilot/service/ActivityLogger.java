package com.cloudpilot.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * ActivityLogger
 * 
 * Thread-safe event logging buffer that retains recent cluster, load balancer,
 * and auto-scaler events for display on the CloudPilot dashboard.
 */
public class ActivityLogger {

    public enum LogType {
        INFO, SUCCESS, WARNING, ALERT, SCALED
    }

    public static class LogEntry {
        private final String timestamp;
        private final LogType type;
        private final String message;

        public LogEntry(LogType type, String message) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            this.type = type;
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public LogType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    private final LinkedList<LogEntry> logs = new LinkedList<>();
    private final int maxLogs;

    public ActivityLogger(int maxLogs) {
        this.maxLogs = maxLogs;
    }

    /**
     * Records a new log entry.
     */
    public synchronized void log(LogType type, String message) {
        if (logs.size() >= maxLogs) {
            logs.removeLast();
        }
        logs.addFirst(new LogEntry(type, message));
    }

    public void info(String message) {
        log(LogType.INFO, message);
    }

    public void success(String message) {
        log(LogType.SUCCESS, message);
    }

    public void warn(String message) {
        log(LogType.WARNING, message);
    }

    public void alert(String message) {
        log(LogType.ALERT, message);
    }

    public void scaled(String message) {
        log(LogType.SCALED, message);
    }

    /**
     * Returns an unmodifiable snapshot of recent logs.
     */
    public synchronized List<LogEntry> getRecentLogs() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }

    /**
     * Clears all logs.
     */
    public synchronized void clear() {
        logs.clear();
    }
}
