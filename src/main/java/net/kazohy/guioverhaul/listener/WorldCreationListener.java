package net.kazohy.guioverhaul.listener;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kazohy.guioverhaul.Mod;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static net.kazohy.guioverhaul.screen.CustomTitleScreen.takeScreenshot;

public class WorldCreationListener {
    public static boolean shouldScreenshotBeTaken = false;
    private static long screenshotCountdownEndTime = -1;

    public static void startCountdown(int delayMillis) {
        screenshotCountdownEndTime = System.currentTimeMillis() + delayMillis;
    }

    public static boolean isCountdownFinished() {
        return screenshotCountdownEndTime > 0 && System.currentTimeMillis() >= screenshotCountdownEndTime;
    }

    public static void resetCountdown() {
        screenshotCountdownEndTime = -1;
    }
    private static int ticksRemaining = -1;

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
        ServerPlayConnectionEvents.DISCONNECT.register(WorldCreationListener::onPlayerDisconnect);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
            shouldScreenshotBeTaken = true;
    }

    private static void onPlayerDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        shouldScreenshotBeTaken = false;
    }

    private static void onWorldCreation(MinecraftServer server) {
        newWorldCreated = true;
    }

    public static Path getMostRecentlyCreatedWorld() {
        Path savesDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("saves");

        try (Stream<Path> worldDirs = Files.list(savesDir)) {
            return worldDirs
                    .filter(Files::isDirectory)
                    .map(path -> {
                        try {
                            return new AbstractMap.SimpleEntry<>(path, Files.getLastModifiedTime(path).toMillis());
                        } catch (IOException e) {
                            return new AbstractMap.SimpleEntry<>(path, 0L);
                        }
                    })
                    .max(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}