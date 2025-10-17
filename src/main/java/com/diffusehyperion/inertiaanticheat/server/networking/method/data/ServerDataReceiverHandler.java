package com.diffusehyperion.inertiaanticheat.server.networking.method.data;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.util.HashAlgorithm;
import com.diffusehyperion.inertiaanticheat.common.util.InertiaAntiCheatConstants;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.handlers.DataReceiverHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.handlers.DataValidationHandler;
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

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugInfo;
import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugLine;

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
        debugInfo("Receiving mod " + this.currentIndex);

        boolean isFinalChunk = buf.readBoolean();
        debugInfo("Final chunk: " + isFinalChunk);

        byte[] fileData = InertiaAntiCheat.decryptAESRSAEncodedBuf(buf, super.keyPair.getPrivate());
        debugInfo("Checksum of chunk: " + InertiaAntiCheat.getHash(fileData, HashAlgorithm.MD5));
        this.buffer = ArrayUtils.addAll(this.buffer, fileData);

        if (isFinalChunk) {
            debugInfo("Adding mod, checksum: " + InertiaAntiCheat.getHash(this.buffer, HashAlgorithm.MD5));

            this.collectedMods.add(this.buffer);
            this.buffer = new byte[]{};
            this.currentIndex += 1;
        }

        debugInfo("Continuing transfer");
        sender.sendPacket(this.modTransferID, PacketByteBufs.empty());

        debugLine();
    }
}
