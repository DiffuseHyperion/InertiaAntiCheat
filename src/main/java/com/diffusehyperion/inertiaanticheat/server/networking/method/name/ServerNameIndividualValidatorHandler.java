package com.diffusehyperion.inertiaanticheat.server.networking.method.name;

import com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import com.diffusehyperion.inertiaanticheat.server.networking.method.name.handlers.NameValidationHandler;

import java.util.List;

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.*;

public class ServerNameIndividualValidatorHandler extends NameValidationHandler {
    public ServerNameIndividualValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    protected boolean validateMods(List<String> modlist) {
        debugLine2();
        debugInfo("Checking modlist now, using individual method");
        debugInfo("Mod list size: " + modlist.size());

        List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.individual.blacklist");
        blacklistedMods.replaceAll((mod) -> (mod.endsWith(".jar") ? mod : mod + ".jar"));
        debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));

        List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("validation.individual.whitelist");
        whitelistedMods.replaceAll((mod) -> (mod.endsWith(".jar") ? mod : mod + ".jar"));
        debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));

        debugLine();
        for (String mod : modlist) {
            if (blacklistedMods.contains(mod)) {
                debugInfo("Found in blacklist");
                debugLine();
                return false;
            }
            if (whitelistedMods.contains(mod)) {
                debugInfo("Found in whitelist");
                debugLine();
                whitelistedMods.remove(mod);
            }
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
