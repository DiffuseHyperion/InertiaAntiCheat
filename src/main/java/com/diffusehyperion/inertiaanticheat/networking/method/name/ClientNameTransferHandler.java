package com.diffusehyperion.inertiaanticheat.networking.method.name;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import com.diffusehyperion.inertiaanticheat.networking.method.TransferHandler;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientNameTransferHandler extends TransferHandler {
    private final int maxIndex;
    private int currentIndex;

    public ClientNameTransferHandler(PublicKey publicKey, Identifier modTransferID) {
        super(publicKey, modTransferID);

        InertiaAntiCheat.debugInfo("Creating name transfer handler");

        this.maxIndex = InertiaAntiCheatClient.allModNames.size();
        this.currentIndex = 0;
    }

    @Override
    public CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugInfo("Sending mod " + this.currentIndex);

        if (this.currentIndex >= this.maxIndex) {
            // All files have been sent, returning null to signify goodbye
            InertiaAntiCheat.debugInfo("Sending final packet");
            InertiaAntiCheat.debugLine();

            ClientLoginNetworking.unregisterGlobalReceiver(InertiaAntiCheatConstants.SEND_MOD);
            return CompletableFuture.completedFuture(null);
        }
        SecretKey secretKey = InertiaAntiCheat.createAESKey();
        PacketByteBuf responseBuf = PacketByteBufs.create();

        byte[] encryptedAESNameData = InertiaAntiCheat.encryptAESBytes(
                InertiaAntiCheatClient.allModNames.get(currentIndex).getBytes(StandardCharsets.UTF_8), secretKey);
        byte[] encryptedRSASecretKey = InertiaAntiCheat.encryptRSABytes(secretKey.getEncoded(), this.publicKey);
        responseBuf.writeInt(encryptedRSASecretKey.length);
        responseBuf.writeBytes(encryptedRSASecretKey);
        responseBuf.writeBytes(encryptedAESNameData);

        this.currentIndex++;

        InertiaAntiCheat.debugLine();
        return CompletableFuture.completedFuture(responseBuf);
    }
}
