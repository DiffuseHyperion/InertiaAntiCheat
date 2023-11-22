package me.diffusehyperion.inertiaanticheat.packets.legacy;

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
import java.util.*;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.*;

public class ModListResponseC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        debugInfo("Received mod list response from " + serverPlayerEntity.getEntityName() + ".");

        impendingPlayers.remove(serverPlayerEntity);
        if (!serverConfig.getString("grace.titleText").isEmpty()) {
            serverPlayNetworkHandler.sendPacket(new ClearTitleS2CPacket(true));
        }

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
            // arrays.aslist creates a fixed size list, so linkedlist is required
            List<String> modList = new LinkedList<>(Arrays.asList(response.split(", ")));

            // hashes should only be calculated using getModlistHash!!

            if (serverConfig.getBoolean("mods.showMods")) {
                info(serverPlayerEntity.getEntityName() + " is joining with the following modlist: " + modList);
            }
            if (serverConfig.getBoolean("hash.showHash")) {
                new Thread(() -> info(serverPlayerEntity.getEntityName() + "'s modlist hash: " + getModlistHash(modList))).start();
            }

            if (serverConfig.getList("hash.hash").isEmpty()) {
                // hash empty, use blacklist/whitelist
                List<String> blacklisted = serverConfig.getList("mods.blacklist");
                List<String> foundBlacklistedMods = new ArrayList<>();
                for (String blacklistedMod : blacklisted) {
                    if (modList.contains(blacklistedMod)) {
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
                        notFoundWhitelistedMods.add(whitelistedMod);
                    }
                }
                if (!notFoundWhitelistedMods.isEmpty()) {
                    kickMessage = serverConfig.getString("mods.whitelistMessage")
                            .replace("${whitelisted}", listToPrettyString(notFoundWhitelistedMods));
                }
            } else {
                String hash = getModlistHash(modList);
                List<String> acceptedHashes = serverConfig.getList("hash.hash");
                if (!acceptedHashes.contains(hash)) {
                    kickMessage = serverConfig.getString("hash.hashMessage");
                }
            }
        }

        if (Objects.nonNull(kickMessage)) {
            serverPlayerEntity.networkHandler.disconnect(Text.of(kickMessage));
            debugInfo("Kicked " + serverPlayerEntity.getEntityName() + " for " + kickMessage + ".");
        } else {
            debugInfo("Accepted " + serverPlayerEntity.getEntityName() + " into the server.");
        }
    }

    private static List<String> removeSoftWhitelistedMods(List<String> modList) {
        for (Object softWhitelistedModObj : serverConfig.getList("hash.softWhitelist")) {
            String softWhitelistedMod = (String) softWhitelistedModObj;
            modList.remove(softWhitelistedMod);
        }
        return modList;
    }

    private static String getModlistHash(List<String> modlist) {
        List<String> finalModlist = removeSoftWhitelistedMods(modlist);
        return getHash(finalModlist.toString(), "MD5");
    }
}
