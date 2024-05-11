package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.packets.ModlistResponses.ModlistEncryptedResponse;
import me.diffusehyperion.inertiaanticheat.packets.ModlistResponses.ModlistPlainResponse;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.debugInfo;

public class ModListRequestS2CPayload implements CustomPayload {
    private final PublicKey publicKey;

    public static final Id<ModListRequestS2CPayload> ID = new CustomPayload.Id<>(InertiaAntiCheatConstants.REQUEST_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, ModListRequestS2CPayload> CODEC = PacketCodec.of(ModListRequestS2CPayload::write, ModListRequestS2CPayload::new);

    public ModListRequestS2CPayload() {
        this.publicKey = null;
    }
    public ModListRequestS2CPayload(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public ModListRequestS2CPayload(PacketByteBuf buf) {
        if (buf.readableBytes() <= 0) {
            this.publicKey = null;
            return;
        }
        byte[] rawPublicKey = new byte[buf.readableBytes()];
        buf.readBytes(rawPublicKey);
        this.publicKey = InertiaAntiCheat.readPublicKey(rawPublicKey);
    }

    public void write(PacketByteBuf buf) {
        if (publicKey != null) {
            buf.writeBytes(publicKey.getEncoded());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ModListRequestS2CPayload.ID;
    }

    public static void onReceive(ModListRequestS2CPayload packet, ClientPlayNetworking.Context context) {
        debugInfo("Received mod list request from server!");
        List<String> modNameList = new ArrayList<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            modNameList.add(container.getMetadata().getName());
        }

        if (packet.publicKey == null) {
            debugInfo("Server is not using E2EE, sending mod list in plaintext.");
            context.responseSender().sendPacket(new ModListResponseC2SPayload(new ModlistPlainResponse(modNameList.toString())));
            debugInfo("Sent mod list.");
            return;
        }

        debugInfo("Server is using E2EE, encrypting and sending mod list.");
        context.responseSender().sendPacket(new ModListResponseC2SPayload(new ModlistEncryptedResponse(InertiaAntiCheat.generateSecretKey(), modNameList.toString())));
        debugInfo("Sent mod list.");

    }
}
