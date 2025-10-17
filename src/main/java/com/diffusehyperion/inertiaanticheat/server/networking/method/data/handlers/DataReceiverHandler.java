package com.diffusehyperion.inertiaanticheat.server.networking.method.data.handlers;

import com.diffusehyperion.inertiaanticheat.server.networking.method.ReceiverHandler;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.security.KeyPair;

public abstract class DataReceiverHandler extends ReceiverHandler {
    protected final DataValidationHandler validator;

    public DataReceiverHandler(KeyPair keyPair, Identifier modTransferID, ServerLoginNetworkHandler handler, DataValidationHandler validator) {
        super(keyPair, modTransferID, handler);
        this.validator = validator;
    }
}
