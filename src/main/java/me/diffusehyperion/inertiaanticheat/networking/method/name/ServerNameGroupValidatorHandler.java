package me.diffusehyperion.inertiaanticheat.networking.method.name;

import me.diffusehyperion.inertiaanticheat.networking.method.name.handlers.NameValidationHandler;

import java.util.List;

public class ServerNameGroupValidatorHandler extends NameValidationHandler {
    public ServerNameGroupValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    protected boolean validateMods(List<String> modlist) {
        return false;
    }
}
