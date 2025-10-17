package com.diffusehyperion.inertiaanticheat.server.networking.method.data;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.util.HashAlgorithm;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.handlers.DataValidationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugInfo;
import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugLine2;

public class ServerDataGroupValidatorHandler extends DataValidationHandler {
    public ServerDataGroupValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    public boolean validateMods(List<byte[]> modlist) {
        debugLine2();
        debugInfo("Checking modlist now, using group method");
        List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.group.softWhitelist");
        debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));
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
