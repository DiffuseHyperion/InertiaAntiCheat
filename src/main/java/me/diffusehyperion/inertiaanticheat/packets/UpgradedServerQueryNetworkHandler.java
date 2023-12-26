package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import me.diffusehyperion.inertiaanticheat.server.ServerLoginHandler;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UpgradedServerQueryNetworkHandler implements ServerUpgradedQueryPacketListener {
    /* ---------- vanilla fields ----------*/

    private static final Text REQUEST_HANDLED = Text.translatable("multiplayer.status.request_handled");
    private final ServerMetadata metadata;
    private final ClientConnection connection;
    private boolean responseSent;

    /* ---------- custom fields ----------*/

    private long startTime;
    private final Runnable disconnectRunnable;
    private final KeyPair serverKeyPair;
    private PublicKey clientPublicKey;

    public UpgradedServerQueryNetworkHandler(ServerMetadata metadata, ClientConnection connection) {
        /* ---------- vanilla fields ----------*/

        this.metadata = metadata;
        this.connection = connection;

        /* ---------- custom fields ----------*/

        this.disconnectRunnable = () -> {
            InertiaAntiCheat.info("Disconnected from address " + this.connection.getAddress());
            this.connection.disconnect(REQUEST_HANDLED);
        };
        this.serverKeyPair = InertiaAntiCheat.createRSAPair();
    }

    @Override
    public void onContactRequest(ContactRequestC2SPacket var1) {
        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Received contact request from address: " + this.connection.getAddress());
        InertiaAntiCheatServer.serverScheduler.cancelTask(this.disconnectRunnable);

        this.clientPublicKey = var1.getClientPublicKey();

        this.connection.send(new ContactResponseS2CPacket(this.serverKeyPair.getPublic()));
        InertiaAntiCheat.debugInfo("Sent contact response back to address");
        InertiaAntiCheat.debugLine();
    }

    @Override
    public void onCommunicateRequest(CommunicateRequestC2SPacket var1) {
        InertiaAntiCheat.debugInfo("Received communication request from address: " + this.connection.getAddress());
        try {
            SecretKey decryptedAESKey = new SecretKeySpec(InertiaAntiCheat.decryptRSABytes(var1.getEncryptedRSAAESKey(), this.serverKeyPair.getPrivate()), "AES");
            byte[] decryptedSerializedModlistBytes = InertiaAntiCheat.decryptAESBytes(var1.getEncryptedAESSerializedModlist(), decryptedAESKey);

            List<File> modFiles = deserializeResponse(decryptedSerializedModlistBytes);
            if (Objects.isNull(modFiles)) {
                InertiaAntiCheat.debugError("The server received an invalid response from a player!");
                InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");

                this.connection.send(new CommunicateResponseS2CPacket());
            } else {
                if (checkModlist(modFiles)) {
                    String ip = InertiaAntiCheat.getIP(connection.getAddress());
                    UUID key;
                    if (!ServerLoginHandler.generatedKeys.containsKey(ip)) {
                        key = UUID.randomUUID();
                        ServerLoginHandler.generatedKeys.put(InertiaAntiCheat.getIP(connection.getAddress()), key);
                    } else {
                        key = ServerLoginHandler.generatedKeys.get(ip);
                    }
                    byte[] encryptedKey = InertiaAntiCheat.encryptRSABytes(InertiaAntiCheat.UUIDToBytes(key), this.clientPublicKey);
                    this.connection.send(new CommunicateResponseS2CPacket(encryptedKey));
                } else {
                    this.connection.send(new CommunicateResponseS2CPacket());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            InertiaAntiCheat.debugError("Something went wrong while deserializing a response packet!");
            InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");

            this.connection.send(new CommunicateResponseS2CPacket());
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.disconnectRunnable.run();
            InertiaAntiCheat.debugInfo("Sent communication response back to address");
            InertiaAntiCheat.debugLine();
        }
    }

    private List<File> deserializeResponse(byte[] serializedModlistBytes) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedModlistBytes));
        Object modFilesObj = ois.readObject();
        if (!(modFilesObj instanceof ArrayList<?>)) {
            return null;
        }
        List<?> modFilesListObj = (List<?>) modFilesObj;
        for (Object obj : modFilesListObj) {
            if (!(obj instanceof File)) {
                return null;
            }
        }
        return (List<File>) modFilesObj; // dunno why intellij is complaining about unchecked cast lol
    }

    private boolean checkModlist(List<File> mods) throws Exception {
        InertiaAntiCheat.debugLine2();
        if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.INDIVIDUAL) {
            InertiaAntiCheat.debugInfo("Checking modlist now, using individual method");
            InertiaAntiCheat.debugInfo("Mod list size: " + mods.size());
            List<String> blacklistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.blacklist");
            InertiaAntiCheat.debugInfo("Blacklisted mods: " + String.join(", ", blacklistedMods));
            List<String> whitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.individual.whitelist");
            InertiaAntiCheat.debugInfo("Whitelisted mods: " + String.join(", ", whitelistedMods));
            InertiaAntiCheat.debugLine();
            for (File mod : mods) {
                InertiaAntiCheat.debugInfo("Checking file name: " + mod.getName());
                String fileHash = InertiaAntiCheat.getChecksum(Files.readAllBytes(mod.toPath()), InertiaAntiCheatServer.hashAlgorithm);
                InertiaAntiCheat.debugInfo("File hash: " + fileHash + "; with algorithm " + InertiaAntiCheatServer.hashAlgorithm);

                if (blacklistedMods.contains(fileHash)) {
                    InertiaAntiCheat.debugInfo("Found in blacklist");
                    InertiaAntiCheat.debugLine();
                    return false;
                }
                if (whitelistedMods.contains(fileHash)) {
                    InertiaAntiCheat.debugInfo("Found in whitelist");
                    whitelistedMods.remove(fileHash);
                }
                InertiaAntiCheat.debugLine();
            }
            if (!whitelistedMods.isEmpty()) {
                InertiaAntiCheat.debugInfo("Whitelist not fufilled");
                InertiaAntiCheat.debugLine();
                return false;
            }
            InertiaAntiCheat.debugInfo("Passed");
            InertiaAntiCheat.debugLine2();
            return true;
        } else if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.GROUP) {
            InertiaAntiCheat.debugInfo("Checking modlist now, using group method");
            List<String> softWhitelistedMods = InertiaAntiCheatServer.serverConfig.getList("mods.group.softWhitelist");
            InertiaAntiCheat.debugInfo("Soft whitelisted mods: " + String.join(", ", softWhitelistedMods));
            StringBuilder combinedHashes = new StringBuilder();
            for (File mod : mods) {
                String fileHash = InertiaAntiCheat.getChecksum(Files.readAllBytes(mod.toPath()), InertiaAntiCheatServer.hashAlgorithm);
                if (softWhitelistedMods.contains(fileHash)) {
                    softWhitelistedMods.remove(fileHash);
                } else {
                    combinedHashes.append(fileHash);
                }
            }
            String finalHash = InertiaAntiCheat.getChecksum(combinedHashes.toString().getBytes(), "MD5"); // no need to be cryptographically safe here
            InertiaAntiCheat.debugInfo("Final hash: " + finalHash);

            boolean success = InertiaAntiCheatServer.serverConfig.getList("mods.group.checksum").contains(finalHash);
            if (success) {
                InertiaAntiCheat.debugInfo("Passed");
            } else {
                InertiaAntiCheat.debugInfo("Failed");
            }
            InertiaAntiCheat.debugLine2();
            return success;
        } else {
            throw new Exception("Invalid mod list check method! Please report this on this project's Github!");
        }
    }

    /* ---------- (Mostly) vanilla stuff below ----------*/

    @Override
    public void onDisconnected(Text reason) {
    }

    @Override
    public boolean isConnectionOpen() {
        return this.connection.isOpen();
    }

    @Override
    public void onRequest(QueryRequestC2SPacket packet) {
        this.startTime = Util.getMeasuringTimeMs();

        if (this.responseSent) {
            this.connection.disconnect(REQUEST_HANDLED);
            return;
        }
        this.responseSent = true;
        this.connection.send(new QueryResponseS2CPacket(this.metadata));
    }

    @Override
    public void onQueryPing(QueryPingC2SPacket packet) {
        this.connection.send(new PingResultS2CPacket(packet.getStartTime()));

        long timeTaken = Util.getMeasuringTimeMs() - this.startTime;
        InertiaAntiCheatServer.serverScheduler.addTask((int) ((timeTaken / 50) + 100), disconnectRunnable);
    }
}
