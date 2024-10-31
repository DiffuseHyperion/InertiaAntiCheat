package me.diffusehyperion.inertiaanticheat.networking.method.name;

import me.diffusehyperion.inertiaanticheat.networking.method.name.handlers.NameValidationHandler;

import java.util.List;

public class ServerNameIndividualValidatorHandler extends NameValidationHandler {
    public ServerNameIndividualValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        super(failureTask, successTask, finishTask);
    }

    @Override
    protected boolean validateMods(List<String> modlist) {
        return false;
    }
}
