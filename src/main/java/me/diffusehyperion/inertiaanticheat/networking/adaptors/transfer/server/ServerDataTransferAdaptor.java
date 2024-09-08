package me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ServerDataTransferAdaptor extends ServerModlistTransferAdaptor {
    private int maxIndex;
    private int currentIndex = 0;
    private final List<byte[]> collectedMods = new ArrayList<>();
    private byte[] buffer;

    public ServerDataTransferAdaptor(KeyPair keyPair, Identifier modTransferID) {
        super(keyPair, modTransferID);
    }

    @Override
    public void transferMod(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean b, PacketByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender) {
        LoginPacketSender sender = (LoginPacketSender) packetSender;
        InertiaAntiCheat.debugInfo("Receiving mod " + this.currentIndex);

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
            if (!checkModlist()) {
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
}
