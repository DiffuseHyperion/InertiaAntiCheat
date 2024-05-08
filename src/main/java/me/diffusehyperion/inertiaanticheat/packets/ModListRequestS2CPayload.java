package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.debugInfo;
import static me.diffusehyperion.inertiaanticheat.client.InertiaAntiCheatClient.clientE2EESecretKey;

public class ModListRequestS2CPayload implements CustomPayload {
    private final PublicKey publicKey;

    public static final Id<ModListRequestS2CPayload> ID = new CustomPayload.Id<>(InertiaAntiCheatConstants.REQUEST_PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, ModListRequestS2CPayload> CODEC = PacketCodec.of(ModListRequestS2CPayload::write, ModListRequestS2CPayload::new);

    public ModListRequestS2CPayload(RegistryByteBuf buf) {
        if (buf.readableBytes() <= 0) {
            publicKey = null;
        } else {
            byte[] rawPublicKeyBytes = new byte[buf.readableBytes()];
            buf.readBytes(rawPublicKeyBytes);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(rawPublicKeyBytes);
            try {
                KeyFactory factory = KeyFactory.getInstance("RSA");
                publicKey = factory.generatePublic(publicKeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void write(RegistryByteBuf buf) {
        buf.writeBytes(publicKey.getEncoded());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return null;
    }

    public static void onReceive(ModListRequestS2CPayload packet, ClientPlayNetworking.Context context) {
        debugInfo("Received mod list request from server!");
        List<String> modNameList = new ArrayList<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            modNameList.add(container.getMetadata().getName());
        }

        PacketByteBuf responseBuf;
        if (packet.publicKey == null) {
            debugInfo("Server is not using E2EE, sending mod list in plaintext.");
            responseBuf = PacketByteBufs.create();
            responseBuf.writeString(modNameList.toString());
        } else {
            if (Objects.isNull(clientE2EESecretKey)) {
                responseBuf = PacketByteBufs.empty();
            } else {
                debugInfo("Server is using E2EE, encrypting and sending mod list.");
                responseBuf = PacketByteBufs.create();

                byte[] encryptedModList = InertiaAntiCheat.encryptBytes(modNameList.toString().getBytes(), clientE2EESecretKey);
                byte[] encryptedSecretKey = InertiaAntiCheat.encryptBytes(clientE2EESecretKey.getEncoded(), packet.publicKey);
                responseBuf.writeInt(encryptedModList.length);
                responseBuf.writeBytes(encryptedModList);
                responseBuf.writeBytes(encryptedSecretKey);
            }
        }

        context.client().execute(() -> clientPlayNetworkHandler.sendPacket(ClientPlayNetworking.createC2SPacket(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, responseBuf)));
        debugInfo("Sent mod list.");
    }
}
