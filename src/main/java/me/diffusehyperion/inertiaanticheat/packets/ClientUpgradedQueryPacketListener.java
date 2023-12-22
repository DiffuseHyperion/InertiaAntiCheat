package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import net.minecraft.network.listener.ClientQueryPacketListener;

public interface ClientUpgradedQueryPacketListener extends ClientQueryPacketListener {
    void onContactReject(ContactResponseRejectS2CPacket var1);
    void onContactUnencryptedResponse(ContactResponseUnencryptedS2CPacket var1);
    void onContactEncryptedResponse(ContactResponseEncryptedS2CPacket var1);

    void onCommunicateAcceptResponse(CommunicateResponseAcceptS2CPacket var1);
    void onCommunicateRejectResponse(CommunicateResponseRejectS2CPacket var1);

}
