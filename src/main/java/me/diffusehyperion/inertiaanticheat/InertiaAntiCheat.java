package me.diffusehyperion.inertiaanticheat;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;
import static me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient.clientConfig;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.serverConfig;

public class InertiaAntiCheat implements ModInitializer {

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing InertiaAntiCheat!");
        try {
            Files.createDirectories(getConfigDir());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void debugInfo(String info) {
        if (Objects.nonNull(serverConfig) && serverConfig.getBoolean("debug.debug")) {
            LOGGER.info(info);
        } else if (Objects.nonNull(clientConfig) && clientConfig.getBoolean("debug.debug")) {
            LOGGER.info(info);
        }
    }

    public static String listToPrettyString(List<String> list) {
        switch (list.size()) {
            case 0 -> {
                return "";
            }
            case 1 -> {
                return list.get(0);
            }
            default -> {
                StringBuilder builder = new StringBuilder();
                builder.append(list.get(0));
                for (int i = 1; i < list.size(); i++) {
                    if (i != (list.size() - 1)) {
                        builder.append(", ");
                        builder.append(list.get(i));
                    } else {
                        builder.append(" and ");
                        builder.append(list.get(i));
                    }
                }
                return builder.toString();
            }
        }
    }

    public static String getHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] arr = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(arr);
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }

    public static Toml initializeConfig(String defaultConfigPath, Long currentConfigVersion) {
        File configFile = getConfigDir().resolve("./InertiaAntiCheat.toml").toFile();
        if (!configFile.exists()) {
            LOGGER.warn("No config file found! Creating a new one now...");
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream(defaultConfigPath)), configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't a create default config!", e);
            }
        }
        Toml config = new Toml().read(configFile);
        if (!Objects.equals(config.getLong("debug.version", 0L), currentConfigVersion)) {
            LOGGER.warn("Looks like your config file is outdated! Backing up current config, then creating an updated config.");
            LOGGER.warn("Your config file will be backed up to \"BACKUP-InertiaAntiCheat.toml\".");
            File backupFile = getConfigDir().resolve("BACKUP-InertiaAntiCheat.toml").toFile();
            try {
                Files.copy(configFile.toPath(), backupFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't copy existing config file into a backup config file! Please do it manually.", e);
            }
            if (!configFile.delete()) {
                throw new RuntimeException("Couldn't delete config file! Please delete it manually.");
            }
            try {
                Files.copy(Objects.requireNonNull(InertiaAntiCheatServer.class.getResourceAsStream(defaultConfigPath)), configFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't create a default config!", e);
            }
            LOGGER.info("Done! Please readjust the configs in the new file accordingly.");
        }
        return config;
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("InertiaAntiCheat");
    }

    public static byte[] encryptBytes(byte[] input, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptBytes(byte[] input, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptBytes(byte[] input, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptBytes(byte[] input, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(input);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
