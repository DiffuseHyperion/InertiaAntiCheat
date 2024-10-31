package me.diffusehyperion.inertiaanticheat.networking.method.data;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.networking.method.data.handlers.DataReceiverHandler;
import me.diffusehyperion.inertiaanticheat.networking.method.data.handlers.DataValidationHandler;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.ArrayUtils;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ServerDataReceiverHandler extends DataReceiverHandler {
    private int currentIndex = 0;
    private byte[] buffer;

    private final List<byte[]> collectedMods = new ArrayList<>();

    public ServerDataReceiverHandler(KeyPair keyPair, Identifier modTransferID, ServerLoginNetworkHandler handler, DataValidationHandler validator) {
        super(keyPair, modTransferID, handler, validator);
    }

    @Override
    public void receiveMod(MinecraftServer minecraftServer, ServerLoginNetworkHandler handler, boolean b, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        if (!b) {
            ServerLoginNetworking.unregisterReceiver(handler, InertiaAntiCheatConstants.SEND_MOD);
            this.validator.checkModlist(collectedMods);
            return;
        }

        LoginPacketSender sender = (LoginPacketSender) packetSender;
        InertiaAntiCheat.debugInfo("Receiving mod " + this.currentIndex);

        boolean isFinalChunk = buf.readBoolean();
        InertiaAntiCheat.debugInfo("Final chunk: " + isFinalChunk);

        byte[] fileData = InertiaAntiCheat.decryptAESRSAEncodedBuf(buf, super.keyPair.getPrivate());
        InertiaAntiCheat.debugInfo("Checksum of chunk: " + InertiaAntiCheat.getHash(fileData, HashAlgorithm.MD5));
        this.buffer = ArrayUtils.addAll(this.buffer, fileData);

        if (isFinalChunk) {
            InertiaAntiCheat.debugInfo("Adding mod, checksum: " + InertiaAntiCheat.getHash(this.buffer, HashAlgorithm.MD5));

            this.collectedMods.add(this.buffer);
            this.buffer = new byte[]{};
            this.currentIndex += 1;
        }

        InertiaAntiCheat.debugInfo("Continuing transfer");
        sender.sendPacket(this.modTransferID, PacketByteBufs.empty());

        InertiaAntiCheat.debugLine();
    }
}
