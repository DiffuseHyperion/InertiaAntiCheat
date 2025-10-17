package com.diffusehyperion.inertiaanticheat.server.networking.method.name.handlers;

import com.diffusehyperion.inertiaanticheat.server.networking.method.ValidatorHandler;

import java.util.List;

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugInfo;
import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugLine;

public abstract class NameValidationHandler extends ValidatorHandler {

    public NameValidationHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    protected abstract boolean validateMods(List<String> modlist);

    public void checkModlist(List<String> modlist) {
        debugInfo("Finishing transfer, checking mods now");
        if (!validateMods(modlist)) {
            failureTask.run();
        } else {
            successTask.run();
        }

        finishTask.run();
        this.future.complete(null);
        debugLine();
    }
}
