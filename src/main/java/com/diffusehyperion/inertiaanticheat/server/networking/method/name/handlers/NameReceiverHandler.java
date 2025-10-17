package com.diffusehyperion.inertiaanticheat.server.networking.method.name.handlers;

import com.diffusehyperion.inertiaanticheat.server.networking.method.ReceiverHandler;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.security.KeyPair;

public abstract class NameReceiverHandler extends ReceiverHandler {
    protected final NameValidationHandler validator;

    public NameReceiverHandler(KeyPair keyPair, Identifier modTransferID, ServerLoginNetworkHandler handler, NameValidationHandler validator) {
        super(keyPair, modTransferID, handler);
        this.validator = validator;
    }
}