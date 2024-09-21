package me.diffusehyperion.inertiaanticheat.client;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.TransferAdaptors;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.client.ClientModlistTransferAdaptor;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.client.ClientDataTransferAdaptor;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.client.ClientNameTransferAdaptor;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientLoginModlistTransferHandler {
    private PublicKey serverKey;
    private KeyPair clientKeyPair;

    public static void init() {
        InertiaAntiCheat.debugInfo("Creating mod transfer handler");
        ClientLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.CHECK_CONNECTION, ClientLoginModlistTransferHandler::confirmConnection);
    }

    /**
     * Responds to any connection check packets
     * This also creates an instance of this class and begins listening for key exchange requests.
     */
    private static CompletableFuture<@Nullable PacketByteBuf>
    confirmConnection(MinecraftClient client, ClientLoginNetworkHandler handler,
                      PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Received request to start mod transfer");

        ClientLoginModlistTransferHandler transferHandler = new ClientLoginModlistTransferHandler();
        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.INITIATE_E2EE, transferHandler::exchangeKey);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Responds to key exchange requests
     * Saves server's public key and generates a client keypair to send
     */
    private CompletableFuture<@Nullable PacketByteBuf>
    exchangeKey(MinecraftClient client, ClientLoginNetworkHandler loginNetworkHandler,
                PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugInfo("Exchanging keys with server");

        this.serverKey = InertiaAntiCheat.retrievePublicKey(buf);

        PacketByteBuf responseBuf = PacketByteBufs.create();
        this.clientKeyPair = InertiaAntiCheat.createRSAPair();
        responseBuf.writeBytes(this.clientKeyPair.getPublic().getEncoded());

        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.SET_ADAPTOR, this::createAdaptors);
        return CompletableFuture.completedFuture(responseBuf);
    }

    /**
     * Responds to server's chosen adaptor and creates appropriate instances
     *
     */
    private CompletableFuture<@Nullable PacketByteBuf>
    createAdaptors(MinecraftClient client, ClientLoginNetworkHandler loginNetworkHandler,
                PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {

        TransferAdaptors transferAdaptorIndex = TransferAdaptors.values()[buf.readInt()];
        PublicKey publicKey = InertiaAntiCheat.retrievePublicKey(buf);

        ClientModlistTransferAdaptor transferAdaptor;
        switch (transferAdaptorIndex) {
            case DATA -> transferAdaptor = new ClientDataTransferAdaptor(publicKey, InertiaAntiCheatConstants.SEND_MOD);
            case NAME -> transferAdaptor = new ClientNameTransferAdaptor(publicKey, InertiaAntiCheatConstants.SEND_MOD);
            default -> throw new IllegalStateException("Unexpected value: " + transferAdaptorIndex);
        }

        ClientLoginConnectionEvents.DISCONNECT.register(transferAdaptor::onDisconnect);

        InertiaAntiCheat.debugInfo("Registered new handler for channel");

        return CompletableFuture.completedFuture(PacketByteBufs.empty());
    }


}
