package me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.client;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class ClientModlistTransferAdaptor {
    protected final PublicKey publicKey;
    protected final SecretKey secretKey;
    protected final Identifier modTransferID;

    public ClientModlistTransferAdaptor(PublicKey publicKey, Identifier modTransferID) {
        this.publicKey = publicKey;
        this.secretKey = InertiaAntiCheat.createAESKey();
        this.modTransferID = modTransferID;
    }

    abstract CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer);

    public void onDisconnect(ClientLoginNetworkHandler clientLoginNetworkHandler, MinecraftClient minecraftClient) {
        ClientLoginNetworking.unregisterReceiver(this.modTransferID);
    }}
