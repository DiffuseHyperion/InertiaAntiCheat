package com.diffusehyperion.inertiaanticheat.networking.method.id.handlers;

import com.diffusehyperion.inertiaanticheat.networking.method.ReceiverHandler;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.security.KeyPair;

public abstract class IdReceiverHandler extends ReceiverHandler {
    protected final IdValidationHandler validator;

    public IdReceiverHandler(KeyPair keyPair, Identifier modTransferID, ServerLoginNetworkHandler handler, IdValidationHandler validator) {
        super(keyPair, modTransferID, handler);
        this.validator = validator;
    }
}