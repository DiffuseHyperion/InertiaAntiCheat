package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;

public class ModListRequestS2CPacket {
    public static void receive(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        UUID uuid = packetByteBuf.readUuid();
        List<String> modNameList = new ArrayList<>();
        // idk how bytebufs work exactly so ill dedicate first item to being uuid
        modNameList.add(uuid.toString());

        for (ModContainer container : FabricLoader.getInstance().getAllMods().stream().toList()) {
            modNameList.add(container.getMetadata().getName());
        }

        PacketByteBuf responseBuf = PacketByteBufs.create();
        responseBuf.writeString(modNameList.toString());
        LOGGER.info("Sending string: " + modNameList);

        client.execute(() -> ClientPlayNetworking.send(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, responseBuf));
    }
}
