package me.diffusehyperion.inertiaanticheat.networking.adaptors.validator;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupValidatorAdaptor extends ServerModlistValidatorAdaptor{

    @Override
    public boolean checkModlist() {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Checking modlist now, using group method");
        List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.group.softWhitelist");
        InertiaAntiCheat.debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));
        List<String> hashes = new ArrayList<>();
        List<String> copySoftWhitelistedMods = new ArrayList<>(softWhitelistedMods);
        for (byte[] mod : this.collectedMods) {
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


        boolean success = InertiaAntiCheatServer.serverConfig.getList("mods.group.hash").contains(finalHash);
        if (success) {
            InertiaAntiCheat.debugInfo("Passed");
        } else {
            InertiaAntiCheat.debugInfo("Failed");
        }
        InertiaAntiCheat.debugLine2();
        return success;
    }
}
