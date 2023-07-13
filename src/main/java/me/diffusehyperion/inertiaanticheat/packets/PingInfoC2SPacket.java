package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.debugInfo;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.serverConfig;

public class PingInfoC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        debugInfo("Received ping request.");
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(serverConfig.getBoolean("e2ee.enable"));
        serverPlayNetworkHandler.sendPacket(ServerPlayNetworking.createS2CPacket(InertiaAntiCheatConstants.PONG_PACKET_ID, buf));
    }
}
