package dev.logviewer.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LogCapture extends AbstractAppender {
    private static LogCapture instance;
    private static boolean installed = false;

    private LogCapture() {
        super("LogViewerCapture", null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
    }

    public static void install() {
        if (installed) return;

        instance = new LogCapture();
        instance.start();

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(instance);
        installed = true;
    }

    public static void uninstall() {
        if (!installed || instance == null) return;

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(instance);
        instance.stop();
        installed = false;
    }

    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        Level level = event.getLevel();
        String loggerName = event.getLoggerName();
        String threadName = event.getThreadName();
        long timestamp = event.getTimeMillis();

        LogEntry entry = new LogEntry(timestamp, level, loggerName, message, threadName);
        LogBuffer.getInstance().add(entry);
    }
}
