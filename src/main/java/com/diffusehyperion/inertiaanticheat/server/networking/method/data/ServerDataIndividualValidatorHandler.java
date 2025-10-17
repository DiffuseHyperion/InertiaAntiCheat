package com.diffusehyperion.inertiaanticheat.server.networking.method.data;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.handlers.DataValidationHandler;

import java.util.List;

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.*;

public class ServerDataIndividualValidatorHandler extends DataValidationHandler {

    public ServerDataIndividualValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    public boolean validateMods(List<byte[]> modlist) {
        debugLine2();
        debugInfo("Checking modlist now, using individual method");
        debugInfo("Mod list size: " + modlist.size());
        List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.individual.blacklist");
        debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));
        List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.individual.whitelist");
        debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));
        debugLine();
        for (byte[] mod : modlist) {
            String fileHash = InertiaAntiCheat.getHash(mod, InertiaAntiCheatServer.hashAlgorithm);
            debugInfo("File hash: " + fileHash + "; with algorithm " + InertiaAntiCheatServer.hashAlgorithm);

            if (blacklistedMods.contains(fileHash)) {
                debugInfo("Found in blacklist");
                debugLine();
                return false;
            }
            if (whitelistedMods.contains(fileHash)) {
                debugInfo("Found in whitelist");
                whitelistedMods.remove(fileHash);
            }
            debugLine();
        }
        if (!whitelistedMods.isEmpty()) {
            debugInfo("Whitelist not fulfilled");
            debugLine();
            return false;
        }
        debugInfo("Passed");
        debugLine2();
        return true;
    }
}
