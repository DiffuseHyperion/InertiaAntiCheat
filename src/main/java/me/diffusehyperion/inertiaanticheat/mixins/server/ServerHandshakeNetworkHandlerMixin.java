package me.diffusehyperion.inertiaanticheat.mixins.server;

import com.llamalad7.mixinextras.sugar.Local;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedServerQueryNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerHandshakeNetworkHandler.class)
public class ServerHandshakeNetworkHandlerMixin {
    @Final
    @Shadow
    private ClientConnection connection;

    @ModifyArg(method = "onHandshake",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V",
                    ordinal = 1))
    private PacketListener onHandshake(PacketListener packetListener, @Local ServerMetadata serverMetadata) {
        return new UpgradedServerQueryNetworkHandler(serverMetadata, connection);
    }

}
