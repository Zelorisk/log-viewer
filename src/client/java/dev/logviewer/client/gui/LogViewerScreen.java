package dev.logviewer.client.gui;

import dev.logviewer.log.LogBuffer;
import dev.logviewer.log.LogEntry;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Level;
import org.lwjgl.glfw.GLFW;

public class LogViewerScreen extends Screen {

    private static final int BG_COLOR = 0xD91a1a1a;
    private static final int HEADER_COLOR = 0xFF0d0d0d;
    private static final int BORDER_COLOR = 0xFF00ff88;
    private static final int INFO_COLOR = 0xFFcccccc;
    private static final int WARN_COLOR = 0xFFffaa00;
    private static final int ERROR_COLOR = 0xFFff4444;
    private static final int DEBUG_COLOR = 0xFF66ccff;
    private static final int ACCENT_COLOR = 0xFF00ff88;

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 30;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;

    private double scrollOffset = 0;
    private boolean autoScroll = true;
    private String filterText = "";

    private boolean showInfo = true;
    private boolean showWarn = true;
    private boolean showError = true;
    private boolean showDebug = true;

    private EditBox searchBox;
    private Button infoButton;
    private Button warnButton;
    private Button errorButton;
    private Button debugButton;
    private Button clearButton;
    private Button exportButton;
    private Button autoScrollButton;

    private boolean isDragging = false;
    private boolean isResizing = false;
    private int dragOffsetX;
    private int dragOffsetY;

    public LogViewerScreen() {
        super(Component.translatable("logviewer.screen.title"));
    }

    @Override
    protected void init() {
        windowWidth = Math.min(width - 40, 900);
        windowHeight = Math.min(height - 40, 600);
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;

        int buttonY = windowY + 5;
        int rightEdge = windowX + windowWidth - PADDING;

        int buttonWidth = 45;
        int buttonSpacing = 4;

        debugButton = addRenderableWidget(
            Button.builder(Component.literal("DBG"), b -> {
                showDebug = !showDebug;
                updateFilterButtons();
            })
                .bounds(rightEdge - buttonWidth, buttonY, buttonWidth, 20)
                .build()
        );

        errorButton = addRenderableWidget(
            Button.builder(Component.literal("ERR"), b -> {
                showError = !showError;
                updateFilterButtons();
            })
                .bounds(
                    rightEdge - buttonWidth * 2 - buttonSpacing,
                    buttonY,
                    buttonWidth,
                    20
                )
                .build()
        );

        warnButton = addRenderableWidget(
            Button.builder(Component.literal("WARN"), b -> {
                showWarn = !showWarn;
                updateFilterButtons();
            })
                .bounds(
                    rightEdge - buttonWidth * 3 - buttonSpacing * 2,
                    buttonY,
                    buttonWidth,
                    20
                )
                .build()
        );

        infoButton = addRenderableWidget(
            Button.builder(Component.literal("INFO"), b -> {
                showInfo = !showInfo;
                updateFilterButtons();
            })
                .bounds(
                    rightEdge - buttonWidth * 4 - buttonSpacing * 3,
                    buttonY,
                    buttonWidth,
                    20
                )
                .build()
        );

        updateFilterButtons();

        int footerY = windowY + windowHeight - FOOTER_HEIGHT + 5;

        searchBox = new EditBox(
            font,
            windowX + PADDING,
            footerY,
            200,
            18,
            Component.literal("Search")
        );
        searchBox.setHint(
            Component.translatable("logviewer.search.placeholder")
        );
        searchBox.setResponder(text -> filterText = text);
        addRenderableWidget(searchBox);

        clearButton = addRenderableWidget(
            Button.builder(
                Component.translatable("logviewer.button.clear"),
                b -> {
                    LogBuffer.getInstance().clear();
                }
            )
                .bounds(windowX + PADDING + 210, footerY, 50, 20)
                .build()
        );

        exportButton = addRenderableWidget(
            Button.builder(
                Component.translatable("logviewer.button.export"),
                b -> {
                    exportLogs();
                }
            )
                .bounds(windowX + PADDING + 265, footerY, 55, 20)
                .build()
        );

        autoScrollButton = addRenderableWidget(
            Button.builder(
                Component.literal(autoScroll ? "Auto: ON" : "Auto: OFF"),
                b -> {
                    autoScroll = !autoScroll;
                    autoScrollButton.setMessage(
                        Component.literal(autoScroll ? "Auto: ON" : "Auto: OFF")
                    );
                }
            )
                .bounds(windowX + PADDING + 325, footerY, 70, 20)
                .build()
        );
    }

    private void updateFilterButtons() {
        infoButton.setMessage(
            Component.literal((showInfo ? "[x] " : "[ ] ") + "INFO")
        );
        warnButton.setMessage(
            Component.literal((showWarn ? "[x] " : "[ ] ") + "WARN")
        );
        errorButton.setMessage(
            Component.literal((showError ? "[x] " : "[ ] ") + "ERR")
        );
        debugButton.setMessage(
            Component.literal((showDebug ? "[x] " : "[ ] ") + "DBG")
        );
    }

    @Override
    public void render(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
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
            windowY + HEADER_HEIGHT,
            HEADER_COLOR
        );
        graphics.fill(
            windowX,
            windowY + windowHeight - FOOTER_HEIGHT,
            windowX + windowWidth,
            windowY + windowHeight,
            HEADER_COLOR
        );

        drawBorder(graphics);

        graphics.drawString(
            font,
            "LOG VIEWER",
            windowX + PADDING,
            windowY + 10,
            ACCENT_COLOR
        );

        List<LogEntry> entries = getFilteredEntries();
        int logAreaY = windowY + HEADER_HEIGHT;
        int logAreaHeight = windowHeight - HEADER_HEIGHT - FOOTER_HEIGHT;
        int maxVisibleLines = logAreaHeight / LINE_HEIGHT;

        if (autoScroll && !entries.isEmpty()) {
            scrollOffset = Math.max(0, entries.size() - maxVisibleLines);
        }

        int startIndex = (int) scrollOffset;
        int endIndex = Math.min(
            startIndex + maxVisibleLines + 1,
            entries.size()
        );

        graphics.enableScissor(
            windowX,
            logAreaY,
            windowX + windowWidth,
            logAreaY + logAreaHeight
        );

        for (int i = startIndex; i < endIndex; i++) {
            LogEntry entry = entries.get(i);
            int y = logAreaY + (i - startIndex) * LINE_HEIGHT + 2;

            int color = getColorForLevel(entry.level());
            String text = entry.getFormattedMessage();

            if (text.length() > 120) {
                text = text.substring(0, 117) + "...";
            }

            graphics.drawString(font, text, windowX + PADDING, y, color, false);
        }

        graphics.disableScissor();

        String statusText = String.format(
            "Lines: %d / %d",
            entries.size(),
            LogBuffer.getInstance().size()
        );
        int statusWidth = font.width(statusText);
        graphics.drawString(
            font,
            statusText,
            windowX + windowWidth - statusWidth - PADDING,
            windowY + windowHeight - FOOTER_HEIGHT + 10,
            0xFF888888
        );

        super.render(graphics, mouseX, mouseY, delta);

        int resizeSize = 10;
        int resizeX = windowX + windowWidth - resizeSize;
        int resizeY = windowY + windowHeight - resizeSize;
        graphics.fill(
            resizeX,
            resizeY,
            windowX + windowWidth,
            windowY + windowHeight,
            BORDER_COLOR
        );
    }

    private void drawBorder(GuiGraphics graphics) {
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

        graphics.fill(
            windowX,
            windowY + HEADER_HEIGHT,
            windowX + windowWidth,
            windowY + HEADER_HEIGHT + 1,
            BORDER_COLOR
        );
        graphics.fill(
            windowX,
            windowY + windowHeight - FOOTER_HEIGHT,
            windowX + windowWidth,
            windowY + windowHeight - FOOTER_HEIGHT + 1,
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
            entry.matchesFilter(
                filterText,
                showInfo,
                showWarn,
                showError,
                showDebug
            )
        );
    }

    @Override
    public boolean mouseScrolled(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount
    ) {
        if (isMouseOverLogArea(mouseX, mouseY)) {
            autoScroll = false;
            autoScrollButton.setMessage(Component.literal("Auto: OFF"));
            scrollOffset = Math.max(0, scrollOffset - verticalAmount * 3);
            List<LogEntry> entries = getFilteredEntries();
            int logAreaHeight = windowHeight - HEADER_HEIGHT - FOOTER_HEIGHT;
            int maxVisibleLines = logAreaHeight / LINE_HEIGHT;
            scrollOffset = Math.min(
                scrollOffset,
                Math.max(0, entries.size() - maxVisibleLines)
            );
            return true;
        }
        return super.mouseScrolled(
            mouseX,
            mouseY,
            horizontalAmount,
            verticalAmount
        );
    }

    private boolean isMouseOverLogArea(double mouseX, double mouseY) {
        return (
            mouseX >= windowX &&
            mouseX <= windowX + windowWidth &&
            mouseY >= windowY + HEADER_HEIGHT &&
            mouseY <= windowY + windowHeight - FOOTER_HEIGHT
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0) {
            int resizeSize = 15;
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
                mouseY <= windowY + HEADER_HEIGHT
            ) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - windowX);
                dragOffsetY = (int) (mouseY - windowY);
                return true;
            }
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int button = event.button();
        if (button == 0) {
            isDragging = false;
            isResizing = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(
        MouseButtonEvent event,
        double deltaX,
        double deltaY
    ) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (isDragging) {
            windowX = (int) (mouseX - dragOffsetX);
            windowY = (int) (mouseY - dragOffsetY);
            windowX = Math.max(0, Math.min(windowX, width - windowWidth));
            windowY = Math.max(0, Math.min(windowY, height - windowHeight));
            repositionWidgets();
            return true;
        }

        if (isResizing) {
            windowWidth = (int) Math.max(400, mouseX - windowX);
            windowHeight = (int) Math.max(300, mouseY - windowY);
            windowWidth = Math.min(windowWidth, width - windowX);
            windowHeight = Math.min(windowHeight, height - windowY);
            repositionWidgets();
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    private void repositionWidgets() {
        int buttonY = windowY + 5;
        int rightEdge = windowX + windowWidth - PADDING;
        int buttonWidth = 45;
        int buttonSpacing = 4;

        debugButton.setX(rightEdge - buttonWidth);
        debugButton.setY(buttonY);
        errorButton.setX(rightEdge - buttonWidth * 2 - buttonSpacing);
        errorButton.setY(buttonY);
        warnButton.setX(rightEdge - buttonWidth * 3 - buttonSpacing * 2);
        warnButton.setY(buttonY);
        infoButton.setX(rightEdge - buttonWidth * 4 - buttonSpacing * 3);
        infoButton.setY(buttonY);

        int footerY = windowY + windowHeight - FOOTER_HEIGHT + 5;
        searchBox.setX(windowX + PADDING);
        searchBox.setY(footerY);
        clearButton.setX(windowX + PADDING + 210);
        clearButton.setY(footerY);
        exportButton.setX(windowX + PADDING + 265);
        exportButton.setY(footerY);
        autoScrollButton.setX(windowX + PADDING + 325);
        autoScrollButton.setY(footerY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int modifiers = event.modifiers();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrl && keyCode == GLFW.GLFW_KEY_L) {
            LogBuffer.getInstance().clear();
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_F) {
            searchBox.setFocused(true);
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_S) {
            autoScroll = !autoScroll;
            autoScrollButton.setMessage(
                Component.literal(autoScroll ? "Auto: ON" : "Auto: OFF")
            );
            return true;
        }

        return super.keyPressed(event);
    }

    private void exportLogs() {
        String filename =
            "logs/log-viewer-export-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            ) +
            ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            List<LogEntry> entries = getFilteredEntries();
            for (LogEntry entry : entries) {
                writer.println(entry.getFormattedMessage());
            }
        } catch (IOException e) {
            dev.logviewer.LogViewerMod.LOGGER.error(
                "Failed to export logs: {}",
                e.getMessage()
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
