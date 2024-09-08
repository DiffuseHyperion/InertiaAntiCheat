package me.diffusehyperion.inertiaanticheat.networking.adaptors.validator;

import java.util.concurrent.CompletableFuture;

public abstract class ServerModlistValidatorAdaptor {
    public final CompletableFuture<Void> future;

    public ServerModlistValidatorAdaptor() {
        this.future = new CompletableFuture<>();
    }

    abstract boolean checkModlist();
}
