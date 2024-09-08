package me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.client;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import java.security.PublicKey;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientNameTransferAdaptor extends ClientModlistTransferAdaptor {
    private final int maxIndex;
    private int currentIndex;
    private String currentName;

    public ClientNameTransferAdaptor(PublicKey publicKey, Identifier modTransferID) {
        super(publicKey, modTransferID);
        this.maxIndex = InertiaAntiCheatClient.allModNames.size();
        this.currentIndex = 0;
        this.currentName = InertiaAntiCheatClient.allModNames.get(currentIndex);
    }

    @Override
    public CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugInfo("Sending mod " + this.currentIndex);

        if (this.currentIndex + 1 >= this.maxIndex && Objects.isNull(this.currentName)) {
            throw new RuntimeException("Not expected to send anymore mods");
        }

        PacketByteBuf responseBuf = PacketByteBufs.create();
        responseBuf.writeString(this.currentName);

        this.currentIndex++;
        this.currentName = InertiaAntiCheatClient.allModNames.get(currentIndex);

        InertiaAntiCheat.debugLine();
        return CompletableFuture.completedFuture(responseBuf);
    }
}
