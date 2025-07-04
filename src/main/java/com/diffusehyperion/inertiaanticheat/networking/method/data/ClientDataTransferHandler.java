package com.diffusehyperion.inertiaanticheat.networking.method.data;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient;
import com.diffusehyperion.inertiaanticheat.networking.method.TransferHandler;
import com.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import io.netty.channel.ChannelFutureListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
    
    public static final int MAX_SIZE = 1000000;

    public ClientDataTransferHandler(PublicKey publicKey, Identifier modTransferID, Consumer<Text> secondaryStatusConsumer) {
        super(publicKey, modTransferID, secondaryStatusConsumer, InertiaAntiCheatClient.allModPaths.size());

        InertiaAntiCheat.debugInfo("Creating data transfer handler");

        this.completed = false;
        this.loadedFiles = new ArrayDeque<>(MAX_LOADED_FILES);
        this.allModPathsIndex = 0;

        Thread fileLoaderThread = new Thread(this::fileLoaderThreadMethod);
        fileLoaderThread.start();
    }

    @Override
    public CompletableFuture<PacketByteBuf> transferMod(MinecraftClient ignored1, ClientLoginNetworkHandler ignored2, PacketByteBuf ignored3, Consumer<ChannelFutureListener> ignored4) {
        if (this.completed && this.loadedFiles.isEmpty() && Objects.isNull(currentFile)) {
            // All files have been sent, returning null to signify goodbye
            InertiaAntiCheat.debugInfo("Sending final packet");
            InertiaAntiCheat.debugLine();

            this.setCompleteTransferStatus();

            ClientLoginNetworking.unregisterGlobalReceiver(InertiaAntiCheatConstants.SEND_MOD);
            return CompletableFuture.completedFuture(null);
        } else if (Objects.isNull(currentFile)) {
            this.increaseSentModsStatus();
            this.currentFile = stageNextFile();
        }
        
        PacketByteBuf buf = PacketByteBufs.create();
        byte[] chunk;

        if (this.currentFile.length > ClientDataTransferHandler.MAX_SIZE) {
            InertiaAntiCheat.debugInfo("Sending part of next file");

            chunk = Arrays.copyOf(this.currentFile, ClientDataTransferHandler.MAX_SIZE);
            InertiaAntiCheat.debugInfo("Hash of chunk: " + InertiaAntiCheat.getHash(chunk, HashAlgorithm.MD5));

            this.currentFile = Arrays.copyOfRange(this.currentFile, ClientDataTransferHandler.MAX_SIZE, this.currentFile.length);
            buf.writeBoolean(false);
        } else {
            InertiaAntiCheat.debugInfo("Sending entirety of next file");

            chunk = this.currentFile;
            InertiaAntiCheat.debugInfo("Hash of chunk: " + InertiaAntiCheat.getHash(this.currentFile, HashAlgorithm.MD5));

            this.currentFile = null;
            buf.writeBoolean(true);
        }
        PacketByteBuf responseBuf = this.preparePacket(buf, chunk);

        InertiaAntiCheat.debugLine();

        return CompletableFuture.completedFuture(responseBuf);
    }

    private synchronized void fileLoaderThreadMethod() {
        try {
            while (this.allModPathsIndex < InertiaAntiCheatClient.allModPaths.size()) {
                while (this.loadedFiles.size() >= MAX_LOADED_FILES) {
                    wait();
                }

                Path path = InertiaAntiCheatClient.allModPaths.get(this.allModPathsIndex);
                this.allModPathsIndex++;

                try {
                    this.loadedFiles.addLast(Files.readAllBytes(path));
                    InertiaAntiCheat.debugInfo("Loaded mod file: " + path);
                    notifyAll();
                } catch (IOException e) {
                    throw new RuntimeException("Could not read mod file at path: " + path, e);
                } catch (IllegalStateException e) {
                    throw new RuntimeException("Could not load mod file into deque as it was full", e);
                }
            }

            InertiaAntiCheat.debugInfo("Mod file loader thread cleaning up at index " + this.allModPathsIndex);
            this.completed = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized byte[] stageNextFile() {
        try {
            while (this.loadedFiles.isEmpty()) {
                wait();
            }

            byte[] loadedFile = this.loadedFiles.remove();
            InertiaAntiCheat.debugInfo("Staged mod file");
            notifyAll();
            return loadedFile;
        } catch (InterruptedException e) {
            throw new RuntimeException("Could not stage next mod file from memory", e);
        }
    }
}
