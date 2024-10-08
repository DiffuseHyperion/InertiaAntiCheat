package me.diffusehyperion.inertiaanticheat.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.TransferAdaptors;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.server.ServerDataTransferAdaptor;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.server.ServerModlistTransferAdaptor;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.validator.IndividualValidatorAdaptor;
import me.diffusehyperion.inertiaanticheat.networking.adaptors.validator.ServerModlistValidatorAdaptor;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;

import java.security.KeyPair;
import java.security.PublicKey;

public class ServerLoginModlistTransferHandler {
    private KeyPair serverKeyPair;
    private PublicKey clientKey;

    public static void init() {
        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginModlistTransferHandler::initiateConnection);
    }

    /**
     * Does preliminary checks to see if the client has permissions to bypass the mod
     * If not, sends a packet to check if the client understands custom packets from this mod
     */
    private static void initiateConnection(ServerLoginNetworkHandler handler, MinecraftServer minecraftServer, LoginPacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        synchronizer.waitFor(minecraftServer.submit(() -> {
            ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) handler;

            InertiaAntiCheat.debugLine();
            InertiaAntiCheat.debugInfo("Checking if " + upgradedHandler.inertiaAntiCheat$getGameProfile().getName() + " has bypass permissions");
            boolean allowed = Permissions.check(upgradedHandler.inertiaAntiCheat$getGameProfile(), "inertiaanticheat.bypass").join();
            if (allowed) {
                InertiaAntiCheat.debugInfo(upgradedHandler.inertiaAntiCheat$getGameProfile().getName() + " is allowed to bypass");
                InertiaAntiCheat.debugLine();
                return;
            }
            InertiaAntiCheat.debugInfo("Not allowed to bypass, checking if address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " responds to mod messages");

            ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.CHECK_CONNECTION, ServerLoginModlistTransferHandler::checkConnection);
            sender.sendPacket(InertiaAntiCheatConstants.CHECK_CONNECTION, PacketByteBufs.empty());
        }));
    }

    /**
     * Confirms whether the client understood the custom packet (meaning he has inertia installed too)
     * Afterward, this creates an instance of this class and starts the key exchanging process
     */
    private static void
    checkConnection(MinecraftServer minecraftServer, ServerLoginNetworkHandler handler,
                    boolean b, PacketByteBuf buf,
                    ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        synchronizer.waitFor(minecraftServer.submit(() -> {
            LoginPacketSender sender = (LoginPacketSender) packetSender;
            ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) handler;

            if (!b) {
                InertiaAntiCheat.debugInfo("Address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " does not respond to mod messages, kicking now");
                handler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("mods.vanillaKickMessage")));
                return;
            }
            InertiaAntiCheat.debugInfo("Address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " responds to mod messages, creating handler");

            ServerLoginModlistTransferHandler transferHandler = new ServerLoginModlistTransferHandler();

            PacketByteBuf response = PacketByteBufs.create();
            KeyPair keyPair = InertiaAntiCheat.createRSAPair();
            transferHandler.serverKeyPair = keyPair;
            response.writeBytes(keyPair.getPublic().getEncoded());

            ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.INITIATE_E2EE, transferHandler::setAdaptor);
            sender.sendPacket(InertiaAntiCheatConstants.INITIATE_E2EE, response);
        }));
    }

    /**
     * Retrieves and stores the client's public key
     * Afterward, inform client on which adaptor to use
     */
    private void
    setAdaptor(MinecraftServer server, ServerLoginNetworkHandler handler,
               boolean b, PacketByteBuf buf,
               ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        synchronizer.waitFor(server.submit(() -> {
            LoginPacketSender sender = (LoginPacketSender) packetSender;

            this.clientKey = InertiaAntiCheat.retrievePublicKey(buf);

            PacketByteBuf response = PacketByteBufs.create();

            // TODO: switch from hardcode to config
            response.writeInt(TransferAdaptors.DATA.ordinal());

            ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.SET_ADAPTOR, this::beginModTransfer);
            sender.sendPacket(InertiaAntiCheatConstants.SET_ADAPTOR, response);
            InertiaAntiCheat.debugLine();
        }));
    }

    /**
     * Creates transfer and validator adaptor instances
     */
    private void
    beginModTransfer(MinecraftServer server, ServerLoginNetworkHandler handler,
               boolean b, PacketByteBuf packetByteBuf,
               ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender packetSender) {
        LoginPacketSender sender = (LoginPacketSender) packetSender;
        ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) handler;

        synchronizer.waitFor(server.submit(() -> {

            ServerModlistValidatorAdaptor validatorHandler = new IndividualValidatorAdaptor(
                    () -> {
                        InertiaAntiCheat.debugInfo("Address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " failed modlist check");
                        handler.disconnect(Text.of(InertiaAntiCheatServer.serverConfig.getString("mods.deniedKickMessage")));
                    },
                    () -> {
                        InertiaAntiCheat.debugInfo("Address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress() + " passed modlist check");
                    },
                    () -> {
                        InertiaAntiCheat.debugInfo("Finishing transfer, checking mods now");
                        ServerLoginNetworking.unregisterReceiver(handler, InertiaAntiCheatConstants.SEND_MOD);
                    }
            );

            // TODO: switch from hardcode to config
            ServerModlistTransferAdaptor transferHandler = new ServerDataTransferAdaptor(this.serverKeyPair, InertiaAntiCheatConstants.SEND_MOD, validatorHandler);

            ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.SEND_MOD, transferHandler::transferMod);
            sender.sendPacket(InertiaAntiCheatConstants.SEND_MOD, PacketByteBufs.empty());

            synchronizer.waitFor(validatorHandler.future);
        }));
        InertiaAntiCheat.debugLine();
    }
}
