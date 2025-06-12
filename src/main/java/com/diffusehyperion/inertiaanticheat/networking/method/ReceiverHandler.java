package com.diffusehyperion.inertiaanticheat.networking.method;

import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.security.KeyPair;

public abstract class ReceiverHandler {
    protected final KeyPair keyPair;
    protected final Identifier modTransferID;

    public ReceiverHandler(KeyPair keyPair, Identifier modTransferID, ServerLoginNetworkHandler handler) {
        this.keyPair = keyPair;
        this.modTransferID = modTransferID;

        ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.SEND_MOD, this::receiveMod);
    }

    protected abstract void receiveMod(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean b, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender);
}
