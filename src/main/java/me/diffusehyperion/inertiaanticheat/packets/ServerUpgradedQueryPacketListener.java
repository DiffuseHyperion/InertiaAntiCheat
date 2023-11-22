package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestEncryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestUnencryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import net.minecraft.network.listener.ServerQueryPacketListener;

public interface ServerUpgradedQueryPacketListener extends ServerQueryPacketListener {
    void onContactRequest(ContactRequestC2SPacket var1);
    void onCommunicateUnencryptedRequest(CommunicateRequestUnencryptedC2SPacket var1);

    void onCommunicateEncryptedRequest(CommunicateRequestEncryptedC2SPacket var1);
}
