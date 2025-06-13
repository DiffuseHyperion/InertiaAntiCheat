package com.diffusehyperion.inertiaanticheat.networking.method.name;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import com.diffusehyperion.inertiaanticheat.networking.method.TransferHandler;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientNameTransferHandler extends TransferHandler {
    private final int maxIndex;
    private int currentIndex;

    public ClientNameTransferHandler(PublicKey publicKey, Identifier modTransferID, Consumer<Text> secondaryStatusConsumer) {
        super(publicKey, modTransferID, secondaryStatusConsumer, InertiaAntiCheatClient.allModNames.size());

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

            this.setCompleteTransferStatus();

            ClientLoginNetworking.unregisterGlobalReceiver(InertiaAntiCheatConstants.SEND_MOD);
            return CompletableFuture.completedFuture(null);
        }
        PacketByteBuf responseBuf = this.preparePacket(InertiaAntiCheatClient.allModNames.get(currentIndex).getBytes(StandardCharsets.UTF_8));

        this.increaseSentModsStatus();
        this.currentIndex++;

        InertiaAntiCheat.debugLine();
        return CompletableFuture.completedFuture(responseBuf);
    }
}
