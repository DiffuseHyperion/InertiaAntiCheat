package me.diffusehyperion.inertiaanticheat.server.adaptors;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.ClientConnection;
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

public class DataValidatorAdaptor extends ServerModlistValidatorAdaptor {
    private int maxIndex;
    private int currentIndex = 0;
    private final List<byte[]> collectedMods = new ArrayList<>();
    private byte[] buffer;

    public DataValidatorAdaptor(KeyPair keyPair, Identifier modTransferID) {
        super(keyPair, modTransferID);
    }

    @Override
    public void startModTransfer(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean b, PacketByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) serverLoginNetworkHandler;
        LoginPacketSender sender = (LoginPacketSender) packetSender; // im 75% sure they forgot to change PacketSender to LoginPacketSender lmao

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
        sender.sendPacket(this.modTransferID, PacketByteBufs.empty());

        synchronizer.waitFor(this.future);

        InertiaAntiCheat.debugLine();
    }

    private void continueModTransfer(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean b, PacketByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender) {
        LoginPacketSender sender = (LoginPacketSender) packetSender; // im 75% sure they forgot to change PacketSender to LoginPacketSender lmao
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

        InertiaAntiCheat.debugInfo("Checksum of chunk: " + InertiaAntiCheat.getHash(fileData, HashAlgorithm.MD5));

        this.buffer = ArrayUtils.addAll(this.buffer, fileData);

        if (isFinalChunk) {
            InertiaAntiCheat.debugInfo("Adding mod, checksum: " + InertiaAntiCheat.getHash(this.buffer, HashAlgorithm.MD5));

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
            sender.sendPacket(this.modTransferID, PacketByteBufs.empty());

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
                String fileHash = InertiaAntiCheat.getHash(mod, InertiaAntiCheatServer.hashAlgorithm);
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
            List<String> copySoftWhitelistedMods = new ArrayList<>(softWhitelistedMods);
            for (byte[] mod : mods) {
                String fileHash = InertiaAntiCheat.getHash(mod, InertiaAntiCheatServer.hashAlgorithm);
                if (copySoftWhitelistedMods.contains(fileHash)) {
                    copySoftWhitelistedMods.remove(fileHash);
                } else {
                    hashes.add(fileHash);
                }
            }
            Collections.sort(hashes);
            String combinedHash = String.join("|", hashes);
            String finalHash = InertiaAntiCheat.getHash(combinedHash.getBytes(), HashAlgorithm.MD5); // no need to be cryptographically safe here
            InertiaAntiCheat.debugInfo("Final hash: " + finalHash);
            InertiaAntiCheat.debugInfo("Combined hash: " + combinedHash);


            boolean success = InertiaAntiCheatServer.serverConfig.getList("mods.group.hash").contains(finalHash);
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
