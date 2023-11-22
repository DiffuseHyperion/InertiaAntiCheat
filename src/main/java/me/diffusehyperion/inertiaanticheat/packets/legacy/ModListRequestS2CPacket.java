package me.diffusehyperion.inertiaanticheat.packets.legacy;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

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

public class ModListRequestS2CPacket {
    public static void receive(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        debugInfo("Received modlist request from server!");
        List<String> modNameList = new ArrayList<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            modNameList.add(container.getMetadata().getName());
        }

        PacketByteBuf responseBuf;
        if (packetByteBuf.readableBytes() <= 0) {
            debugInfo("Server is not using E2EE, sending modlist in plaintext.");
            responseBuf = PacketByteBufs.create();
            responseBuf.writeString(modNameList.toString());
        } else {
            if (Objects.isNull(clientE2EESecretKey)) {
                responseBuf = PacketByteBufs.empty();
            } else {
                debugInfo("Server is using E2EE, sending modlist encrypted.");
                responseBuf = PacketByteBufs.create();

                byte[] rawPublicKeyBytes = new byte[packetByteBuf.readableBytes()];
                packetByteBuf.readBytes(rawPublicKeyBytes);
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(rawPublicKeyBytes);
                try {
                    KeyFactory factory = KeyFactory.getInstance("RSA");
                    PublicKey publicKey = factory.generatePublic(publicKeySpec);

                    byte[] encryptedModList = InertiaAntiCheat.encryptBytes(modNameList.toString().getBytes(), clientE2EESecretKey);
                    byte[] encryptedSecretKey = InertiaAntiCheat.encryptBytes(clientE2EESecretKey.getEncoded(), publicKey);
                    responseBuf.writeInt(encryptedModList.length);
                    responseBuf.writeBytes(encryptedModList);
                    responseBuf.writeBytes(encryptedSecretKey);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        client.execute(() -> clientPlayNetworkHandler.sendPacket(ClientPlayNetworking.createC2SPacket(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, responseBuf)));
        debugInfo("Sent modlist.");
    }
}
