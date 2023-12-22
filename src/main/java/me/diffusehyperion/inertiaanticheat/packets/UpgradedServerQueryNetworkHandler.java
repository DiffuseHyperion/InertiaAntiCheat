package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestEncryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestUnencryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.serverConfig;
import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.serverE2EEKeyPair;

public class UpgradedServerQueryNetworkHandler implements ServerUpgradedQueryPacketListener {
    private long startTime;
    private final Runnable disconnectRunnable = new Runnable() {
        @Override
        public void run() {
            InertiaAntiCheat.info("Disconnected");
            connection.disconnect(REQUEST_HANDLED);
        }
    };

    @Override
    public void onContactRequest(ContactRequestC2SPacket var1) {
        InertiaAntiCheat.info("Received contact from address: " + connection.getAddress().toString());
        InertiaAntiCheatServer.serverScheduler.cancelTask(disconnectRunnable);

        boolean clientE2EESupport = var1.getE2EESupport();
        boolean serverE2EESupport = Objects.nonNull(serverE2EEKeyPair);

        if (!serverE2EESupport) {
            connection.send(new ContactResponseUnencryptedS2CPacket());
            InertiaAntiCheat.info("Sent contact unencrypted response");
        } else if (!clientE2EESupport) {
            connection.send(new ContactResponseRejectS2CPacket());
            InertiaAntiCheat.info("Sent contact reject response");
        } else {
            connection.send(new ContactResponseEncryptedS2CPacket(serverE2EEKeyPair.getPublic()));
            InertiaAntiCheat.info("Sent contact encrypted response");
        }
    }

    @Override
    public void onCommunicateUnencryptedRequest(CommunicateRequestUnencryptedC2SPacket var1) {
        InertiaAntiCheat.info("Received unencrypted communication");
        try {
            List<File> modFiles = deserializeResponse(var1.getSerializedModlist());
            if (Objects.isNull(modFiles)) {
                InertiaAntiCheat.debugError("The server received an invalid response from a player!");
                InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");

                connection.send(new CommunicateResponseRejectS2CPacket());
            } else {
                if (checkModlist(modFiles)) {
                    UUID newKey = UUID.randomUUID();
                    InertiaAntiCheatServer.generatedKeys.put(InertiaAntiCheat.getIP(connection.getAddress()), newKey);
                    connection.send(new CommunicateResponseAcceptS2CPacket(newKey));
                } else {
                    connection.send(new CommunicateResponseRejectS2CPacket());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            InertiaAntiCheat.debugError("Something went wrong while deserializing a response packet!");
            InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");
            InertiaAntiCheat.debugException(e);

            connection.send(new CommunicateResponseRejectS2CPacket());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        disconnectRunnable.run();
    }

    @Override
    public void onCommunicateEncryptedRequest(CommunicateRequestEncryptedC2SPacket var1) {
        InertiaAntiCheat.info("Received encrypted communication");
        try {
            SecretKey decryptedAESKey = new SecretKeySpec(InertiaAntiCheat.decryptRSABytes(var1.getEncrypytedRSAAESKey(), serverE2EEKeyPair.getPrivate()), "AES");
            byte[] decryptedSerializedModlistBytes = InertiaAntiCheat.decryptAESBytes(var1.getEncryptedAESSerializedModlist(), decryptedAESKey);

            List<File> modFiles = deserializeResponse(decryptedSerializedModlistBytes);
            if (Objects.isNull(modFiles)) {
                InertiaAntiCheat.debugError("The server received an invalid response from a player!");
                InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");

                connection.send(new CommunicateResponseRejectS2CPacket());
            } else {
                if (checkModlist(modFiles)) {
                    UUID newKey = UUID.randomUUID();
                    InertiaAntiCheatServer.generatedKeys.put(InertiaAntiCheat.getIP(connection.getAddress()), newKey);
                    connection.send(new CommunicateResponseAcceptS2CPacket(newKey));
                } else {
                    connection.send(new CommunicateResponseRejectS2CPacket());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            InertiaAntiCheat.debugError("Something went wrong while deserializing a response packet!");
            InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");

            connection.send(new CommunicateResponseRejectS2CPacket());
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        disconnectRunnable.run();
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
        if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.INDIVIDUAL) {
            InertiaAntiCheat.info("Checking modlist now, using individual method");
            InertiaAntiCheat.info("Mod list size: " + mods.size());
            List<String> blacklistedMods = serverConfig.getList("mods.individual.blacklist");
            InertiaAntiCheat.info("Blacklisted mods: " + String.join(", ", blacklistedMods));
            List<String> whitelistedMods = serverConfig.getList("mods.individual.whitelist");
            InertiaAntiCheat.info("Whitelisted mods: " + String.join(", ", whitelistedMods));
            InertiaAntiCheat.info("----------");

            for (File mod : mods) {
                InertiaAntiCheat.info("Checking file name: " + mod.getName());
                String fileHash = InertiaAntiCheat.getChecksum(Files.readAllBytes(mod.toPath()), InertiaAntiCheatServer.hashAlgorithm);
                InertiaAntiCheat.info("File hash: " + fileHash + "; with algorithm " + InertiaAntiCheatServer.hashAlgorithm);

                if (blacklistedMods.contains(fileHash)) {
                    InertiaAntiCheat.info("Found in blacklist");
                    InertiaAntiCheat.info("----------");
                    return false;
                }
                if (whitelistedMods.contains(fileHash)) {
                    InertiaAntiCheat.info("Found in whitelist");
                    whitelistedMods.remove(fileHash);
                }
                InertiaAntiCheat.info("----------");
            }
            if (!whitelistedMods.isEmpty()) {
                InertiaAntiCheat.info("Whitelist not fufilled");
                InertiaAntiCheat.info("----------");
                return false;
            }
            InertiaAntiCheat.info("Passed");
            InertiaAntiCheat.info("----------");
            return true;
        } else if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.GROUP) {
            InertiaAntiCheat.info("Checking modlist now, using group method");
            StringBuilder combinedHashes = new StringBuilder();
            for (File mod : mods) {
                String fileHash = InertiaAntiCheat.getChecksum(Files.readAllBytes(mod.toPath()), InertiaAntiCheatServer.hashAlgorithm);
                combinedHashes.append(fileHash);
            }
            String finalHash = InertiaAntiCheat.getChecksum(combinedHashes.toString().getBytes(), "MD5"); // no need to be cryptographically safe here
            return Objects.equals(serverConfig.getString("mods.group.checksum"), finalHash);
        } else {
            throw new Exception("Invalid mod list check method! Please report this on this project's Github!");
        }
    }

    /* ---------- (Mostly) vanilla stuff below ----------*/

    private static final Text REQUEST_HANDLED = Text.translatable("multiplayer.status.request_handled");
    private final ServerMetadata metadata;
    private final ClientConnection connection;
    private boolean responseSent;

    public UpgradedServerQueryNetworkHandler(ServerMetadata metadata, ClientConnection connection) {
        this.metadata = metadata;
        this.connection = connection;
    }

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
