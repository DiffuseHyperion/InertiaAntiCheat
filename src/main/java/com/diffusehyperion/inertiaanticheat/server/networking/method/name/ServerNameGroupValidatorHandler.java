package com.diffusehyperion.inertiaanticheat.server.networking.method.name;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.util.HashAlgorithm;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.server.networking.method.name.handlers.NameValidationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugInfo;
import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugLine2;

public class ServerNameGroupValidatorHandler extends NameValidationHandler {
    public ServerNameGroupValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    protected boolean validateMods(List<String> modlist) {
        debugLine2();
        debugInfo("Checking modlist now, using group method");

        List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.group.softWhitelist");
        softWhitelistedMods.replaceAll((mod) -> (mod.endsWith(".jar") ? mod : mod + ".jar"));
        debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));

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
        debugInfo("Final hash: " + finalHash);
        debugInfo("Combined hash: " + combinedHash);


        boolean success = InertiaAntiCheatServer.serverConfig.getList("validation.group.hash").contains(finalHash);
        if (success) {
            debugInfo("Passed");
        } else {
            debugInfo("Failed");
        }
        debugLine2();
        return success;
    }
}
