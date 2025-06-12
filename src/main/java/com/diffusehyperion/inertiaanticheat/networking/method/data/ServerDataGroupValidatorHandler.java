package com.diffusehyperion.inertiaanticheat.networking.method.data;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.networking.method.data.handlers.DataValidationHandler;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.util.HashAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerDataGroupValidatorHandler extends DataValidationHandler {
    public ServerDataGroupValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    public boolean validateMods(List<byte[]> modlist) {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Checking modlist now, using group method");
        List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.group.softWhitelist");
        InertiaAntiCheat.debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));
        List<String> hashes = new ArrayList<>();
        List<String> copySoftWhitelistedMods = new ArrayList<>(softWhitelistedMods);
        for (byte[] mod : modlist) {
            String fileHash = InertiaAntiCheat.getHash(mod, InertiaAntiCheatServer.hashAlgorithm);
            if (copySoftWhitelistedMods.contains(fileHash)) {
                copySoftWhitelistedMods.remove(fileHash);
            } else {
                hashes.add(fileHash);
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
