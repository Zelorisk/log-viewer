package dev.logviewer;

import dev.logviewer.log.LogCapture;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogViewerMod implements ModInitializer {
    public static final String MOD_ID = "log-viewer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LogCapture.install();
        LOGGER.info("Log Viewer initialized - press F7 to toggle the log viewer");
    }
}
