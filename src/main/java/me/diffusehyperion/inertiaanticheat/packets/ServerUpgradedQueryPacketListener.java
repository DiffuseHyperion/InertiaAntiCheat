package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import net.minecraft.network.listener.ServerQueryPacketListener;

public interface ServerUpgradedQueryPacketListener extends ServerQueryPacketListener {
    void onContactRequest(ContactRequestC2SPacket var1);
    void onCommunicateRequest(CommunicateRequestC2SPacket var1);
}
