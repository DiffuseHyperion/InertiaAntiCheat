package com.diffusehyperion.inertiaanticheat.networking.method;

import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class TransferHandler {
    protected final PublicKey publicKey;
    protected final Identifier modTransferID;

    public TransferHandler(PublicKey publicKey, Identifier modTransferID) {
        this.publicKey = publicKey;
        this.modTransferID = modTransferID;

        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.SEND_MOD, this::transferMod);
    }

    protected abstract CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer);

    public void onDisconnect(ClientLoginNetworkHandler ignored1, MinecraftClient ignored2) {
        ClientLoginNetworking.unregisterReceiver(this.modTransferID);
    }
}
