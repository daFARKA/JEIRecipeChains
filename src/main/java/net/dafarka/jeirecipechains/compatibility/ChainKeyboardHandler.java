package net.dafarka.jeirecipechains.compatibility;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ChainKeyboardHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;

        // Only act when JEI screen is open
        if (!mc.screen.getClass().getName().contains("mezz.jei")) return;

        long window = mc.getWindow().getWindow();

        // WASD for pan
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W)) ChainViewState.move(0, -5);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S)) ChainViewState.move(0, 5);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A)) ChainViewState.move(-5, 0);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D)) ChainViewState.move(5, 0);

        // +/- for zoom
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_EQUAL)) ChainViewState.zoomIn(); // '+' key
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_MINUS)) ChainViewState.zoomOut(); // '-' key
    }
}
