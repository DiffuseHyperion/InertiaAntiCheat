package com.diffusehyperion.inertiaanticheat.networking.method.name;

import com.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.networking.method.name.handlers.NameReceiverHandler;
import com.diffusehyperion.inertiaanticheat.networking.method.name.handlers.NameValidationHandler;
import com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class ServerNameReceiverHandler extends NameReceiverHandler {
    private int currentIndex = 0;
    private final List<String> collectedMods = new ArrayList<>();

    public ServerNameReceiverHandler(KeyPair keyPair, Identifier modTransferID, ServerLoginNetworkHandler handler, NameValidationHandler validator) {
        super(keyPair, modTransferID, handler, validator);
    }

    @Override
    protected void receiveMod(MinecraftServer minecraftServer, ServerLoginNetworkHandler handler, boolean b, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        if (!b) {
            ServerLoginNetworking.unregisterReceiver(handler, InertiaAntiCheatConstants.SEND_MOD);
            this.validator.checkModlist(collectedMods);
            return;
        }

        LoginPacketSender sender = (LoginPacketSender) packetSender;
        InertiaAntiCheat.debugInfo("Receiving mod " + this.currentIndex);

        byte[] name = InertiaAntiCheat.decryptAESRSAEncodedBuf(buf, super.keyPair.getPrivate());

        this.collectedMods.add(new String(name, StandardCharsets.UTF_8));
        this.currentIndex += 1;

        InertiaAntiCheat.debugInfo("Continuing transfer");
        sender.sendPacket(this.modTransferID, PacketByteBufs.empty());

        InertiaAntiCheat.debugLine();
    }
}
