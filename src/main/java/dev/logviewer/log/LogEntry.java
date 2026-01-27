package dev.logviewer.log;

import org.apache.logging.log4j.Level;

public record LogEntry(
    long timestamp,
    Level level,
    String loggerName,
    String message,
    String threadName
) {
    public String getShortLoggerName() {
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }

    public String getFormattedTime() {
        long seconds = timestamp / 1000;
        long hours = (seconds / 3600) % 24;
        long minutes = (seconds / 60) % 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public String getFormattedMessage() {
        return String.format("[%s] [%s/%s] %s",
            getFormattedTime(),
            getShortLoggerName(),
            level.name(),
            message
        );
    }

    public boolean matchesFilter(String filter, boolean showInfo, boolean showWarn, boolean showError, boolean showDebug) {
        boolean levelMatch = switch (level.name()) {
            case "INFO" -> showInfo;
            case "WARN" -> showWarn;
            case "ERROR" -> showError;
            case "DEBUG" -> showDebug;
            default -> showInfo;
        };

        if (!levelMatch) return false;

        if (filter == null || filter.isEmpty()) return true;

        String lowerFilter = filter.toLowerCase();
        return message.toLowerCase().contains(lowerFilter) ||
               loggerName.toLowerCase().contains(lowerFilter);
    }
}
