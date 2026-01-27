package dev.logviewer.client.mixin;

import dev.logviewer.client.gui.LogViewerHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(
        long window,
        MouseButtonInfo buttonInfo,
        int action,
        CallbackInfo ci
    ) {
        if (minecraft.screen != null) return;

        LogViewerHud hud = LogViewerHud.getInstance();
        if (!hud.isVisible() || !hud.isMouseControlActive()) return;

        double scaledX =
            (xpos * (double) minecraft.getWindow().getGuiScaledWidth()) /
            (double) minecraft.getWindow().getScreenWidth();
        double scaledY =
            (ypos * (double) minecraft.getWindow().getGuiScaledHeight()) /
            (double) minecraft.getWindow().getScreenHeight();

        int button = buttonInfo.button();

        if (action == 1) {
            if (hud.handleMouseClick(scaledX, scaledY, button)) {
                ci.cancel();
            }
        } else if (action == 0) {
            if (hud.handleMouseRelease(scaledX, scaledY, button)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        if (minecraft.screen != null) return;

        LogViewerHud hud = LogViewerHud.getInstance();
        if (!hud.isVisible() || !hud.isMouseControlActive()) return;

        double scaledX =
            (x * (double) minecraft.getWindow().getGuiScaledWidth()) /
            (double) minecraft.getWindow().getScreenWidth();
        double scaledY =
            (y * (double) minecraft.getWindow().getGuiScaledHeight()) /
            (double) minecraft.getWindow().getScreenHeight();

        double deltaX = x - xpos;
        double deltaY = y - ypos;

        hud.handleMouseDrag(scaledX, scaledY, 0, deltaX, deltaY);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(
        long window,
        double horizontal,
        double vertical,
        CallbackInfo ci
    ) {
        if (minecraft.screen != null) return;

        LogViewerHud hud = LogViewerHud.getInstance();
        if (!hud.isVisible() || !hud.isMouseControlActive()) return;

        double scaledX =
            (xpos * (double) minecraft.getWindow().getGuiScaledWidth()) /
            (double) minecraft.getWindow().getScreenWidth();
        double scaledY =
            (ypos * (double) minecraft.getWindow().getGuiScaledHeight()) /
            (double) minecraft.getWindow().getScreenHeight();

        if (hud.handleMouseScroll(scaledX, scaledY, vertical)) {
            ci.cancel();
        }
    }
}
