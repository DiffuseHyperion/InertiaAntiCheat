package me.diffusehyperion.inertiaanticheat.packets;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.impendingPlayers;

public class ModListResponseC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        String response = packetByteBuf.readString();
        response = response.replace("[", "");
        response = response.replace("]", "");
        List<String> responseList = Arrays.asList(response.split(", "));
        UUID uuid = UUID.fromString(responseList.get(0));
        List<String> modList = responseList.subList(1, responseList.size() - 1);

        LOGGER.info(uuid.toString());
        LOGGER.info(modList.toString());
        impendingPlayers.remove(uuid);
    }
}
