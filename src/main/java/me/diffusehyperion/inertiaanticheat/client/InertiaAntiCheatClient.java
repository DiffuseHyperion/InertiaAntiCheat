package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InertiaAntiCheatClient implements ClientModInitializer {
    public static Toml clientConfig;
    public static final List<String> allModNames = new ArrayList<>();
    public static final List<Path> allModPaths = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        InertiaAntiCheatClient.clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION);

        this.setupModlist();
        ClientLoginModlistTransferHandler.init();
    }

    public void setupModlist() {
        File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
        for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
            if (modFile.isDirectory()) {
                continue;
            }
            if (!modFile.getAbsolutePath().endsWith(".jar")) {
                continue;
            }
            InertiaAntiCheatClient.allModNames.add(modFile.getName());
            InertiaAntiCheatClient.allModPaths.add(modFile.toPath());
        }
        InertiaAntiCheat.debugInfo("Found " + InertiaAntiCheatClient.allModNames.size() + " mods");
    }
}
