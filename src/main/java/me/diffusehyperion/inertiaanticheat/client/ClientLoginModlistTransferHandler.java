package me.diffusehyperion.inertiaanticheat.client;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientLoginModlistTransferHandler {
    public static void init() {
        InertiaAntiCheat.debugInfo("Creating mod transfer handler");
        ClientLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.MOD_TRANSFER_START_ID, ClientLoginModlistTransferHandler::startModTransfer);
    }

    private static CompletableFuture<PacketByteBuf> startModTransfer(MinecraftClient client, ClientLoginNetworkHandler clientLoginNetworkHandler, PacketByteBuf packetByteBuf, Consumer<GenericFutureListener<? extends Future<? super Void>>> genericFutureListenerConsumer) {
        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Received request to start mod transfer");

        Identifier modTransferID = new Identifier("inertiaanticheat", packetByteBuf.readString());
        PublicKey publicKey = InertiaAntiCheat.retrievePublicKey(packetByteBuf);
        InertiaAntiCheat.debugInfo("Transfer to be done on channel path: " + modTransferID.getPath());

        ClientLoginModlistTransferHandler handler = new ClientLoginModlistTransferHandler(publicKey, InertiaAntiCheatClient.allModData.size(), modTransferID);
        ClientLoginNetworking.registerGlobalReceiver(modTransferID, handler::transferMod);
        InertiaAntiCheat.debugInfo("Registered new handler for channel");

        PacketByteBuf responseBuf = PacketByteBufs.create();
        responseBuf.writeInt(InertiaAntiCheatClient.allModData.size()); //TODO: encrypt this
        InertiaAntiCheat.debugInfo("Responding with mod size of " + InertiaAntiCheatClient.allModData.size());
        InertiaAntiCheat.debugLine();

        return CompletableFuture.completedFuture(responseBuf);
    }

    private final PublicKey publicKey;
    private final SecretKey secretKey;
    private final Identifier modTransferID;

    private final int maxIndex;
    private int currentIndex = 0;
    private final int MAX_SIZE = 1000000;
    private byte[] currentFile;


    public ClientLoginModlistTransferHandler(PublicKey publicKey, int maxIndex, Identifier modTransferID) {
        this.publicKey = publicKey;
        this.secretKey = InertiaAntiCheat.createAESKey();
        this.modTransferID = modTransferID;

        this.maxIndex = maxIndex;
        this.currentFile = InertiaAntiCheatClient.allModData.get(currentIndex);

        ClientLoginConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ClientLoginNetworkHandler clientLoginNetworkHandler, MinecraftClient minecraftClient) {
        ClientLoginNetworking.unregisterGlobalReceiver(this.modTransferID);
    }


    private CompletableFuture<PacketByteBuf> transferMod(MinecraftClient client, ClientLoginNetworkHandler clientLoginNetworkHandler, PacketByteBuf packetByteBuf, Consumer<GenericFutureListener<? extends Future<? super Void>>> genericFutureListenerConsumer) {
        InertiaAntiCheat.debugInfo("Sending mod " + this.currentIndex);
        if (this.currentIndex + 1 >= this.maxIndex && Objects.isNull(this.currentFile)) {
            throw new RuntimeException("Not expected to send anymore mods");
        }
        PacketByteBuf responseBuf = PacketByteBufs.create();

        if (this.currentFile.length > MAX_SIZE) {
            InertiaAntiCheat.debugInfo("Sending part of next file");

            byte[] chunk = Arrays.copyOf(this.currentFile, this.MAX_SIZE);

            byte[] encryptedAESFileData = InertiaAntiCheat.encryptAESBytes(chunk, this.secretKey);
            byte[] encryptedRSASecretKey = InertiaAntiCheat.encryptRSABytes(this.secretKey.getEncoded(), this.publicKey);
            responseBuf.writeBoolean(false);
            responseBuf.writeInt(encryptedRSASecretKey.length);
            responseBuf.writeBytes(encryptedRSASecretKey);
            responseBuf.writeBytes(encryptedAESFileData);

            this.currentFile = Arrays.copyOfRange(this.currentFile, this.MAX_SIZE, this.currentFile.length);
        } else {
            InertiaAntiCheat.debugInfo("Sending entirety of next file");

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
