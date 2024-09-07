package me.diffusehyperion.inertiaanticheat.server.adaptors;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

public abstract class ServerModlistValidatorAdaptor {
    protected final KeyPair keyPair;
    protected final Identifier modTransferID;
    public final CompletableFuture<Void> future;

    public ServerModlistValidatorAdaptor(KeyPair keyPair, Identifier modTransferID) {
        this.keyPair = keyPair;
        this.modTransferID = modTransferID;
        this.future = new CompletableFuture<>();
    }

    abstract void startModTransfer(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler,
                                   boolean b, PacketByteBuf packetByteBuf,
                                   ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender);
}
