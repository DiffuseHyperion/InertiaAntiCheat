package com.diffusehyperion.inertiaanticheat.client;

import com.diffusehyperion.inertiaanticheat.client.networking.method.TransferHandler;
import com.diffusehyperion.inertiaanticheat.client.networking.method.data.ClientDataTransferHandler;
import com.diffusehyperion.inertiaanticheat.client.networking.method.id.ClientIdTransferHandler;
import com.diffusehyperion.inertiaanticheat.client.networking.method.name.ClientNameTransferHandler;
import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.interfaces.UpgradedClientLoginNetworkHandler;
import com.diffusehyperion.inertiaanticheat.common.util.InertiaAntiCheatConstants;
import com.diffusehyperion.inertiaanticheat.server.networking.method.CheckingTypes;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient.debugInfo;
import static com.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient.debugLine;

public class ClientLoginModlistTransferHandler {
    private PublicKey serverPublicKey;
    private KeyPair clientKeyPair;

    public static void init() {
        debugInfo("Creating mod transfer handler");
        ClientLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.CHECK_CONNECTION, ClientLoginModlistTransferHandler::confirmConnection);
    }

    /**
     * Responds to any connection check packets
     * This also creates an instance of this class and begins listening for key exchange requests.
     */
    private static CompletableFuture<@Nullable PacketByteBuf>
    confirmConnection(MinecraftClient client, ClientLoginNetworkHandler handler,
                      PacketByteBuf buf, Consumer<ChannelFutureListener> callbacksConsumer) {
        UpgradedClientLoginNetworkHandler upgradedHandler = (UpgradedClientLoginNetworkHandler) handler;

        debugLine();
        debugInfo("Received request to start mod transfer");

        upgradedHandler.inertiaAntiCheat$getSecondaryStatusConsumer().accept(Text.of("Preparing mod transfer..."));

        ClientLoginModlistTransferHandler transferHandler = new ClientLoginModlistTransferHandler();
        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.INITIATE_E2EE, transferHandler::exchangeKey);
        return CompletableFuture.completedFuture(PacketByteBufs.empty());
    }

    /**
     * Responds to key exchange requests
     * Saves server's public key and generates a client keypair to send
     */
    private CompletableFuture<@Nullable PacketByteBuf>
    exchangeKey(MinecraftClient client, ClientLoginNetworkHandler handler,
                PacketByteBuf buf, Consumer<ChannelFutureListener> callbacksConsumer) {
        UpgradedClientLoginNetworkHandler upgradedHandler = (UpgradedClientLoginNetworkHandler) handler;

        debugInfo("Exchanging keys with server");

        upgradedHandler.inertiaAntiCheat$getSecondaryStatusConsumer().accept(Text.of("Transferring keys..."));

        this.serverPublicKey = InertiaAntiCheat.retrievePublicKey(buf);

        PacketByteBuf responseBuf = PacketByteBufs.create();
        this.clientKeyPair = InertiaAntiCheat.createRSAPair();
        responseBuf.writeBytes(this.clientKeyPair.getPublic().getEncoded());

        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.SET_ADAPTOR, this::createAdaptors);
        return CompletableFuture.completedFuture(responseBuf);
    }

    /**
     * Responds to server's chosen adaptor and creates appropriate instances
     */
    private CompletableFuture<@Nullable PacketByteBuf>
    createAdaptors(MinecraftClient client, ClientLoginNetworkHandler handler,
                PacketByteBuf buf, Consumer<ChannelFutureListener> callbacksConsumer) {
        UpgradedClientLoginNetworkHandler upgradedHandler = (UpgradedClientLoginNetworkHandler) handler;

        upgradedHandler.inertiaAntiCheat$getSecondaryStatusConsumer().accept(Text.of("Starting for mod transfer..."));

        int transferAdaptorIndex = buf.readInt();
        Consumer<Text> secondaryStatusConsumer = upgradedHandler.inertiaAntiCheat$getSecondaryStatusConsumer();
        debugInfo("Received adapter index of " + transferAdaptorIndex);

        CheckingTypes transferAdaptorType = CheckingTypes.values()[transferAdaptorIndex];

        TransferHandler transferAdaptor = switch (transferAdaptorType) {
            case DATA -> new ClientDataTransferHandler(this.serverPublicKey, InertiaAntiCheatConstants.SEND_MOD, secondaryStatusConsumer);
            case NAME -> new ClientNameTransferHandler(this.serverPublicKey, InertiaAntiCheatConstants.SEND_MOD, secondaryStatusConsumer);
            case ID -> new ClientIdTransferHandler(this.serverPublicKey, InertiaAntiCheatConstants.SEND_MOD, secondaryStatusConsumer);
        };

        ClientLoginConnectionEvents.DISCONNECT.register(transferAdaptor::onDisconnect);

        debugInfo("Registered new handler for channel");
        debugLine();

        return CompletableFuture.completedFuture(PacketByteBufs.empty());
    }
}
