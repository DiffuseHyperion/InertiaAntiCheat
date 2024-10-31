package me.diffusehyperion.inertiaanticheat.networking.method.data;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.networking.method.data.handlers.DataValidationHandler;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;

import java.util.List;

public class ServerDataIndividualValidatorHandler extends DataValidationHandler {

    public ServerDataIndividualValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    public boolean validateMods(List<byte[]> modlist) {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Checking modlist now, using individual method");
        InertiaAntiCheat.debugInfo("Mod list size: " + modlist.size());
        List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.blacklist");
        InertiaAntiCheat.debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));
        List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.whitelist");
        InertiaAntiCheat.debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));
        InertiaAntiCheat.debugLine();
        for (byte[] mod : modlist) {
            String fileHash = InertiaAntiCheat.getHash(mod, InertiaAntiCheatServer.hashAlgorithm);
            InertiaAntiCheat.debugInfo("File hash: " + fileHash + "; with algorithm " + InertiaAntiCheatServer.hashAlgorithm);

            if (blacklistedMods.contains(fileHash)) {
                InertiaAntiCheat.debugInfo("Found in blacklist");
                InertiaAntiCheat.debugLine();
                return false;
            }
            if (whitelistedMods.contains(fileHash)) {
                InertiaAntiCheat.debugInfo("Found in whitelist");
                whitelistedMods.remove(fileHash);
            }
            InertiaAntiCheat.debugLine();
        }
        if (!whitelistedMods.isEmpty()) {
            InertiaAntiCheat.debugInfo("Whitelist not fulfilled");
            InertiaAntiCheat.debugLine();
            return false;
        }
        InertiaAntiCheat.debugInfo("Passed");
        InertiaAntiCheat.debugLine2();
        return true;
    }
}
