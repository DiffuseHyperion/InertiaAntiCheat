package com.diffusehyperion.inertiaanticheat.server;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.util.HashAlgorithm;
import com.diffusehyperion.inertiaanticheat.common.util.InertiaAntiCheatConstants;
import com.diffusehyperion.inertiaanticheat.common.util.TransferMethod;
import com.diffusehyperion.inertiaanticheat.common.util.ValidationMethod;
import com.moandjiezana.toml.Toml;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import static com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat.*;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static Toml serverConfig;
    public static TransferMethod transferMethod;
    public static ValidationMethod validationMethod;
    public static HashAlgorithm hashAlgorithm;

    @Override
    public void onInitializeServer() {
        InertiaAntiCheatServer.serverConfig = InertiaAntiCheat.initializeConfig("/config/server/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_SERVER_CONFIG_VERSION);

        switch (InertiaAntiCheatServer.serverConfig.getString("transfer.method").toLowerCase()) {
            case "data" -> {
                InertiaAntiCheat.info("Using data transfer method");
                InertiaAntiCheatServer.transferMethod = TransferMethod.DATA;
            }
            case "name" -> {
                InertiaAntiCheat.info("Using name transfer method");
                InertiaAntiCheatServer.transferMethod = TransferMethod.NAME;
            }
            case "id" -> {
                InertiaAntiCheat.info("Using id transfer method");
                InertiaAntiCheatServer.transferMethod = TransferMethod.ID;
            }
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid transfer method specified under \"transfer.method\"! ");
                InertiaAntiCheat.error("Defaulting to data transfer method for now.");
                InertiaAntiCheatServer.transferMethod = TransferMethod.DATA;
            }
        }

        switch (InertiaAntiCheatServer.serverConfig.getString("validation.method").toLowerCase()) {
            case "individual" -> {
                InertiaAntiCheat.info("Using individual validation method");
                InertiaAntiCheatServer.validationMethod = ValidationMethod.INDIVIDUAL;
            }
            case "group" -> {
                InertiaAntiCheat.info("Using group validation method");
                InertiaAntiCheatServer.validationMethod = ValidationMethod.GROUP;
            }
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid validation method specified under \"validation.method\"! ");
                InertiaAntiCheat.error("Defaulting to individual method for now.");
                InertiaAntiCheatServer.validationMethod = ValidationMethod.INDIVIDUAL;
            }
        }

        switch (InertiaAntiCheatServer.serverConfig.getString("validation.algorithm").toLowerCase()) {
            case "md5" -> {
                InertiaAntiCheat.info("Using MD5 algorithm for validation");
                InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.MD5;
            }
            case "sha1" -> {
                InertiaAntiCheat.info("Using SHA1 algorithm for validation");
                InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.SHA1;
            }
            case "sha256" -> {
                InertiaAntiCheat.info("Using SHA256 algorithm for validation");
                InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.SHA256;
            }
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid algorithm specified under \"validation.algorithm\"! ");
                InertiaAntiCheat.error("Defaulting to MD5 algorithm for now.");
                InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.MD5;
            }
        }

        ServerLoginModlistTransferHandler.init();
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
        return FabricLoader.getInstance().isDevelopmentEnvironment() || serverConfig.getBoolean("debug.debug");
    }
}
