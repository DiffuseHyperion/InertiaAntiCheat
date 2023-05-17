package me.diffusehyperion.inertiaanticheat.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.packets.ModListResponseC2SPacket;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.LOGGER;

public class InertiaAntiCheatServer implements DedicatedServerModInitializer {

    public static HashMap<UUID, Pair<Long, ServerPlayerEntity>> impendingPlayers = new HashMap<>();
    @Override
    public void onInitializeServer() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }
            long timeToWait = 1000;
            UUID uuid = UUID.randomUUID();
            // since we cannot uniquely get an identifier from PacketSender, we make our own one lol
            impendingPlayers.put(uuid, new Pair<>(System.currentTimeMillis() + timeToWait, player));
            player.networkHandler.sendPacket(ServerPlayNetworking.createS2CPacket(InertiaAntiCheatConstants.REQUEST_PACKET_ID, PacketByteBufs.create().writeUuid(uuid)));
            LOGGER.info("Someone joined the server!");
        });

        ServerPlayNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.RESPONSE_PACKET_ID, ModListResponseC2SPacket::receive);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Iterator<Map.Entry<UUID, Pair<Long, ServerPlayerEntity>>> it = impendingPlayers.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Pair<Long, ServerPlayerEntity>> entry = it.next();
                if (entry.getValue().getA() <= System.currentTimeMillis()) {
                    entry.getValue().getB().networkHandler.sendPacket(new DisconnectS2CPacket(Text.of("Disconnect grahhh")));
                    it.remove();
                }
            }
        });
    }
}
