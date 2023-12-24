package me.diffusehyperion.inertiaanticheat.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.UUID;

public class ServerLoginHandler {
    public static HashMap<String, UUID> generatedKeys = new HashMap<>();

    public static void registerServerKeyHandler() {
        InertiaAntiCheat.debugInfo("Registering key communication handler");
        ServerLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.KEY_COMMUNICATION_ID, ServerLoginHandler::serverKeyHandler);
    }

    public static void sendKeyRequest(ServerLoginNetworkHandler serverLoginNetworkHandler, MinecraftServer ignored2, PacketSender packetSender, ServerLoginNetworking.LoginSynchronizer ignored3) {
        InertiaAntiCheat.debugLine();
        if (InertiaAntiCheat.inDebug()) {
            ServerLoginNetworkHandlerInterface upgradedServerLoginNetworkHandler = (ServerLoginNetworkHandlerInterface) serverLoginNetworkHandler;
            InertiaAntiCheat.debugInfo("Sending key request to address " + upgradedServerLoginNetworkHandler.inertiaAntiCheat$getConnection().getAddress());
        }
        packetSender.sendPacket(InertiaAntiCheatConstants.KEY_COMMUNICATION_ID, PacketByteBufs.empty());
        InertiaAntiCheat.debugLine();
    }

    public static void serverKeyHandler(MinecraftServer server, ServerLoginNetworkHandler handler, boolean understood, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender responseSender) {
        ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) handler;
        InertiaAntiCheat.debugInfo("Received key from address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress());
        if (!understood) {
            InertiaAntiCheat.debugWarn("Client did not understand key request - most likely due to not having the mod installed");
            handler.disconnect(Text.of("did not respond to key request"));
            return;
        }
        InertiaAntiCheat.debugInfo("Client understood request!");

        String ip = InertiaAntiCheat.getIP(upgradedHandler.inertiaAntiCheat$getConnection().getAddress());

        synchronizer.waitFor(server.submit(() -> {
            if (buf.readableBytes() <= 0) {
                InertiaAntiCheat.debugInfo("Received no data from the client");
                handler.disconnect(Text.of("invalid key"));
                return;
            }
            UUID key = buf.readUuid();

            if (!generatedKeys.containsKey(ip)) {
                InertiaAntiCheat.debugInfo("Client did not previously generate a key");
                handler.disconnect(Text.of("no keys generated for this ip"));
                return;
            }
            if (!generatedKeys.get(ip).equals(key)) {
                InertiaAntiCheat.debugInfo("Client provided an invalid key");
                handler.disconnect(Text.of("key mismatch"));
                return;
            }

            InertiaAntiCheat.debugInfo("Passed");
            generatedKeys.remove(ip);
            InertiaAntiCheat.debugLine();
        }));
    }
}
