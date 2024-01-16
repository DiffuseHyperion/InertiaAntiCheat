package me.diffusehyperion.inertiaanticheat.server;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import net.fabricmc.api.DedicatedServerModInitializer;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static Toml serverConfig;
    public static ModlistCheckMethod modlistCheckMethod;
    public static HashAlgorithm hashAlgorithm;

    @Override
    public void onInitializeServer() {
        InertiaAntiCheatServer.serverConfig = InertiaAntiCheat.initializeConfig("/config/server/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_SERVER_CONFIG_VERSION);

        switch (InertiaAntiCheatServer.serverConfig.getString("mods.method").toLowerCase()) {
            case "individual" -> InertiaAntiCheatServer.modlistCheckMethod = ModlistCheckMethod.INDIVIDUAL;
            case "group" -> InertiaAntiCheatServer.modlistCheckMethod = ModlistCheckMethod.GROUP;
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid method specified under \"mods.method\"! ");
                InertiaAntiCheat.error("Defaulting to individual method for now.");
                InertiaAntiCheatServer.modlistCheckMethod = ModlistCheckMethod.INDIVIDUAL;
            }
        }

        switch (InertiaAntiCheatServer.serverConfig.getString("mods.algorithm").toLowerCase()) {
            case "md5" -> InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.MD5;
            case "sha1" -> InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.SHA1;
            case "sha256" -> InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.SHA256;
            default -> {
                InertiaAntiCheat.error("There was an error in your config! Invalid algorithm specified under \"mods.algorithm\"! ");
                InertiaAntiCheat.error("Defaulting to MD5 algorithm for now.");
                InertiaAntiCheatServer.hashAlgorithm = HashAlgorithm.MD5;
            }
        }

        ServerLoginModlistTransferHandler.init();
    }
}
