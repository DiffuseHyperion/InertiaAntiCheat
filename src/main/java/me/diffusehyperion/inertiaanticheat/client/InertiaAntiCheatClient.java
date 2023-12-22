package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.Scheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION;

public class InertiaAntiCheatClient implements ClientModInitializer {
    public static Toml clientConfig;
    public static SecretKey clientE2EESecretKey; // null if e2ee not enabled
    public static final Scheduler clientScheduler = new Scheduler();

    @Override
    public void onInitializeClient() {
        clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", CURRENT_CLIENT_CONFIG_VERSION);
        debugInfo("Initializing E2EE...");
        clientE2EESecretKey = initializeE2EE();
    }

    private SecretKey initializeE2EE() {
        if (!clientConfig.getBoolean("e2ee.enable")) {
            debugInfo("E2EE was not enabled. Skipping e2ee initialization...");
            return null;
        }

        String secretKeyFileName = clientConfig.getString("e2ee.secretKeyName");
        File secretKeyFile = getConfigDir().resolve("./" + secretKeyFileName).toFile();
        SecretKey secretKey;
        if (!secretKeyFile.exists()) {
            warn("E2EE was enabled, but the mod could not find the secret key file! Generating new secret key now...");
            warn("This is fine if this is the first time you are running the mod.");
            secretKey = InertiaAntiCheat.createAESKey(secretKeyFile);
        } else {
            debugInfo("Found secret key file.");
            secretKey = InertiaAntiCheat.loadAESKey(secretKeyFile);
            debugInfo("Secret key MD5 hash: " + InertiaAntiCheat.getChecksum(secretKey.getEncoded(), "MD5"));
        }
        return secretKey;
    }

    public static byte[] serializeModlist() {
        try {
            File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
            List<File> modFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(modDirectory.listFiles())));
            InertiaAntiCheat.info("Serializing " + modFiles.size() + " mods");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(modFiles);

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
