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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientDataTransferHandler extends TransferHandler {

    private int allModPathsIndex;
    private final Deque<byte[]> loadedFiles;
    private static final int MAX_LOADED_FILES = 10;
    private boolean completed;
    private byte[] currentFile;

    public ClientDataTransferHandler(PublicKey publicKey, Identifier modTransferID) {
        super(publicKey, modTransferID);

        InertiaAntiCheat.debugInfo("Creating data transfer handler");

        this.completed = false;
        this.loadedFiles = new ArrayDeque<>(MAX_LOADED_FILES);
        this.allModPathsIndex = 0;

        while (loadedFiles.size() < MAX_LOADED_FILES && !this.completed) {
            loadNextFile();
        }
        stageNextFile();
    }

    @Override
    public CompletableFuture<PacketByteBuf> transferMod(MinecraftClient ignored1, ClientLoginNetworkHandler ignored2, PacketByteBuf ignored3, Consumer<PacketCallbacks> ignored4) {
        if (this.completed && this.loadedFiles.isEmpty() && Objects.isNull(currentFile)) {
            // All files have been sent, returning null to signify goodbye
            InertiaAntiCheat.debugInfo("Sending final packet");
            InertiaAntiCheat.debugLine();

            ClientLoginNetworking.unregisterGlobalReceiver(InertiaAntiCheatConstants.SEND_MOD);
            return CompletableFuture.completedFuture(null);
        } else if (Objects.isNull(currentFile)) {
            loadNextFile();
            stageNextFile();
        }

        // InertiaAntiCheat.debugInfo("Sending mod " + this.currentIndex);

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
        InertiaAntiCheat.debugInfo("Attempting to load mod file at index " + this.allModPathsIndex + " into memory");

        if (this.allModPathsIndex >= InertiaAntiCheatClient.allModPaths.size()) {
            if (this.completed) {
                throw new RuntimeException("Attempted to load next mod file after already declaring transfer as completed");
            }

            InertiaAntiCheat.debugInfo("All mod files have been loaded, no longer loading more files");

            this.completed = true;
            return;
        }

        Path path = InertiaAntiCheatClient.allModPaths.get(this.allModPathsIndex);
        this.allModPathsIndex++;

        InertiaAntiCheat.debugInfo("Current number of loaded mod files: " + this.loadedFiles.size());

        try {
            InertiaAntiCheat.debugInfo("Loading mod file: " + path);

            this.loadedFiles.addLast(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new RuntimeException("Could not read mod file at path: " + path, e);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Could not load mod file into deque as it was full", e);
        }
    }

    private void stageNextFile() {
        InertiaAntiCheat.debugInfo("Staging next mod file from memory");

        try {
            this.currentFile = this.loadedFiles.remove();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Could not stage next mod file as deque was empty", e);
        }
    }
}
