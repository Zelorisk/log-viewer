package dev.logviewer.client.gui;

import dev.logviewer.log.LogBuffer;
import dev.logviewer.log.LogEntry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.Level;
import org.joml.Matrix3x2fStack;

public class LogViewerHud {

    private static final LogViewerHud INSTANCE = new LogViewerHud();

    private static final int BG_COLOR = 0xE6101010;
    private static final int HEADER_COLOR = 0xF0181818;
    private static final int BORDER_COLOR = 0xFF2a2a2a;
    private static final int INFO_COLOR = 0xFFa0a0a0;
    private static final int WARN_COLOR = 0xFFd4a054;
    private static final int ERROR_COLOR = 0xFFc75050;
    private static final int DEBUG_COLOR = 0xFF6090b0;
    private static final int ACCENT_COLOR = 0xFFe0e0e0;
    private static final int DIM_TEXT = 0xFF505050;

    private static final float BASE_TEXT_SCALE = 0.5f;
    private static final int BASE_LINE_HEIGHT = 5;
    private static final int BASE_PADDING = 2;
    private static final int BASE_HEADER_HEIGHT = 7;
    private static final int BASE_MIN_WIDTH = 80;
    private static final int BASE_MIN_HEIGHT = 40;
    private static final int BASE_MAX_WIDTH = 400;
    private static final int BASE_MAX_HEIGHT = 300;
    private static final int BASE_WIDTH = 140;
    private static final int BASE_HEIGHT = 60;

    private boolean visible = false;
    private boolean mouseControlActive = false;

    private double windowXPercent = 0.01;
    private double windowYPercent = 0.01;
    private double windowWidthPercent = 0.33;
    private double windowHeightPercent = 0.25;

    private double scrollOffset = 0;
    private boolean autoScroll = true;

    private boolean showInfo = true;
    private boolean showWarn = true;
    private boolean showError = true;
    private boolean showDebug = true;

    private boolean isDragging = false;
    private boolean isResizing = false;
    private int dragOffsetX;
    private int dragOffsetY;

    private LogViewerHud() {}

    public static LogViewerHud getInstance() {
        return INSTANCE;
    }

    public void toggle() {
        visible = !visible;
        if (!visible) {
            mouseControlActive = false;
        }
    }

    public void toggleMouseControl() {
        if (visible) {
            mouseControlActive = !mouseControlActive;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isMouseControlActive() {
        return mouseControlActive;
    }

    public void render(
        GuiGraphics graphics,
        int screenWidth,
        int screenHeight
    ) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();

        double guiScale = mc.getWindow().getGuiScale();
        float scaleFactor = (float) (4.0 / guiScale);

        float textScale = BASE_TEXT_SCALE * scaleFactor;
        int lineHeight = Math.max(4, (int) (BASE_LINE_HEIGHT * scaleFactor));
        int padding = Math.max(1, (int) (BASE_PADDING * scaleFactor));
        int headerHeight = Math.max(
            6,
            (int) (BASE_HEADER_HEIGHT * scaleFactor)
        );
        int minWidth = (int) (BASE_MIN_WIDTH * scaleFactor);
        int minHeight = (int) (BASE_MIN_HEIGHT * scaleFactor);
        int maxWidth = (int) (BASE_MAX_WIDTH * scaleFactor);
        int maxHeight = (int) (BASE_MAX_HEIGHT * scaleFactor);

        int windowX = (int) (windowXPercent * screenWidth);
        int windowY = (int) (windowYPercent * screenHeight);
        int windowWidth = (int) (windowWidthPercent * screenWidth);
        int windowHeight = (int) (windowHeightPercent * screenHeight);

        windowWidth = Math.max(minWidth, Math.min(maxWidth, windowWidth));
        windowHeight = Math.max(minHeight, Math.min(maxHeight, windowHeight));
        windowX = Math.max(0, Math.min(windowX, screenWidth - windowWidth));
        windowY = Math.max(0, Math.min(windowY, screenHeight - windowHeight));

        graphics.fill(
            windowX,
            windowY,
            windowX + windowWidth,
            windowY + windowHeight,
            BG_COLOR
        );

        graphics.fill(
            windowX,
            windowY,
            windowX + windowWidth,
            windowY + headerHeight,
            HEADER_COLOR
        );

        drawBorder(graphics, windowX, windowY, windowWidth, windowHeight);

        Matrix3x2fStack pose = graphics.pose();

        pose.pushMatrix();
        pose.scale(textScale, textScale);

        float invScale = 1.0f / textScale;
        int scaledX = (int) ((windowX + padding) * invScale);
        int scaledY = (int) ((windowY + 1) * invScale);

        String title = mouseControlActive ? "LOG [K]" : "LOG";
        graphics.drawString(
            mc.font,
            title,
            scaledX,
            scaledY,
            ACCENT_COLOR,
            false
        );

        String filters = "";
        filters += showInfo ? "I" : "-";
        filters += showWarn ? "W" : "-";
        filters += showError ? "E" : "-";
        filters += showDebug ? "D" : "-";

        int filtersX = (int) ((windowX +
                windowWidth -
                padding -
                (int) (20 * scaleFactor)) *
            invScale);
        graphics.drawString(
            mc.font,
            filters,
            filtersX,
            scaledY,
            DIM_TEXT,
            false
        );

        String scrollIndicator = autoScroll ? "v" : "=";
        int scrollX = (int) ((windowX +
                windowWidth -
                padding -
                (int) (4 * scaleFactor)) *
            invScale);
        graphics.drawString(
            mc.font,
            scrollIndicator,
            scrollX,
            scaledY,
            autoScroll ? ACCENT_COLOR : DIM_TEXT,
            false
        );

        pose.popMatrix();

        List<LogEntry> entries = getFilteredEntries();
        int logAreaY = windowY + headerHeight + 2;
        int logAreaHeight = windowHeight - headerHeight - 4;
        int maxVisibleLines = logAreaHeight / lineHeight;

        if (autoScroll && !entries.isEmpty()) {
            scrollOffset = Math.max(0, entries.size() - maxVisibleLines);
        }

        int startIndex = (int) scrollOffset;
        int endIndex = Math.min(
            startIndex + maxVisibleLines + 1,
            entries.size()
        );

        graphics.enableScissor(
            windowX + 1,
            logAreaY,
            windowX + windowWidth - 1,
            windowY + windowHeight - 1
        );

        int maxChars = (int) ((windowWidth - padding * 2) / (6 * textScale));

        pose.pushMatrix();
        pose.scale(textScale, textScale);

        for (int i = startIndex; i < endIndex; i++) {
            LogEntry entry = entries.get(i);
            int y = logAreaY + (i - startIndex) * lineHeight;

            int color = getColorForLevel(entry.level());
            String text = formatLogLine(entry, maxChars);

            int textX = (int) ((windowX + padding) * invScale);
            int textY = (int) (y * invScale);
            graphics.drawString(mc.font, text, textX, textY, color, false);
        }

        pose.popMatrix();

        graphics.disableScissor();

        if (mouseControlActive) {
            int resizeSize = Math.max(4, (int) (8 * scaleFactor));
            graphics.fill(
                windowX + windowWidth - resizeSize,
                windowY + windowHeight - resizeSize,
                windowX + windowWidth,
                windowY + windowHeight,
                BORDER_COLOR
            );
        }
    }

    private String formatLogLine(LogEntry entry, int maxChars) {
        String levelChar = switch (entry.level().name()) {
            case "WARN" -> "W";
            case "ERROR", "FATAL" -> "E";
            case "DEBUG", "TRACE" -> "D";
            default -> "I";
        };

        String time = entry.getFormattedTime();
        String msg = entry.message();

        String line = time + " " + levelChar + " " + msg;
        if (line.length() > maxChars) {
            line = line.substring(0, maxChars - 2) + "..";
        }
        return line;
    }

    private void drawBorder(
        GuiGraphics graphics,
        int windowX,
        int windowY,
        int windowWidth,
        int windowHeight
    ) {
        graphics.fill(
            windowX,
            windowY,
            windowX + windowWidth,
            windowY + 1,
            BORDER_COLOR
        );
        graphics.fill(
            windowX,
            windowY + windowHeight - 1,
            windowX + windowWidth,
            windowY + windowHeight,
            BORDER_COLOR
        );
        graphics.fill(
            windowX,
            windowY,
            windowX + 1,
            windowY + windowHeight,
            BORDER_COLOR
        );
        graphics.fill(
            windowX + windowWidth - 1,
            windowY,
            windowX + windowWidth,
            windowY + windowHeight,
            BORDER_COLOR
        );
    }

    private int getColorForLevel(Level level) {
        return switch (level.name()) {
            case "WARN" -> WARN_COLOR;
            case "ERROR", "FATAL" -> ERROR_COLOR;
            case "DEBUG", "TRACE" -> DEBUG_COLOR;
            default -> INFO_COLOR;
        };
    }

    private List<LogEntry> getFilteredEntries() {
        return LogBuffer.getInstance().getFiltered(entry ->
            entry.matchesFilter("", showInfo, showWarn, showError, showDebug)
        );
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (!visible || !mouseControlActive) return false;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double guiScale = mc.getWindow().getGuiScale();
        float scaleFactor = (float) (4.0 / guiScale);

        int headerHeight = Math.max(
            6,
            (int) (BASE_HEADER_HEIGHT * scaleFactor)
        );
        int minWidth = (int) (BASE_MIN_WIDTH * scaleFactor);
        int minHeight = (int) (BASE_MIN_HEIGHT * scaleFactor);
        int maxWidth = (int) (BASE_MAX_WIDTH * scaleFactor);
        int maxHeight = (int) (BASE_MAX_HEIGHT * scaleFactor);

        int windowX = (int) (windowXPercent * screenWidth);
        int windowY = (int) (windowYPercent * screenHeight);
        int windowWidth = (int) (windowWidthPercent * screenWidth);
        int windowHeight = (int) (windowHeightPercent * screenHeight);
        windowWidth = Math.max(minWidth, Math.min(maxWidth, windowWidth));
        windowHeight = Math.max(minHeight, Math.min(maxHeight, windowHeight));

        if (button == 0) {
            int resizeSize = 12;
            if (
                mouseX >= windowX + windowWidth - resizeSize &&
                mouseX <= windowX + windowWidth &&
                mouseY >= windowY + windowHeight - resizeSize &&
                mouseY <= windowY + windowHeight
            ) {
                isResizing = true;
                return true;
            }

            if (
                mouseX >= windowX &&
                mouseX <= windowX + windowWidth &&
                mouseY >= windowY &&
                mouseY <= windowY + headerHeight
            ) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - windowX);
                dragOffsetY = (int) (mouseY - windowY);
                return true;
            }

            if (
                mouseX >= windowX &&
                mouseX <= windowX + windowWidth &&
                mouseY >= windowY &&
                mouseY <= windowY + windowHeight
            ) {
                return true;
            }
        }
        return false;
    }

    public boolean handleMouseRelease(
        double mouseX,
        double mouseY,
        int button
    ) {
        if (!visible || !mouseControlActive) return false;

        if (button == 0) {
            boolean wasInteracting = isDragging || isResizing;
            isDragging = false;
            isResizing = false;
            return wasInteracting;
        }
        return false;
    }

    public boolean handleMouseDrag(
        double mouseX,
        double mouseY,
        int button,
        double deltaX,
        double deltaY
    ) {
        if (!visible || !mouseControlActive) return false;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double guiScale = mc.getWindow().getGuiScale();
        float scaleFactor = (float) (4.0 / guiScale);

        int minWidth = (int) (BASE_MIN_WIDTH * scaleFactor);
        int minHeight = (int) (BASE_MIN_HEIGHT * scaleFactor);
        int maxWidth = (int) (BASE_MAX_WIDTH * scaleFactor);
        int maxHeight = (int) (BASE_MAX_HEIGHT * scaleFactor);

        if (isDragging) {
            int windowX = (int) (mouseX - dragOffsetX);
            int windowY = (int) (mouseY - dragOffsetY);
            windowXPercent = (double) windowX / screenWidth;
            windowYPercent = (double) windowY / screenHeight;
            return true;
        }

        if (isResizing) {
            int windowX = (int) (windowXPercent * screenWidth);
            int windowY = (int) (windowYPercent * screenHeight);
            int windowWidth = (int) Math.max(
                minWidth,
                Math.min(maxWidth, mouseX - windowX)
            );
            int windowHeight = (int) Math.max(
                minHeight,
                Math.min(maxHeight, mouseY - windowY)
            );
            windowWidthPercent = (double) windowWidth / screenWidth;
            windowHeightPercent = (double) windowHeight / screenHeight;
            return true;
        }

        return false;
    }

    public boolean handleMouseScroll(
        double mouseX,
        double mouseY,
        double amount
    ) {
        if (!visible) return false;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double guiScale = mc.getWindow().getGuiScale();
        float scaleFactor = (float) (4.0 / guiScale);

        int headerHeight = Math.max(
            6,
            (int) (BASE_HEADER_HEIGHT * scaleFactor)
        );
        int lineHeight = Math.max(4, (int) (BASE_LINE_HEIGHT * scaleFactor));
        int minWidth = (int) (BASE_MIN_WIDTH * scaleFactor);
        int minHeight = (int) (BASE_MIN_HEIGHT * scaleFactor);
        int maxWidth = (int) (BASE_MAX_WIDTH * scaleFactor);
        int maxHeight = (int) (BASE_MAX_HEIGHT * scaleFactor);

        int windowX = (int) (windowXPercent * screenWidth);
        int windowY = (int) (windowYPercent * screenHeight);
        int windowWidth = (int) (windowWidthPercent * screenWidth);
        int windowHeight = (int) (windowHeightPercent * screenHeight);
        windowWidth = Math.max(minWidth, Math.min(maxWidth, windowWidth));
        windowHeight = Math.max(minHeight, Math.min(maxHeight, windowHeight));

        if (
            mouseX >= windowX &&
            mouseX <= windowX + windowWidth &&
            mouseY >= windowY &&
            mouseY <= windowY + windowHeight
        ) {
            if (mouseControlActive) {
                autoScroll = false;
                scrollOffset = Math.max(0, scrollOffset - amount * 3);
                List<LogEntry> entries = getFilteredEntries();
                int logAreaHeight = windowHeight - headerHeight - 4;
                int maxVisibleLines = logAreaHeight / lineHeight;
                scrollOffset = Math.min(
                    scrollOffset,
                    Math.max(0, entries.size() - maxVisibleLines)
                );
            }
            return mouseControlActive;
        }
        return false;
    }

    public void clear() {
        LogBuffer.getInstance().clear();
    }

    public void toggleFilter(int index) {
        switch (index) {
            case 0 -> showInfo = !showInfo;
            case 1 -> showWarn = !showWarn;
            case 2 -> showError = !showError;
            case 3 -> showDebug = !showDebug;
        }
    }
}
