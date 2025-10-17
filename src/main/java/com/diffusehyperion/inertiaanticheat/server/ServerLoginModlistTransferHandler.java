package com.diffusehyperion.inertiaanticheat.server;

import com.diffusehyperion.inertiaanticheat.common.InertiaAntiCheat;
import com.diffusehyperion.inertiaanticheat.common.interfaces.UpgradedServerLoginNetworkHandler;
import com.diffusehyperion.inertiaanticheat.common.util.InertiaAntiCheatConstants;
import com.diffusehyperion.inertiaanticheat.server.networking.method.ValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.ServerDataGroupValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.ServerDataIndividualValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.ServerDataReceiverHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.data.handlers.DataValidationHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.id.ServerIdGroupValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.id.ServerIdIndividualValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.id.ServerIdReceiverHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.id.handlers.IdValidationHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.name.ServerNameGroupValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.name.ServerNameIndividualValidatorHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.name.ServerNameReceiverHandler;
import com.diffusehyperion.inertiaanticheat.server.networking.method.name.handlers.NameValidationHandler;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;

import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugInfo;
import static com.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.debugLine;

public class ServerLoginModlistTransferHandler {
    private KeyPair serverKeyPair;
    private PublicKey clientKey;

    private final CompletableFuture<Void> loginBlocker = new CompletableFuture<>();

    public static void init() {
        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginModlistTransferHandler::initiateConnection);
    }

    /**
     * Creates an instance of this class to have an instance of loginBlocker to delay logins
     * Afterward, this does preliminary checks to see if the client has permissions to bypass the mod
     * If not, sends a packet to check if the client understands custom packets from this mod
     */
    private static void initiateConnection(ServerLoginNetworkHandler handler, MinecraftServer minecraftServer, LoginPacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        UpgradedServerLoginNetworkHandler upgradedHandler = (UpgradedServerLoginNetworkHandler) handler;

        ServerLoginModlistTransferHandler transferHandler = new ServerLoginModlistTransferHandler();
        synchronizer.waitFor(transferHandler.loginBlocker);

        debugLine();
        debugInfo("Checking if " + handler.getConnectionInfo() + " has bypass permissions");
        boolean allowed = Permissions.check(upgradedHandler.inertiaAntiCheat$getGameProfile(), "inertiaanticheat.bypass").join();
        if (allowed) {
            debugInfo(handler.getConnectionInfo() + " is allowed to bypass");
            debugLine();
            transferHandler.loginBlocker.complete(null);
            return;
        }
        debugInfo("Not allowed to bypass, checking if address " + handler.getConnectionInfo() + " responds to mod messages");

        ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.CHECK_CONNECTION, transferHandler::checkConnection);
        sender.sendPacket(InertiaAntiCheatConstants.CHECK_CONNECTION, PacketByteBufs.empty());
    }

    /**
     * Confirms whether the client understood the custom packet (meaning he has inertia installed too)
     * Afterward, this starts the key exchanging process
     */
    private void
    checkConnection(MinecraftServer minecraftServer, ServerLoginNetworkHandler handler,
                    boolean b, PacketByteBuf buf,
                    ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        LoginPacketSender sender = (LoginPacketSender) packetSender;

        if (!b) {
            debugInfo(handler.getConnectionInfo() + " does not respond to mod messages, kicking now");
            handler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("validation.vanillaKickMessage")));
            return;
        }
        debugInfo(handler.getConnectionInfo() + " responds to mod messages, creating handler");


        PacketByteBuf response = PacketByteBufs.create();
        KeyPair keyPair = InertiaAntiCheat.createRSAPair();
        this.serverKeyPair = keyPair;
        response.writeBytes(keyPair.getPublic().getEncoded());

        ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.INITIATE_E2EE, this::setAdaptor);
        sender.sendPacket(InertiaAntiCheatConstants.INITIATE_E2EE, response);
    }

    /**
     * Retrieves and stores the client's public key
     * Afterward, inform client on which transfer method to use
     */
    private void
    setAdaptor(MinecraftServer server, ServerLoginNetworkHandler handler,
               boolean b, PacketByteBuf buf,
               ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        debugInfo("Received " + handler.getConnectionInfo() + " keypair");
        LoginPacketSender sender = (LoginPacketSender) packetSender;

        this.clientKey = InertiaAntiCheat.retrievePublicKey(buf);

        PacketByteBuf response = PacketByteBufs.create();

        response.writeInt(InertiaAntiCheatServer.transferMethod.ordinal());

        ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.SET_ADAPTOR, this::beginModTransfer);
        sender.sendPacket(InertiaAntiCheatConstants.SET_ADAPTOR, response);
    }

    /**
     * Creates transfer and validator adaptor instances
     */
    private void
    beginModTransfer(MinecraftServer server, ServerLoginNetworkHandler handler,
               boolean b, PacketByteBuf packetByteBuf,
               ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        LoginPacketSender sender = (LoginPacketSender) packetSender;
        UpgradedServerLoginNetworkHandler upgradedHandler = (UpgradedServerLoginNetworkHandler) handler;

        Runnable failureTask = () -> {
            debugInfo("Address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " failed modlist check");
            handler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("validation.deniedKickMessage")));
        };
        Runnable successTask = () -> debugInfo("Address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " passed modlist check");
        Runnable finishTask = () -> {
            debugInfo("Finishing transfer, checking mods now");
            ServerLoginNetworking.unregisterReceiver(handler, InertiaAntiCheatConstants.SEND_MOD);
        };

        ValidatorHandler validatorAdaptor;

        switch (InertiaAntiCheatServer.transferMethod) {
            case DATA -> {
                validatorAdaptor = switch (InertiaAntiCheatServer.validationMethod) {
                    case INDIVIDUAL ->
                            new ServerDataIndividualValidatorHandler(failureTask, successTask, finishTask);
                    case GROUP ->
                            new ServerDataGroupValidatorHandler(failureTask, successTask, finishTask);
                };

                new ServerDataReceiverHandler(this.serverKeyPair, InertiaAntiCheatConstants.SEND_MOD, handler, (DataValidationHandler) validatorAdaptor);
                sender.sendPacket(InertiaAntiCheatConstants.SEND_MOD, PacketByteBufs.empty());
            }
            case NAME -> {
                validatorAdaptor = switch (InertiaAntiCheatServer.validationMethod) {
                    case INDIVIDUAL ->
                            new ServerNameIndividualValidatorHandler(failureTask, successTask, finishTask);
                    case GROUP ->
                            new ServerNameGroupValidatorHandler(failureTask, successTask, finishTask);
                };

                new ServerNameReceiverHandler(this.serverKeyPair, InertiaAntiCheatConstants.SEND_MOD, handler, (NameValidationHandler) validatorAdaptor);
                sender.sendPacket(InertiaAntiCheatConstants.SEND_MOD, PacketByteBufs.empty());
            }
            case ID -> {
                validatorAdaptor = switch (InertiaAntiCheatServer.validationMethod) {
                    case INDIVIDUAL ->
                            new ServerIdIndividualValidatorHandler(failureTask, successTask, finishTask);
                    case GROUP ->
                            new ServerIdGroupValidatorHandler(failureTask, successTask, finishTask);
                };

                new ServerIdReceiverHandler(this.serverKeyPair, InertiaAntiCheatConstants.SEND_MOD, handler, (IdValidationHandler) validatorAdaptor);
                sender.sendPacket(InertiaAntiCheatConstants.SEND_MOD, PacketByteBufs.empty());
            }
            default -> // should never happen since this would get caught in server initialization, but java needs this
                    throw new RuntimeException("Invalid or no given checking method type given in server config!");
        }

        validatorAdaptor.future.whenComplete((ignored1, ignored2) -> this.loginBlocker.complete(null));
        debugLine();
    }
}
