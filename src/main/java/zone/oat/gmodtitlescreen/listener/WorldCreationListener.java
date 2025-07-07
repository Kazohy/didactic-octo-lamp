package zone.oat.gmodtitlescreen.listener;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.oat.gmodtitlescreen.screen.TakeScreenshotHD;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.fabricmc.loader.impl.FabricLoaderImpl.MOD_ID;
import static zone.oat.gmodtitlescreen.screen.CustomTitleScreen.takeScreenshot;

public class WorldCreationListener {
    private static int ticksRemaining = -1;

    public static void startCountdown(int ticks) {
        ticksRemaining = ticks;
    }

    private static boolean newWorldCreated = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksRemaining > 0) {
                ticksRemaining--;
            } else if (ticksRemaining == 0) {
                takeScreenshot(client);
                ticksRemaining = -1; // Reset
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(WorldCreationListener::onWorldCreation);
        ServerPlayConnectionEvents.JOIN.register(WorldCreationListener::onPlayerJoin);
    }

    public static boolean onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.player;

        System.out.println(player.getName().getString() + " has joined the world!");

        if (newWorldCreated) {
            newWorldCreated = false;
            WorldCreationListener.startCountdown(100);
            MinecraftClient client = MinecraftClient.getInstance();
            takeScreenshot(client);
            return true;
        } else {
            return false;
        }
    }

    private static void onWorldCreation(MinecraftServer server) {
        newWorldCreated = true;
        System.out.println("World is being created (server started)");
    }

}