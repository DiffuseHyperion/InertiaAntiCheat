package com.diffusehyperion.inertiaanticheat.networking.method.name.handlers;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.networking.method.ValidatorHandler;

import java.util.List;

public abstract class NameValidationHandler extends ValidatorHandler {

    public NameValidationHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    protected abstract boolean validateMods(List<String> modlist);

    public void checkModlist(List<String> modlist) {
        InertiaAntiCheat.debugInfo("Finishing transfer, checking mods now");
        if (!validateMods(modlist)) {
            failureTask.run();
        } else {
            successTask.run();
        }

        finishTask.run();
        this.future.complete(null);
        InertiaAntiCheat.debugLine();
    }
}
