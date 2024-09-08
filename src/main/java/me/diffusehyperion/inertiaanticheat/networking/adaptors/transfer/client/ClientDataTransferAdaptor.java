package me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.client;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientDataTransferAdaptor extends ClientModlistTransferAdaptor{
    private final int maxIndex;
    private int currentIndex;
    private byte[] currentFile;

    public ClientDataTransferAdaptor(PublicKey publicKey, Identifier modTransferID) {
        super(publicKey, modTransferID);
        this.maxIndex = InertiaAntiCheatClient.allModData.size();
        this.currentIndex = 0;
        this.currentFile = InertiaAntiCheatClient.allModData.get(currentIndex);
    }

    @Override
    public CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler handler, PacketByteBuf buf, Consumer<PacketCallbacks> callbacksConsumer) {
        InertiaAntiCheat.debugInfo("Sending mod " + this.currentIndex);
        if (this.currentIndex + 1 >= this.maxIndex && Objects.isNull(this.currentFile)) {
            throw new RuntimeException("Not expected to send anymore mods");
        }
        PacketByteBuf responseBuf = PacketByteBufs.create();

        int MAX_SIZE = 1000000;
        if (this.currentFile.length > MAX_SIZE) {
            InertiaAntiCheat.debugInfo("Sending part of next file");

            byte[] chunk = Arrays.copyOf(this.currentFile, MAX_SIZE);
            InertiaAntiCheat.debugInfo("Hash of chunk: " + InertiaAntiCheat.getHash(chunk, HashAlgorithm.MD5));

            byte[] encryptedAESFileData = InertiaAntiCheat.encryptAESBytes(chunk, this.secretKey);
            byte[] encryptedRSASecretKey = InertiaAntiCheat.encryptRSABytes(this.secretKey.getEncoded(), this.publicKey);
            responseBuf.writeBoolean(false);
            responseBuf.writeInt(encryptedRSASecretKey.length);
            responseBuf.writeBytes(encryptedRSASecretKey);
            responseBuf.writeBytes(encryptedAESFileData);

            this.currentFile = Arrays.copyOfRange(this.currentFile, MAX_SIZE, this.currentFile.length);
        } else {
            InertiaAntiCheat.debugInfo("Sending entirety of next file");

            InertiaAntiCheat.debugInfo("Hash of chunk: " + InertiaAntiCheat.getHash(this.currentFile, HashAlgorithm.MD5));

            byte[] encryptedAESFileData = InertiaAntiCheat.encryptAESBytes(this.currentFile, this.secretKey);
            byte[] encryptedRSASecretKey = InertiaAntiCheat.encryptRSABytes(this.secretKey.getEncoded(), this.publicKey);
            responseBuf.writeBoolean(true);
            responseBuf.writeInt(encryptedRSASecretKey.length);
            responseBuf.writeBytes(encryptedRSASecretKey);
            responseBuf.writeBytes(encryptedAESFileData);

            this.currentFile = null;
            if (this.currentIndex + 1 < this.maxIndex) {
                loadNextFile();
            } else {
                ClientLoginNetworking.unregisterGlobalReceiver(this.modTransferID);
            }
        }
        InertiaAntiCheat.debugLine();
        return CompletableFuture.completedFuture(responseBuf);
    }

    private void loadNextFile() {
        InertiaAntiCheat.debugLine2();
        InertiaAntiCheat.debugInfo("Loading next file");

        if (this.currentIndex + 1 >= this.maxIndex) {
            throw new RuntimeException("No more mods to load");
        }
        this.currentIndex += 1;
        this.currentFile = InertiaAntiCheatClient.allModData.get(currentIndex);
        InertiaAntiCheat.debugLine2();
    }
}
