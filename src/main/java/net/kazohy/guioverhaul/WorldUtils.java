package net.kazohy.guioverhaul;

import com.mojang.datafixers.types.templates.Tag;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.util.NbtType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelStorage.LevelList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static com.mojang.text2speech.Narrator.LOGGER;

public class WorldUtils {
    public static String getMostRecentSingleplayerWorldName() {
        MinecraftClient client = MinecraftClient.getInstance();
        LevelStorage storage = client.getLevelStorage();

        try {
            LevelList levelList = storage.getLevelList();
            List<LevelSummary> summaries = storage.loadSummaries(levelList)
                    .thenApply(list -> list.stream()
                            .sorted(Comparator.comparing(LevelSummary::getLastPlayed).reversed())
                            .toList()
                    )
                    .join(); // block until ready

            if (!summaries.isEmpty()) {
                return summaries.get(0).getName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return "No worlds played";
    }


    public static GameMode getWorldGameMode(String worldName) throws IOException {
        // Point to default saves folder (relative to run directory)
        File worldDirectory = new File("saves", worldName);
        File levelDat = new File(worldDirectory, "level.dat");

        if (!levelDat.exists()) {
            throw new IOException("level.dat not found for world: " + worldName);
        }

        try (FileInputStream fis = new FileInputStream(levelDat)) {
            NbtCompound nbt = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());

            if (nbt == null || !nbt.contains("Data")) {
                throw new IOException("Invalid level.dat structure in world: " + worldName);
            }

            NbtCompound data = nbt.getCompound("Data");

            // Check for hardcore first
            if (data.contains("hardcore") && data.getBoolean("hardcore")) {
                return GameMode.SURVIVAL;
            }

            int gameType = data.getInt("GameType");

            return switch (gameType) {
                case 0 -> GameMode.SURVIVAL;
                case 1 -> GameMode.CREATIVE;
                case 2 -> GameMode.ADVENTURE;
                case 3 -> GameMode.SPECTATOR;
                default -> throw new IOException("Unknown GameType: " + gameType);
            };
        }
    }
}