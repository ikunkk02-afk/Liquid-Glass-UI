/*
 * Adapted from ReGlass by ReStudio / RedxAx.
 * Original project: https://github.com/RedxAx/ReGlass
 * Licensed under the MIT License.
 * Backported and modified for Minecraft Fabric 1.21.1.
 */
package restudio.reglass.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;
import restudio.reglass.client.api.WidgetStyle;
import restudio.reglass.client.config.ReGlassSettingsIO;
import restudio.reglass.client.legacy1211.Legacy1211ShaderManager;
import restudio.reglass.client.screen.config.ReGlassConfigScreen;
import restudio.reglass.ReGlass;

public class ReGlassClient implements ClientModInitializer {
    private static KeyMapping playgroundKey;
    private static KeyMapping configKey;

    public static Minecraft minecraftClient;
    private static boolean developmentCaptureOpened;
    private static int developmentCaptureReadyTicks;
    private static int developmentCaptureTicks = -1;

    @Override
    public void onInitializeClient() {
        minecraftClient = Minecraft.getInstance();
        Legacy1211ShaderManager.register();

        playgroundKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.reglass.playground", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.reglass"));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.reglass.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.reglass"));

        ReGlassSettingsIO.loadIntoMemory();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Boolean.getBoolean("reglass.capturePlayground") && !developmentCaptureOpened) {
                if (Legacy1211ShaderManager.ready() && client.getOverlay() == null && client.screen != null) {
                    developmentCaptureReadyTicks++;
                    if (developmentCaptureReadyTicks >= 40) {
                        developmentCaptureOpened = true;
                        developmentCaptureTicks = 0;
                        client.setScreen(new PlaygroundScreen());
                    }
                } else {
                    developmentCaptureReadyTicks = 0;
                }
            } else if (developmentCaptureTicks >= 0) {
                developmentCaptureTicks++;
                if (developmentCaptureTicks == 60) {
                    Screenshot.grab(client.gameDirectory, "reglass-backport-1.21.1.png", client.getMainRenderTarget(), message -> ReGlass.LOGGER.info(message.getString()));
                } else if (developmentCaptureTicks == 100) {
                    client.stop();
                }
            }
            if (configKey.consumeClick()) {
                client.setScreen(new ReGlassConfigScreen(null));
            }
            if (playgroundKey.consumeClick()) {
                client.setScreen(new PlaygroundScreen());
            }
        });
    }

    public static class PlaygroundScreen extends Screen {
        private boolean blur;
        private WidgetStyle customStyle;

        public PlaygroundScreen() {
            super(Component.literal("ReGlass Playground"));
        }

        @Override
        protected void init() {
            super.init();

            customStyle = WidgetStyle.create().tint(ChatFormatting.GOLD.getColor(), 0.4f).blurRadius(0).shadow(25f, 0.2f, 0f, 3f).smoothing(.05f).shadowColor(0x000000, 1.0f);
            addRenderableWidget(new LiquidGlassWidget(width / 2 - 75, height / 2 - 25, 150, 50, customStyle).setMoveable(true));
            if (Boolean.getBoolean("reglass.capturePlayground")) {
                addRenderableWidget(new LiquidGlassWidget(width / 2 - 120, height / 2 + 20, 100, 100, WidgetStyle.create().smoothing(.05f)).setMoveable(true));
                addRenderableWidget(new LiquidGlassWidget(width / 2 - 50, height / 2 + 35, 100, 100, WidgetStyle.create().smoothing(.05f)).setMoveable(true));
                addRenderableWidget(new LiquidGlassWidget(width / 2 + 20, height / 2 + 20, 100, 100, WidgetStyle.create().smoothing(.05f)).setMoveable(true));
            }
            addRenderableWidget(Button.builder(Component.literal("Toggle BG Blur"), b -> blur = !blur).bounds(10, 10, 120, 20).build());
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            context.drawString(minecraftClient.font, Component.literal("This is a Minecraft Screen"), width / 2 - 70, 10, 0xFFFFFFFF, true);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
            if (blur) super.renderBackground(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1) {
                addRenderableWidget(new LiquidGlassWidget((int) mouseX - 50, (int) mouseY - 50, 100, 100, WidgetStyle.create().smoothing(.05f))).setMoveable(true);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
