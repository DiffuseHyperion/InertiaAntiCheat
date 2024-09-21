package me.diffusehyperion.inertiaanticheat.networking.adaptors.validator;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;

import java.util.concurrent.CompletableFuture;

public abstract class ServerModlistValidatorAdaptor {
    public final CompletableFuture<Void> future;
    public final Runnable failureTask;
    public final Runnable successTask;
    public final Runnable finishTask;

    public ServerModlistValidatorAdaptor(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        this.failureTask = failureTask;
        this.successTask = successTask;
        this.finishTask = finishTask;
        this.future = new CompletableFuture<>();
    }

    abstract boolean validateModlist();

    public void checkModlist() {
        InertiaAntiCheat.debugInfo("Finishing transfer, checking mods now");
        if (!validateModlist()) {
            failureTask.run();
        } else {
            successTask.run();
        }

        finishTask.run();
        this.future.complete(null);
        InertiaAntiCheat.debugLine();
    }
}
