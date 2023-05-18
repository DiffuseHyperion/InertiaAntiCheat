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

public class ModListRequestS2CPacket {
    public static void receive(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        List<String> modNameList = new ArrayList<>();

        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            modNameList.add(container.getMetadata().getName());
        }

        PacketByteBuf responseBuf = PacketByteBufs.create();
        responseBuf.writeString(modNameList.toString());

        client.execute(() -> clientPlayNetworkHandler.sendPacket(ClientPlayNetworking.createC2SPacket(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, responseBuf)));
    }
}
