package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.config;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.impendingPlayers;

public class ModListResponseC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        String rawResponse = packetByteBuf.readString();
        String response = rawResponse;
        response = response.replace("[", "");
        response = response.replace("]", "");
        List<String> modList = Arrays.asList(response.split(", "));

        InertiaAntiCheat.debugInfo(serverPlayerEntity.getEntityName() + " is joining with the following modlist: " + modList);
        if (config.getBoolean("hash.enable")) {
            // checksum empty, use blacklist/whitelist
            List<String> blacklisted = config.getList("mods.blacklist");
            List<String> foundBlacklistedMods = new ArrayList<>();
            for (String blacklistedMod : blacklisted) {
                if (modList.contains(blacklistedMod)) {
                    InertiaAntiCheat.debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as he is running " + blacklistedMod + "!");
                    foundBlacklistedMods.add(blacklistedMod);
                }
            }
            if (!foundBlacklistedMods.isEmpty()) {
                serverPlayNetworkHandler.disconnect(Text.of(config.getString("mods.blacklistMessage")
                        .replace("${blacklisted}", InertiaAntiCheat.listToPrettyString(foundBlacklistedMods))));
            }

            List<String> whitelisted = config.getList("mods.whitelist");
            List<String> notFoundWhitelistedMods = new ArrayList<>();
            for (String whitelistedMod : whitelisted) {
                if (!modList.contains(whitelistedMod)) {
                    InertiaAntiCheat.debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as he is not running " + whitelistedMod + "!");
                    notFoundWhitelistedMods.add(whitelistedMod);
                }
            }
            if (!notFoundWhitelistedMods.isEmpty()) {
                serverPlayNetworkHandler.disconnect(Text.of(config.getString("mods.whitelistMessage")
                                        .replace("${whitelisted}", InertiaAntiCheat.listToPrettyString(notFoundWhitelistedMods))));
            }
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] arr = md.digest(rawResponse.getBytes());
                String hash = Base64.getEncoder().encodeToString(arr);
                if (config.getBoolean("hash.showHash")) {
                    LOGGER.info(serverPlayerEntity.getEntityName() + "'s modlist hash: " + hash);
                }
                if (!hash.equals(config.getString("hash.hash"))) {
                    InertiaAntiCheat.debugInfo("Kicking " + serverPlayerEntity.getEntityName() + " as his modlist hash does not match up!");
                    serverPlayNetworkHandler.sendPacket(new DisconnectS2CPacket(Text.of(config.getString("hash.hashMessage"))));
                }
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        impendingPlayers.remove(serverPlayerEntity);
    }
}
