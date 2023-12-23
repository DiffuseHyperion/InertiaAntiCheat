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
        InertiaAntiCheat.warn("Registering handler");
        ServerLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.KEY_COMMUNICATION_ID, ServerLoginHandler::serverKeyHandler);
    }

    public static void sendKeyRequest(ServerLoginNetworkHandler ignored1, MinecraftServer ignored2, PacketSender packetSender, ServerLoginNetworking.LoginSynchronizer ignored3) {
        InertiaAntiCheat.warn("Sending key request");
        packetSender.sendPacket(InertiaAntiCheatConstants.KEY_COMMUNICATION_ID, PacketByteBufs.empty());
    }

    public static void serverKeyHandler(MinecraftServer server, ServerLoginNetworkHandler handler, boolean understood, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender responseSender) {
        InertiaAntiCheat.warn("Handler triggered");
        if (!understood) {
            InertiaAntiCheat.error("Not understood");
            handler.disconnect(Text.of("did not respond to key request"));
            return;
        }
        InertiaAntiCheat.warn("Understood!");

        String ip = InertiaAntiCheat.getIP(((ServerLoginNetworkHandlerInterface) handler).inertiaAntiCheat$getConnection().getAddress());

        synchronizer.waitFor(server.submit(() -> {
            InertiaAntiCheat.warn("Communication bytes: " + buf.readableBytes());
            if (buf.readableBytes() <= 0) {
                handler.disconnect(Text.of("invalid key"));
            }
            UUID key = buf.readUuid();


            if (!generatedKeys.containsKey(ip)) {
                handler.disconnect(Text.of("no keys generated for this ip"));
            } else {
                if (!generatedKeys.get(ip).equals(key)) {
                    handler.disconnect(Text.of("key mismatch"));
                } else {
                    generatedKeys.remove(ip);
                }
            }
        }));
    }
}
