package me.diffusehyperion.inertiaanticheat.client;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.adaptors.DataTransferAdaptor;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import javax.crypto.SecretKey;
import javax.xml.crypto.Data;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientLoginModlistTransferHandler {
    public static void init() {
        InertiaAntiCheat.debugInfo("Creating mod transfer handler");
        ClientLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.MOD_TRANSFER_START_ID, ClientLoginModlistTransferHandler::startModTransfer);
    }

    private static CompletableFuture<PacketByteBuf> startModTransfer(MinecraftClient client, ClientLoginNetworkHandler loginNetworkHandler, PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Received request to start mod transfer");

        PublicKey publicKey = InertiaAntiCheat.retrievePublicKey(buf);

        DataTransferAdaptor handler = new DataTransferAdaptor(publicKey, InertiaAntiCheatConstants.MOD_TRANSFER_CONTINUE_ID);
        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.MOD_TRANSFER_CONTINUE_ID, handler::transferMod);
        ClientLoginConnectionEvents.DISCONNECT.register(handler::onDisconnect);

        InertiaAntiCheat.debugInfo("Registered new handler for channel");

        PacketByteBuf responseBuf = PacketByteBufs.create();
        responseBuf.writeBytes(InertiaAntiCheat.encryptRSABytes(BigInteger.valueOf(InertiaAntiCheatClient.allModData.size()).toByteArray(), publicKey));
        InertiaAntiCheat.debugInfo("Responding with mod size of " + InertiaAntiCheatClient.allModData.size());
        InertiaAntiCheat.debugLine();

        return CompletableFuture.completedFuture(responseBuf);
    }


}
