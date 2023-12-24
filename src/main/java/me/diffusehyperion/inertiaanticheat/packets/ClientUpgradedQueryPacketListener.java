package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface ClientUpgradedQueryPacketListener extends ClientQueryPacketListener {
    void onContactResponse(ContactResponseS2CPacket var1);
    void onCommunicateResponse(CommunicateResponseS2CPacket var1);

}
