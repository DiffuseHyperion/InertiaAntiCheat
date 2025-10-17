package com.diffusehyperion.inertiaanticheat.client;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.util.InertiaAntiCheatConstants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
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

import static com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat.*;

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

    public static void debugInfo(String info) {
        if (inDebug()) {
            info(info);
        }
    }

    public static void debugWarn(String info) {
        if (inDebug()) {
            warn(info);
        }
    }

    public static void debugError(String info) {
        if (inDebug()) {
            error(info);
        }
    }

    public static void debugLine() {
        if (inDebug()) {
            info("--------------------"); // lol
        }
    }

    public static void debugLine2() {
        if (inDebug()) {
            info("===================="); // lol 2
        }
    }

    public static boolean inDebug() {
        return FabricLoader.getInstance().isDevelopmentEnvironment() || clientConfig.getBoolean("debug.debug");
    }

    private void setupModlist() {
        File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
        for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
            debugInfo("Attempting to load mod " + modFile.getName());
            if (modFile.isDirectory()) {
                debugWarn("Skipping mod " + modFile.getName() + " as it is a directory");
                continue;
            }
            if (!modFile.getAbsolutePath().endsWith(".jar")) {
                debugWarn("Skipping mod " + modFile.getName() + " as it does not end with .jar");
                continue;
            }

            try (JarFile jarFile = new JarFile(modFile)) {
                ZipEntry entry = jarFile.getEntry("fabric.mod.json");
                if (Objects.isNull(entry)) {
                    debugWarn("Skipping mod " + modFile.getName() + " as it does not contain \"fabric.mod.json\"");
                    continue;
                }

                Gson gson = new Gson();
                try (InputStream input = jarFile.getInputStream(entry)) {
                    JsonObject root = gson.fromJson(new InputStreamReader(input), JsonObject.class);

                    if (!root.has("id")) {
                        debugWarn("Skipping mod " + modFile.getName() + " as it does not contain a mod ID");
                        continue;
                    }

                    InertiaAntiCheatClient.allModNames.add(modFile.getName());
                    InertiaAntiCheatClient.allModPaths.add(modFile.toPath());
                    InertiaAntiCheatClient.allModIds.add(root.get("id").getAsString());
                    debugInfo("Successfully loaded " + modFile.getName());
                }
            } catch (IOException e) {
                debugWarn("Skipping mod " + modFile.getName() + " as it could not be read");
            }
        }
        InertiaAntiCheat.info("Loaded " + InertiaAntiCheatClient.allModNames.size() + " mods");
    }
}