package me.diffusehyperion.inertiaanticheat.networking.adaptors.validator;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;

import java.util.List;

public class IndividualValidatorAdaptor extends ServerModlistValidatorAdaptor{

    public IndividualValidatorAdaptor(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    public boolean validateModlist() {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Checking modlist now, using individual method");
        InertiaAntiCheat.debugInfo("Mod list size: " + this.collectedMods.size());
        List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.blacklist");
        InertiaAntiCheat.debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));
        List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.whitelist");
        InertiaAntiCheat.debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));
        InertiaAntiCheat.debugLine();
        for (byte[] mod : this.collectedMods) {
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
