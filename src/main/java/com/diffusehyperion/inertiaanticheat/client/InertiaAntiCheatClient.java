package com.diffusehyperion.inertiaanticheat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moandjiezana.toml.Toml;
import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class InertiaAntiCheatClient implements ClientModInitializer {
    public static Toml clientConfig;
    public static final List<String> allModNames = new ArrayList<>();
    public static final List<Path> allModPaths = new ArrayList<>();
    public static final List<String> allModIds = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        InertiaAntiCheatClient.clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION);

        this.setupModlist();
        ClientLoginModlistTransferHandler.init();
    }

    private void setupModlist() {
        File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
        for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
            if (modFile.isDirectory()) {
                continue;
            }
            if (!modFile.getAbsolutePath().endsWith(".jar")) {
                continue;
            }
            try (JarFile jarFile = new JarFile(modFile)) {
                ZipEntry entry = jarFile.getEntry("fabric.mod.json");
                if (Objects.isNull(entry)) {
                    continue;
                }

                try (InputStream input = jarFile.getInputStream(entry)) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(input);

                    InertiaAntiCheatClient.allModNames.add(modFile.getName());
                    InertiaAntiCheatClient.allModPaths.add(modFile.toPath());
                    InertiaAntiCheatClient.allModIds.add(root.path("id").asText());
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read mod file as a jar file", e);
            }

        }
        InertiaAntiCheat.debugInfo("Found " + InertiaAntiCheatClient.allModNames.size() + " mods");
    }
}
