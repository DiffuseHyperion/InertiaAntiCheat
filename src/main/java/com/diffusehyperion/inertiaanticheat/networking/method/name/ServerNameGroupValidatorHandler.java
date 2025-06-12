package com.diffusehyperion.inertiaanticheat.networking.method.name;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.networking.method.name.handlers.NameValidationHandler;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.util.HashAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerNameGroupValidatorHandler extends NameValidationHandler {
    public ServerNameGroupValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    protected boolean validateMods(List<String> modlist) {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Checking modlist now, using group method");

        List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.group.softWhitelist");
        softWhitelistedMods.replaceAll((mod) -> (mod.endsWith(".jar") ? mod : mod + ".jar"));
        InertiaAntiCheat.debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));

        List<String> hashes = new ArrayList<>();
        List<String> copySoftWhitelistedMods = new ArrayList<>(softWhitelistedMods);
        for (String mod : modlist) {
            if (copySoftWhitelistedMods.contains(mod)) {
                copySoftWhitelistedMods.remove(mod);
            } else {
                hashes.add(mod);
            }
        }
        Collections.sort(hashes);
        String combinedHash = String.join("|", hashes);
        String finalHash = InertiaAntiCheat.getHash(combinedHash.getBytes(), HashAlgorithm.MD5); // no need to be cryptographically safe here
        InertiaAntiCheat.debugInfo("Final hash: " + finalHash);
        InertiaAntiCheat.debugInfo("Combined hash: " + combinedHash);


        boolean success = InertiaAntiCheatServer.serverConfig.getList("validation.group.hash").contains(finalHash);
        if (success) {
            InertiaAntiCheat.debugInfo("Passed");
        } else {
            InertiaAntiCheat.debugInfo("Failed");
        }
        InertiaAntiCheat.debugLine2();
        return success;
    }
}
