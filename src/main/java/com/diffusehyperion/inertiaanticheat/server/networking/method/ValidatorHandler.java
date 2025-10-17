package com.diffusehyperion.inertiaanticheat.server.networking.method;

import java.util.concurrent.CompletableFuture;

public abstract class ValidatorHandler {
    public final CompletableFuture<Void> future;
    public final Runnable failureTask;
    public final Runnable successTask;
    public final Runnable finishTask;

    public ValidatorHandler(Runnable failureTask, Runnable successTask, Runnable finishTask) {
        this.failureTask = failureTask;
        this.successTask = successTask;
        this.finishTask = finishTask;
        this.future = new CompletableFuture<>();
    }
}
