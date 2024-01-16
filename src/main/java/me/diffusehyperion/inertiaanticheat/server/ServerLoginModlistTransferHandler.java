package me.diffusehyperion.inertiaanticheat.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerLoginModlistTransferHandler {
    public static void init() {
        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginModlistTransferHandler::requestModTransfer);
    }

    private static void requestModTransfer(ServerLoginNetworkHandler handler, MinecraftServer server, PacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) handler;

        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Checking if " + upgradedHandler.inertiaAntiCheat$getGameProfile().getName() + " has bypass permissions");
        boolean allowed = Permissions.check(upgradedHandler.inertiaAntiCheat$getGameProfile(), "inertiaanticheat.bypass").join();
        if (allowed) {
            InertiaAntiCheat.debugInfo(upgradedHandler.inertiaAntiCheat$getGameProfile().getName() + " is allowed to bypass");
            InertiaAntiCheat.debugLine();
            return;
        }
        InertiaAntiCheat.debugInfo("Not allowed to bypass, sending request to address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress());

        KeyPair keyPair = InertiaAntiCheat.createRSAPair();
        PacketByteBuf response = PacketByteBufs.create();
        response.writeBytes(keyPair.getPublic().getEncoded());

        ServerLoginModlistTransferHandler transferHandler = new ServerLoginModlistTransferHandler(keyPair, InertiaAntiCheatConstants.MOD_TRANSFER_CONTINUE_ID);
        ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.MOD_TRANSFER_START_ID, transferHandler::startModTransfer);
        sender.sendPacket(InertiaAntiCheatConstants.MOD_TRANSFER_START_ID, response);
        synchronizer.waitFor(transferHandler.future);

        InertiaAntiCheat.debugLine();
    }

    private final KeyPair keyPair;
    private final Identifier modTransferID;

    private int maxIndex;
    private int currentIndex = 0;
    private final List<byte[]> collectedMods = new ArrayList<>();
    private byte[] buffer;

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    public ServerLoginModlistTransferHandler(KeyPair keyPair, Identifier modTransferID) {
        this.keyPair = keyPair;
        this.modTransferID = modTransferID;
    }

    protected void startModTransfer(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean b, PacketByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender) {
        ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) serverLoginNetworkHandler;
        InertiaAntiCheat.debugInfo("Received response from address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress());
        if (!b) {
            serverLoginNetworkHandler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("mods.vanillaKickMessage")));
            return;
        }

        byte[] encryptedData = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(encryptedData);
        this.maxIndex = new BigInteger(InertiaAntiCheat.decryptRSABytes(encryptedData, this.keyPair.getPrivate())).intValue();

        InertiaAntiCheat.debugInfo("Max index: " + this.maxIndex);

        ServerLoginNetworking.registerReceiver(serverLoginNetworkHandler, this.modTransferID, this::continueModTransfer);
        packetSender.sendPacket(this.modTransferID, PacketByteBufs.empty());

        loginSynchronizer.waitFor(this.future);

        InertiaAntiCheat.debugLine();
    }

    private void continueModTransfer(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean b, PacketByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender) {
        InertiaAntiCheat.debugInfo("Receiving mod " + this.currentIndex);
        if (!b) {
            serverLoginNetworkHandler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("mods.vanillaKickMessage")));
            return;
        }

        boolean isFinalChunk = packetByteBuf.readBoolean();
        InertiaAntiCheat.debugInfo("Final chunk: " + isFinalChunk);

        int encryptedSecretKeyLength = packetByteBuf.readInt();
        byte[] encryptedSecretKey = new byte[encryptedSecretKeyLength];
        packetByteBuf.readBytes(encryptedSecretKey);
        SecretKey secretKey = new SecretKeySpec(InertiaAntiCheat.decryptRSABytes(encryptedSecretKey, this.keyPair.getPrivate()), "AES");

        byte[] encryptedData = new byte[packetByteBuf.readableBytes()];
        packetByteBuf.readBytes(encryptedData);
        byte[] fileData = InertiaAntiCheat.decryptAESBytes(encryptedData, secretKey);

        InertiaAntiCheat.debugInfo("Checksum of chunk: " + InertiaAntiCheat.getChecksum(fileData, HashAlgorithm.MD5));

        this.buffer = ArrayUtils.addAll(this.buffer, fileData);

        if (isFinalChunk) {
            InertiaAntiCheat.debugInfo("Adding mod, checksum: " + InertiaAntiCheat.getChecksum(this.buffer, HashAlgorithm.MD5));

            this.collectedMods.add(this.buffer);
            this.buffer = new byte[]{};
            this.currentIndex += 1;
        }

        if (this.currentIndex >= this.maxIndex) {
            InertiaAntiCheat.debugInfo("Finishing transfer, checking mods now");
            if (!checkModlist(this.collectedMods)) {
                serverLoginNetworkHandler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("mods.deniedKickMessage")));
            }
            ServerLoginNetworking.unregisterReceiver(serverLoginNetworkHandler, this.modTransferID);
            this.future.complete(null);

            InertiaAntiCheat.debugLine();
        } else {
            InertiaAntiCheat.debugInfo("Continuing transfer");
            packetSender.sendPacket(this.modTransferID, PacketByteBufs.empty());

            InertiaAntiCheat.debugLine();
        }
    }

    private boolean checkModlist(List<byte[]> mods) {
        InertiaAntiCheat.debugLine2();
        if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.INDIVIDUAL) {
            InertiaAntiCheat.debugInfo("Checking modlist now, using individual method");
            InertiaAntiCheat.debugInfo("Mod list size: " + mods.size());
            List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.blacklist");
            InertiaAntiCheat.debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));
            List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.whitelist");
            InertiaAntiCheat.debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));
            InertiaAntiCheat.debugLine();
            for (byte[] mod : mods) {
                String fileHash = InertiaAntiCheat.getChecksum(mod, InertiaAntiCheatServer.hashAlgorithm);
                InertiaAntiCheat.debugInfo("File hash: " + fileHash + "; with algorithm " + InertiaAntiCheatServer.hashAlgorithm);

                if (blacklistedMods.contains(fileHash)) {
                    InertiaAntiCheat.debugInfo("Found in blacklist");
                    InertiaAntiCheat.debugLine();
                    return false;
                }
                if (whitelistedMods.contains(fileHash)) {
                    InertiaAntiCheat.debugInfo("Found in whitelist");
                    whitelistedMods.remove(fileHash);
                }
                InertiaAntiCheat.debugLine();
            }
            if (!whitelistedMods.isEmpty()) {
                InertiaAntiCheat.debugInfo("Whitelist not fulfilled");
                InertiaAntiCheat.debugLine();
                return false;
            }
            InertiaAntiCheat.debugInfo("Passed");
            InertiaAntiCheat.debugLine2();
            return true;
        } else {
            InertiaAntiCheat.debugInfo("Checking modlist now, using group method");
            List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.group.softWhitelist");
            InertiaAntiCheat.debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));
            List<String> hashes = new ArrayList<>();
            for (byte[] mod : mods) {
                String fileHash = InertiaAntiCheat.getChecksum(mod, InertiaAntiCheatServer.hashAlgorithm);
                if (softWhitelistedMods.contains(fileHash)) {
                    softWhitelistedMods.remove(fileHash);
                } else {
                    hashes.add(fileHash);
                }
            }
            Collections.sort(hashes);
            String combinedHash = String.join("|", hashes);
            String finalHash = InertiaAntiCheat.getChecksum(combinedHash.getBytes(), HashAlgorithm.MD5); // no need to be cryptographically safe here
            InertiaAntiCheat.debugInfo("Final hash: " + finalHash);
            InertiaAntiCheat.debugInfo("Combined hash: " + combinedHash);


            boolean success = InertiaAntiCheatServer.serverConfig.getList("mods.group.checksum").contains(finalHash);
            if (success) {
                InertiaAntiCheat.debugInfo("Passed");
            } else {
                InertiaAntiCheat.debugInfo("Failed");
            }
            InertiaAntiCheat.debugLine2();
            return success;
        }
    }
}
