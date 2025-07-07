package zone.oat.gmodtitlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelStorage.LevelList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Optional;

import java.util.Comparator;
import java.util.List;

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
                return summaries.get(0).getDisplayName();
            }

            summaries.sort(Comparator.comparing(LevelSummary::getLastPlayed).reversed());

            if (!summaries.isEmpty()) {
                return summaries.get(0).getDisplayName(); // or getName() for folder
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Optional<String> getLastServerAddress() {
        try {
            File serversFile = new File(MinecraftClient.getInstance().runDirectory, "servers.dat");
            if (!serversFile.exists()) return Optional.empty();

            InputStream input = new FileInputStream(serversFile);
            NbtCompound serversDat = NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes());
            NbtList servers = serversDat.getList("servers", 10); // Tag 10 = Compound

            if (servers.isEmpty()) return Optional.empty();

            // Assume first in list is most recently used (usually true)
            NbtCompound mostRecent = servers.getCompound(0);
            return Optional.ofNullable(mostRecent.getString("ip"));

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}