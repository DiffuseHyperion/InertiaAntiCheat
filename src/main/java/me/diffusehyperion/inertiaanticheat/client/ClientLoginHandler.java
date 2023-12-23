package me.diffusehyperion.inertiaanticheat.client;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ClientLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClientLoginHandler {
    public static void registerClientKeyHandler() {
        InertiaAntiCheat.warn("Registering handler");
        ClientLoginNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.KEY_COMMUNICATION_ID, ClientLoginHandler::clientKeyHandler);
    }

    public static CompletableFuture<PacketByteBuf> clientKeyHandler(MinecraftClient client, ClientLoginNetworkHandler clientLoginNetworkHandler, PacketByteBuf packetByteBuf, Consumer<GenericFutureListener<? extends Future<? super Void>>> genericFutureListenerConsumer) {
        InertiaAntiCheat.warn("Handler triggered");
        ServerInfo info = ((ClientLoginNetworkHandlerInterface) clientLoginNetworkHandler).inertiaAntiCheat$getServerInfo();
        UUID key = InertiaAntiCheatClient.storedKeys.get(info);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(key);

        return CompletableFuture.completedFuture(PacketByteBufs.create().writeUuid(key));
    }
}
