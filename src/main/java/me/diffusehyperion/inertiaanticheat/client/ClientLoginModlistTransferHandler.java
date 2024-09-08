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
    public static void init() {
        InertiaAntiCheat.debugInfo("Creating mod transfer handler");
        ClientLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.CHECK_CONNECTION, ClientLoginModlistTransferHandler::confirmConnection);

    }

    private static CompletableFuture<@Nullable PacketByteBuf>
    confirmConnection(MinecraftClient client, ClientLoginNetworkHandler handler,
                      PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Received request to start mod transfer");

        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.INITIATE_E2EE, ClientLoginModlistTransferHandler::exchangeKey);
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<@Nullable PacketByteBuf>
    exchangeKey(MinecraftClient client, ClientLoginNetworkHandler loginNetworkHandler,
                PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugInfo("Exchanging keys with server");

        PacketByteBuf responseBuf = PacketByteBufs.create();
        KeyPair keyPair = InertiaAntiCheat.createRSAPair();
        responseBuf.writeBytes(keyPair.getPublic().getEncoded());

        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.SET_ADAPTOR, ClientLoginModlistTransferHandler::setAdaptor);
        return CompletableFuture.completedFuture(responseBuf);
    }

    private static CompletableFuture<@Nullable PacketByteBuf>
    setAdaptor(MinecraftClient client, ClientLoginNetworkHandler loginNetworkHandler,
                PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {

        TransferAdaptors transferAdaptorIndex = TransferAdaptors.values()[buf.readInt()];
        PublicKey publicKey = InertiaAntiCheat.retrievePublicKey(buf);

        ClientModlistTransferAdaptor transferAdaptor;
        switch (transferAdaptorIndex) {
            case DATA -> transferAdaptor = new ClientDataTransferAdaptor(publicKey, InertiaAntiCheatConstants.SET_ADAPTOR);
            case NAME -> transferAdaptor = new ClientNameTransferAdaptor(publicKey, InertiaAntiCheatConstants.SET_ADAPTOR);
            default -> throw new IllegalStateException("Unexpected value: " + transferAdaptorIndex);
        }

        ClientLoginConnectionEvents.DISCONNECT.register(transferAdaptor::onDisconnect);

        InertiaAntiCheat.debugInfo("Registered new handler for channel");

        PacketByteBuf responseBuf = PacketByteBufs.create();
        responseBuf.writeBytes(InertiaAntiCheat.encryptRSABytes(BigInteger.valueOf(InertiaAntiCheatClient.allModData.size()).toByteArray(), publicKey));
        InertiaAntiCheat.debugInfo("Responding with mod size of " + InertiaAntiCheatClient.allModData.size());
        InertiaAntiCheat.debugLine();

        return CompletableFuture.completedFuture(responseBuf);
    }


}
