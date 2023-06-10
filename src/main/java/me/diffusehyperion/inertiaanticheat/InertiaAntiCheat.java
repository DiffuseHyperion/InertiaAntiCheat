package me.diffusehyperion.inertiaanticheat;

import net.fabricmc.api.ModInitializer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Base64;
import java.util.List;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.config;

public class InertiaAntiCheat implements ModInitializer {

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing InertiaAntiCheat!");
    }

    public static void debugInfo(String info) {
        if (config.getBoolean("debug.debug")) {
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

    public static String encryptString(String input, PublicKey key) {
        try {
            Cipher encryptionCipher = Cipher.getInstance("RSA");
            encryptionCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedMessageBytes =
                    encryptionCipher.doFinal(input.getBytes());
            return Base64.getEncoder().encodeToString(encryptedMessageBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    public static String decryptString(String input, PrivateKey key) {
        try {
            Cipher decryptionCipher = Cipher.getInstance("RSA");
            decryptionCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedMessageBytes =
                    decryptionCipher.doFinal(Base64.getDecoder().decode(input));
            return new String(decryptedMessageBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
