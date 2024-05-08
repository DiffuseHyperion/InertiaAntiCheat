package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.ModListRequestS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION;

public class InertiaAntiCheatClient implements ClientModInitializer {

    public static Toml clientConfig;
    public static SecretKey clientE2EESecretKey; // null if e2ee not enabled
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(ModListRequestS2CPayload.ID, ModListRequestS2CPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ModListRequestS2CPayload.ID, ModListRequestS2CPayload::onReceive);
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
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(256);
                secretKey = keyGenerator.generateKey();

                secretKeyFile.createNewFile();
                Files.write(secretKeyFile.toPath(), secretKey.getEncoded());

                debugInfo("Secret key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(secretKey.getEncoded()), "MD5"));
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException("Something went wrong while generating new key!", e);
            }
        } else {
            debugInfo("Found secret key file.");
            try {
                Path privateKeyFilePath = Paths.get(secretKeyFile.toURI());
                byte[] privateKeyFileBytes = Files.readAllBytes(privateKeyFilePath);
                secretKey = new SecretKeySpec(privateKeyFileBytes, "AES");

                debugInfo("Secret key MD5 hash: " + InertiaAntiCheat.getHash(Arrays.toString(secretKey.getEncoded()), "MD5"));
            } catch (IOException e) {
                throw new RuntimeException("Something went wrong while reading the secret key!", e);
            }
        }
        return secretKey;
    }
}
