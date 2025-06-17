package com.diffusehyperion.inertiaanticheat.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
            InertiaAntiCheat.debugInfo("Attempting to load mod " + modFile.getName());
            if (modFile.isDirectory()) {
                InertiaAntiCheat.debugWarn("Skipping mod " + modFile.getName() + " as it is a directory");
                continue;
            }
            if (!modFile.getAbsolutePath().endsWith(".jar")) {
                InertiaAntiCheat.debugWarn("Skipping mod " + modFile.getName() + " as it does not end with .jar");
                continue;
            }

            try (JarFile jarFile = new JarFile(modFile)) {
                ZipEntry entry = jarFile.getEntry("fabric.mod.json");
                if (Objects.isNull(entry)) {
                    InertiaAntiCheat.debugWarn("Skipping mod " + modFile.getName() + " as it does not contain \"fabric.mod.json\"");
                    continue;
                }

                Gson gson = new Gson();
                try (InputStream input = jarFile.getInputStream(entry)) {
                    JsonObject root = gson.fromJson(new InputStreamReader(input), JsonObject.class);

                    if (!root.has("id")) {
                        InertiaAntiCheat.debugWarn("Skipping mod " + modFile.getName() + " as it does not contain a mod ID");
                        continue;
                    }

                    InertiaAntiCheatClient.allModNames.add(modFile.getName());
                    InertiaAntiCheatClient.allModPaths.add(modFile.toPath());
                    InertiaAntiCheatClient.allModIds.add(root.get("id").getAsString());
                    InertiaAntiCheat.debugInfo("Successfully loaded " + modFile.getName());
                }
            } catch (IOException e) {
                InertiaAntiCheat.debugWarn("Skipping mod " + modFile.getName() + " as it could not be read");
            }
        }
        InertiaAntiCheat.info("Loaded " + InertiaAntiCheatClient.allModNames.size() + " mods");
    }
}