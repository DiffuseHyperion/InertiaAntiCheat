package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.packets.ModlistResponses.ModlistEncryptedResponse;
import me.diffusehyperion.inertiaanticheat.packets.ModlistResponses.ModlistPlainResponse;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheat.*;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.*;

public class ModListResponseC2SPayload implements CustomPayload {
    private final ModListResponse response;

    public static final Id<ModListResponseC2SPayload> ID = new CustomPayload.Id<>(InertiaAntiCheatConstants.RESPONSE_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, ModListResponseC2SPayload> CODEC = PacketCodec.of(ModListResponseC2SPayload::write, ModListResponseC2SPayload::new);

    public ModListResponseC2SPayload(ModListResponse response) {
        this.response = response;
    }

    public ModListResponseC2SPayload(PacketByteBuf buf) {
        if (buf.readableBytes() <= 0) {
            this.response = null;
        } else {
            if (Objects.nonNull(serverE2EEKeyPair)) {
                int length = buf.readInt();
                byte[] encryptedModList = new byte[length];
                buf.readBytes(encryptedModList);
                byte[] encryptedAESKey = new byte[buf.readableBytes()];
                buf.readBytes(encryptedAESKey);

                byte[] rawAESKey = InertiaAntiCheat.decryptBytes(encryptedAESKey, serverE2EEKeyPair.getPrivate());
                SecretKey AESKey = new SecretKeySpec(rawAESKey, "AES");
                byte[] rawResponse = InertiaAntiCheat.decryptBytes(encryptedModList, AESKey);
                this.response = new ModlistEncryptedResponse(AESKey, new String(rawResponse));
            } else {
                this.response = new ModlistPlainResponse(buf.readString());
            }
        }
    }

    public void write(PacketByteBuf buf) {
        switch (response.getType()) {
            case PLAIN -> {
                ModlistPlainResponse plainResponse = (ModlistPlainResponse) response;
                buf.writeString(plainResponse.modlist());
            }
            case ENCRYPTED -> {
                ModlistEncryptedResponse encryptedResponse = (ModlistEncryptedResponse) response;

                byte[] encryptedModList = InertiaAntiCheat.encryptBytes(encryptedResponse.modlist().getBytes(), encryptedResponse.secretKey());
                byte[] encryptedSecretKey = InertiaAntiCheat.encryptBytes(encryptedResponse.secretKey().getEncoded(), serverE2EEKeyPair.getPublic());

                buf.writeInt(encryptedModList.length);
                buf.writeBytes(encryptedModList);
                buf.writeBytes(encryptedSecretKey);
            }
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ModListResponseC2SPayload.ID;
    }

    public static void onReceive(ModListResponseC2SPayload packet, ServerPlayNetworking.Context context) {
        String senderPlayerName = context.player().getName().getString();
        debugInfo("Received mod list response from " + senderPlayerName + ".");

        impendingPlayers.remove(context.player());
        if (!serverConfig.getString("grace.titleText").isEmpty()) {
            context.responseSender().sendPacket(new ClearTitleS2CPacket(true));
        }
        List<String> modList = getResponseFromResponsePacket(packet);

        String hash = null;
        // hashes should only be calculated using getModlistHash!!

        if (serverConfig.getBoolean("mods.showMods")) {
            info(senderPlayerName + " is joining with the following modlist: ");
            StringBuilder prettyModlist = new StringBuilder("[");
            for (String mod : modList) {
                prettyModlist.append("\"").append(mod).append("\", ");
            }
            prettyModlist.delete(prettyModlist.length() - 2, prettyModlist.length());
            prettyModlist.append("]");
            info(prettyModlist.toString());
        }
        if (serverConfig.getBoolean("hash.showHash")) {
            hash = getModlistHash(modList);
            info(senderPlayerName + "'s modlist hash: " + hash);
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
                kickPlayer(context, serverConfig.getString("mods.blacklistMessage")
                        .replace("${blacklisted}", listToPrettyString(foundBlacklistedMods)));
                return;
            }

            List<String> whitelisted = serverConfig.getList("mods.whitelist");
            List<String> notFoundWhitelistedMods = new ArrayList<>();
            for (String whitelistedMod : whitelisted) {
                if (!modList.contains(whitelistedMod)) {
                    notFoundWhitelistedMods.add(whitelistedMod);
                }
            }
            if (!notFoundWhitelistedMods.isEmpty()) {
                kickPlayer(context, serverConfig.getString("mods.whitelistMessage")
                        .replace("${whitelisted}", listToPrettyString(notFoundWhitelistedMods)));
                return;
            }
        } else {
            if (hash == null) {
                hash = getModlistHash(modList);
            }
            List<String> acceptedHashes = serverConfig.getList("hash.hash");
            if (!acceptedHashes.contains(hash)) {
                kickPlayer(context, serverConfig.getString("hash.hashMessage"));
                return;
            }
        }
        debugInfo("Accepted " + senderPlayerName + " into the server.");
    }

    @NotNull
    private static List<String> getResponseFromResponsePacket(ModListResponseC2SPayload packet) {
        String response;

        switch (packet.response.getType()) {
            case PLAIN -> {
                ModlistPlainResponse plainResponse = (ModlistPlainResponse) packet.response;
                response = plainResponse.modlist();
            }
            case ENCRYPTED -> {
                ModlistEncryptedResponse encryptedResponse = (ModlistEncryptedResponse) packet.response;
                response = encryptedResponse.modlist();
            }
            case null, default -> throw new RuntimeException();
        }

        response = response.replace("[", "").replace("]", "");
        // arrays.aslist creates a fixed size list, so linkedlist is required
        return new LinkedList<>(Arrays.asList(response.split(", ")));
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

    private static void kickPlayer(ServerPlayNetworking.Context context, String kickMessage) {
        context.responseSender().disconnect(Text.of(kickMessage));
        debugInfo("Kicked " + context.player().getName().getString() + " for " + kickMessage + ".");
    }
}
