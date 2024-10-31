package me.diffusehyperion.inertiaanticheat.networking.method.data;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import me.diffusehyperion.inertiaanticheat.networking.method.TransferHandler;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.util.Identifier;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientDataTransferHandler extends TransferHandler {
    private final int maxIndex;
    private int currentIndex;
    private byte[] currentFile;

    public ClientDataTransferHandler(PublicKey publicKey, Identifier modTransferID) {
        super(publicKey, modTransferID);

        this.maxIndex = InertiaAntiCheatClient.allModData.size();
        this.currentIndex = 0;
        this.currentFile = InertiaAntiCheatClient.allModData.get(currentIndex);
    }

    @Override
    public CompletableFuture<PacketByteBuf> transferMod(MinecraftClient ignored1, ClientLoginNetworkHandler ignored2, PacketByteBuf ignored3, Consumer<PacketCallbacks> ignored4) {
        if (this.currentIndex + 1 >= this.maxIndex && Objects.isNull(currentFile)) {
            // All files have been sent, returning null to signify goodbye
            InertiaAntiCheat.debugInfo("Sending final packet");
            InertiaAntiCheat.debugLine();

            ClientLoginNetworking.unregisterGlobalReceiver(InertiaAntiCheatConstants.SEND_MOD);
            return CompletableFuture.completedFuture(null);
        } else if (Objects.isNull(currentFile)) {
            loadNextFile();
        }

        InertiaAntiCheat.debugInfo("Sending mod " + this.currentIndex);

        PacketByteBuf responseBuf = PacketByteBufs.create();

        int MAX_SIZE = 1000000;
        SecretKey secretKey = InertiaAntiCheat.createAESKey();
        byte[] chunk;

        if (this.currentFile.length > MAX_SIZE) {
            InertiaAntiCheat.debugInfo("Sending part of next file");

            chunk = Arrays.copyOf(this.currentFile, MAX_SIZE);
            InertiaAntiCheat.debugInfo("Hash of chunk: " + InertiaAntiCheat.getHash(chunk, HashAlgorithm.MD5));

            this.currentFile = Arrays.copyOfRange(this.currentFile, MAX_SIZE, this.currentFile.length);
            responseBuf.writeBoolean(false);
        } else {
            InertiaAntiCheat.debugInfo("Sending entirety of next file");

            chunk = this.currentFile;
            InertiaAntiCheat.debugInfo("Hash of chunk: " + InertiaAntiCheat.getHash(this.currentFile, HashAlgorithm.MD5));

            this.currentFile = null;
            responseBuf.writeBoolean(true);
        }

        byte[] encryptedAESFileData = InertiaAntiCheat.encryptAESBytes(chunk, secretKey);
        byte[] encryptedRSASecretKey = InertiaAntiCheat.encryptRSABytes(secretKey.getEncoded(), this.publicKey);
        responseBuf.writeInt(encryptedRSASecretKey.length);
        responseBuf.writeBytes(encryptedRSASecretKey);
        responseBuf.writeBytes(encryptedAESFileData);

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
