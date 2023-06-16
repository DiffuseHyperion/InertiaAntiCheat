package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.*;

public class ModListResponseC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        String response;
        String kickMessage = null;
        if (packetByteBuf.readableBytes() <= 0) {
            debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as they do not support E2EE.");
            kickMessage = serverConfig.getString("e2ee.unsupportedMessage");
        } else {
            if (Objects.nonNull(serverE2EEKeyPair)) {
                int length = packetByteBuf.readInt();
                byte[] encryptedModList = new byte[length];
                packetByteBuf.readBytes(encryptedModList);
                byte[] encryptedAESKey = new byte[packetByteBuf.readableBytes()];
                packetByteBuf.readBytes(encryptedAESKey);

                byte[] rawAESKey = InertiaAntiCheat.decryptBytes(encryptedAESKey, serverE2EEKeyPair.getPrivate());
                SecretKey AESKey = new SecretKeySpec(rawAESKey, "AES");
                byte[] rawResponse = InertiaAntiCheat.decryptBytes(encryptedModList, AESKey);
                response = new String(rawResponse);
            } else {
                response = packetByteBuf.readString();
            }
            response = response.replace("[", "").replace("]", "");
            List<String> modList = Arrays.asList(response.split(", "));

            if (serverConfig.getBoolean("mods.showMods")) {
                LOGGER.info(serverPlayerEntity.getEntityName() + " is joining with the following modlist: " + modList);
            }
            String hash = null; // store for later use if needed
            if (serverConfig.getBoolean("hash.showHash")) {
                hash = getHash(response);
                LOGGER.info(serverPlayerEntity.getEntityName() + "'s modlist hash: " + hash);
            }

            if (serverConfig.getString("hash.hash").isEmpty()) {
                // checksum empty, use blacklist/whitelist
                List<String> blacklisted = serverConfig.getList("mods.blacklist");
                List<String> foundBlacklistedMods = new ArrayList<>();
                for (String blacklistedMod : blacklisted) {
                    if (modList.contains(blacklistedMod)) {
                        debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as he is running " + blacklistedMod + "!");
                        foundBlacklistedMods.add(blacklistedMod);
                    }
                }
                if (!foundBlacklistedMods.isEmpty()) {
                    kickMessage = serverConfig.getString("mods.blacklistMessage")
                            .replace("${blacklisted}", listToPrettyString(foundBlacklistedMods));
                }

                List<String> whitelisted = serverConfig.getList("mods.whitelist");
                List<String> notFoundWhitelistedMods = new ArrayList<>();
                for (String whitelistedMod : whitelisted) {
                    if (!modList.contains(whitelistedMod)) {
                        debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as he is not running " + whitelistedMod + "!");
                        notFoundWhitelistedMods.add(whitelistedMod);
                    }
                }
                if (!notFoundWhitelistedMods.isEmpty()) {
                    kickMessage = serverConfig.getString("mods.whitelistMessage")
                            .replace("${whitelisted}", listToPrettyString(notFoundWhitelistedMods));
                }
            } else {
                if (Objects.isNull(hash)) {
                    hash = getHash(response);
                }
                if (!hash.equals(serverConfig.getString("hash.hash"))) {
                    debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as his modlist hash does not match up!");
                    kickMessage = serverConfig.getString("hash.hashMessage");
                }
            }
        }
        if (Objects.nonNull(kickMessage)) {
            serverPlayerEntity.networkHandler.disconnect(Text.of(kickMessage));
        }
        impendingPlayers.remove(serverPlayerEntity);
        if (!serverConfig.getString("grace.titleText").isEmpty()) {
            serverPlayNetworkHandler.sendPacket(new ClearTitleS2CPacket(true));
        }
    }
}
