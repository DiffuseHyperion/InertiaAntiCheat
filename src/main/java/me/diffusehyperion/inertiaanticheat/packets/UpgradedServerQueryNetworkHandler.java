package me.diffusehyperion.inertiaanticheat.packets;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestEncryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestUnencryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.CommunicateResponseS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseEncryptedS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseRejectS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.ContactResponseUnencryptedS2CPacket;
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
import java.util.List;
import java.util.Objects;

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
        InertiaAntiCheat.info("Received contact");
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
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(var1.getSerializedModlist().getBytes()));
            List<File> modFiles = (List<File>) ois.readObject();

            connection.send(new CommunicateResponseS2CPacket(checkModlist(modFiles)));
        } catch (IOException | ClassNotFoundException e) {
            InertiaAntiCheat.debugError("Something went wrong while deserializing a response packet!");
            InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");
            InertiaAntiCheat.debugException(e);

            connection.send(new CommunicateResponseS2CPacket(false));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        disconnectRunnable.run();
    }

    @Override
    public void onCommunicateEncryptedRequest(CommunicateRequestEncryptedC2SPacket var1) {
        try {
            SecretKey decryptedAESKey = new SecretKeySpec(InertiaAntiCheat.decryptRSABytes(var1.getEncrypytedRSAAESKey(), serverE2EEKeyPair.getPrivate()), "AES");
            byte[] decryptedSerializedModlistBytes = InertiaAntiCheat.decryptAESBytes(var1.getEncryptedAESSerializedModlist(), decryptedAESKey);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decryptedSerializedModlistBytes));
            List<File> modFiles = (List<File>) ois.readObject();

            connection.send(new CommunicateResponseS2CPacket(checkModlist(modFiles)));
        } catch (IOException | ClassNotFoundException e) {
            InertiaAntiCheat.debugError("Something went wrong while deserializing a response packet!");
            InertiaAntiCheat.debugError("This may be caused by a player modifying their response.");
            InertiaAntiCheat.debugException(e);

            connection.send(new CommunicateResponseS2CPacket(false));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        disconnectRunnable.run();
    }

    private boolean checkModlist(List<File> mods) throws Exception {
        if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.INDIVIDUAL) {
            List<String> blacklistedMods = serverConfig.getList("mods.individual.blacklist");
            List<String> whitelistedMods = serverConfig.getList("mods.individual.whitelist");
            for (File mod : mods) {
                String fileHash = InertiaAntiCheat.getHash(Files.readAllBytes(mod.toPath()), InertiaAntiCheatServer.hashAlgorithm);

                if (blacklistedMods.contains(fileHash)) {
                    return false;
                }
                if (!whitelistedMods.contains(fileHash)) {
                    return false;
                }
            }
            return true;
        } else if (InertiaAntiCheatServer.modlistCheckMethod == ModlistCheckMethod.GROUP) {
            StringBuilder combinedHashes = new StringBuilder();
            for (File mod : mods) {
                String fileHash = InertiaAntiCheat.getHash(Files.readAllBytes(mod.toPath()), InertiaAntiCheatServer.hashAlgorithm);
                combinedHashes.append(fileHash);
            }
            String finalHash = InertiaAntiCheat.getHash(combinedHashes.toString(), "MD5"); // no need to be cryptographically safe here
            return Objects.equals(serverConfig.getString("mods.group.hash"), finalHash);
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
