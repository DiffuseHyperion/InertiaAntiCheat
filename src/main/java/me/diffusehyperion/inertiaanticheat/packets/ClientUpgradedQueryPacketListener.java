package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.S2C.CommunicateResponseS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseRejectS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseEncryptedS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseUnencryptedS2CPacket;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface ClientUpgradedQueryPacketListener extends ClientQueryPacketListener {
    void onContactReject(ContactResponseRejectS2CPacket var1);
    void onContactUnencryptedResponse(ContactResponseUnencryptedS2CPacket var1);
    void onContactEncryptedResponse(ContactResponseEncryptedS2CPacket var1);

    void onCommunicateResponse(CommunicateResponseS2CPacket var1);
}
