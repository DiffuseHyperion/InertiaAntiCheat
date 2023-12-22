package me.diffusehyperion.inertiaanticheat.mixins;

import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestEncryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.C2S.CommunicateRequestUnencryptedC2SPacket;
import me.diffusehyperion.inertiaanticheat.packets.S2C.*;
import me.diffusehyperion.inertiaanticheat.packets.C2S.ContactRequestC2SPacket;
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
        packetHandler.register(CommunicateRequestEncryptedC2SPacket.class, o -> new CommunicateRequestEncryptedC2SPacket((PacketByteBuf) o));
        packetHandler.register(CommunicateRequestUnencryptedC2SPacket.class, o -> new CommunicateRequestUnencryptedC2SPacket((PacketByteBuf) o));
        packetHandler.register(ContactRequestC2SPacket.class, o -> new ContactRequestC2SPacket((PacketByteBuf) o));

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
        packetSet.register(CommunicateResponseAcceptS2CPacket.class, o -> new CommunicateResponseAcceptS2CPacket((PacketByteBuf) o));
        packetSet.register(CommunicateResponseRejectS2CPacket.class, o -> new CommunicateResponseRejectS2CPacket((PacketByteBuf) o));
        packetSet.register(ContactResponseEncryptedS2CPacket.class, o -> new ContactResponseEncryptedS2CPacket((PacketByteBuf) o));
        packetSet.register(ContactResponseRejectS2CPacket.class, o -> new ContactResponseRejectS2CPacket((PacketByteBuf) o));
        packetSet.register(ContactResponseUnencryptedS2CPacket.class, o -> new ContactResponseUnencryptedS2CPacket((PacketByteBuf) o));
        return packetSet;
    }
}
