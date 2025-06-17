package com.diffusehyperion.inertiaanticheat.networking.method;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class TransferHandler {
    protected final PublicKey publicKey;
    protected final Identifier modTransferID;
    protected final Consumer<Text> secondaryStatusConsumer;

    private int sentMods;
    private final int totalMods;
    
    public TransferHandler(PublicKey publicKey, Identifier modTransferID, Consumer<Text> secondaryStatusConsumer, int totalMods) {
        this.publicKey = publicKey;
        this.modTransferID = modTransferID;
        this.secondaryStatusConsumer = secondaryStatusConsumer;

        this.sentMods = 0;
        this.totalMods = totalMods;
        this.updateSecondaryStatus("Sent 0/" + totalMods + " mods");

        ClientLoginNetworking.registerReceiver(InertiaAntiCheatConstants.SEND_MOD, this::transferMod);
    }

    protected abstract CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buf, Consumer<ChannelFutureListener> callbacksConsumer);

    public void onDisconnect(ClientLoginNetworkHandler ignored1, MinecraftClient ignored2) {
        ClientLoginNetworking.unregisterReceiver(this.modTransferID);
    }

    protected PacketByteBuf preparePacket(byte[] data) {
        PacketByteBuf buf = PacketByteBufs.create();

        return this.preparePacket(buf, data);
    }

    protected PacketByteBuf preparePacket(PacketByteBuf buf, byte[] data) {
        SecretKey secretKey = InertiaAntiCheat.createAESKey();

        byte[] encryptedRSASecretKey = InertiaAntiCheat.encryptRSABytes(secretKey.getEncoded(), this.publicKey);
        byte[] encryptedAESNameData = InertiaAntiCheat.encryptAESBytes(data, secretKey);
        buf.writeInt(encryptedRSASecretKey.length);
        buf.writeBytes(encryptedRSASecretKey);
        buf.writeBytes(encryptedAESNameData);

        return buf;
    }

    protected void setCompleteTransferStatus() {
        this.secondaryStatusConsumer.accept(Text.of("Waiting for validation..."));
    }

    protected void increaseSentModsStatus() {
        this.sentMods++;
        this.updateSecondaryStatus("Sent " + this.sentMods + "/" + this.totalMods + " mods");
    }

    private void updateSecondaryStatus(String message) {
        this.secondaryStatusConsumer.accept(Text.of(message));
    }
}
