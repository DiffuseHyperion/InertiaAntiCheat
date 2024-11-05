package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InertiaAntiCheatClient implements ClientModInitializer {
    public static Toml clientConfig;
    public static final List<String> allModNames = new ArrayList<>();
    public static final List<byte[]> allModData = new ArrayList<>();
    public static final List<String> allResourcePackNames = new ArrayList<>();
    public static final List<byte[]> allResourcePackData = new ArrayList<>();


    @Override
    public void onInitializeClient() {
        InertiaAntiCheatClient.clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION);

        this.setupModlist();
        this.setupResourcePackList();
        ClientLoginModlistTransferHandler.init();
    }

    public void setupModlist() {
        try {
            File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
            for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
                if (modFile.isDirectory()) {
                    continue;
                }
                if (!modFile.getAbsolutePath().endsWith(".jar")) {
                    continue;
                }
                InertiaAntiCheatClient.allModNames.add(modFile.getName());
                InertiaAntiCheatClient.allModData.add(Files.readAllBytes(modFile.toPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setupResourcePackList() {
        try {
            File modDirectory = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").toFile();
            for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
                if (modFile.isDirectory()) {
                    continue;
                }
                if (!modFile.getAbsolutePath().endsWith(".zip")) {
                    continue;
                }
                InertiaAntiCheatClient.allResourcePackNames.add(modFile.getName());
                InertiaAntiCheatClient.allResourcePackData.add(Files.readAllBytes(modFile.toPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
