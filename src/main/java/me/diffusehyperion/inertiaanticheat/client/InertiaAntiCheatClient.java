package me.diffusehyperion.inertiaanticheat.client;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.packets.ModListRequestS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class InertiaAntiCheatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.REQUEST_PACKET_ID, ModListRequestS2CPacket::receive);
    }
}
