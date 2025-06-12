package com.diffusehyperion.inertiaanticheat.networking.method.name;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.networking.method.name.handlers.NameValidationHandler;
import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;

import java.util.List;

public class ServerNameIndividualValidatorHandler extends NameValidationHandler {
    public ServerNameIndividualValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    protected boolean validateMods(List<String> modlist) {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Checking modlist now, using individual method");
        InertiaAntiCheat.debugInfo("Mod list size: " + modlist.size());

        List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.individual.blacklist");
        blacklistedMods.replaceAll((mod) -> (mod.endsWith(".jar") ? mod : mod + ".jar"));
        InertiaAntiCheat.debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));

        List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.individual.whitelist");
        whitelistedMods.replaceAll((mod) -> (mod.endsWith(".jar") ? mod : mod + ".jar"));
        InertiaAntiCheat.debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));

        InertiaAntiCheat.debugLine();
        for (String mod : modlist) {
            if (blacklistedMods.contains(mod)) {
                InertiaAntiCheat.debugInfo("Found in blacklist");
                InertiaAntiCheat.debugLine();
                return false;
            }
            if (whitelistedMods.contains(mod)) {
                InertiaAntiCheat.debugInfo("Found in whitelist");
                InertiaAntiCheat.debugLine();
                whitelistedMods.remove(mod);
            }
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
