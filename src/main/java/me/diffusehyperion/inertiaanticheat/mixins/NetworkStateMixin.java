package me.diffusehyperion.inertiaanticheat.mixins;

import me.diffusehyperion.inertiaanticheat.packets.UpgradedQueryRequestC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedQueryResponseS2CPacket;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

/*
 * Purpose of this is to register the custom query request/response packet, otherwise io.netty.handler.codec.EncoderException will be thrown
 */
@Mixin(NetworkState.class)
public class NetworkStateMixin {
    @ModifyArg(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkState$PacketHandlerInitializer;setup(Lnet/minecraft/network/NetworkSide;Lnet/minecraft/network/NetworkState$InternalPacketHandler;)Lnet/minecraft/network/NetworkState$PacketHandlerInitializer;",
                    ordinal = 0 ), // serverbound
            index = 1,
            slice = @Slice(
                    from = @At(value = "CONSTANT",
                            args = "stringValue=status"
                    )
            ))
    private static NetworkState.InternalPacketHandler registerServerbound(NetworkState.InternalPacketHandler packetHandler) {
        packetHandler.register(UpgradedQueryRequestC2SPacket.class, o -> new UpgradedQueryRequestC2SPacket((PacketByteBuf) o));
        return packetHandler;
    }

    @ModifyArg(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkState$PacketHandlerInitializer;setup(Lnet/minecraft/network/NetworkSide;Lnet/minecraft/network/NetworkState$InternalPacketHandler;)Lnet/minecraft/network/NetworkState$PacketHandlerInitializer;",
                    ordinal = 1 ), // clientbound
            index = 1,
            slice = @Slice(
                    from = @At(value = "CONSTANT",
                            args = "stringValue=status"
                    )
            ))
    private static NetworkState.InternalPacketHandler registerClientbound(NetworkState.InternalPacketHandler packetSet) {
        packetSet.register(UpgradedQueryResponseS2CPacket.class, o -> new UpgradedQueryResponseS2CPacket((PacketByteBuf) o));
        return packetSet;
    }
}