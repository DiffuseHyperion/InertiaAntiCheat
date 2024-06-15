package me.diffusehyperion.inertiaanticheat.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.diffusehyperion.inertiaanticheat.packets.AnticheatPackets;
import me.diffusehyperion.inertiaanticheat.packets.S2C.AnticheatDetailsS2CPacket;
import me.diffusehyperion.inertiaanticheat.packets.UpgradedClientQueryNetworkHandler;
import net.minecraft.network.NetworkState;
import net.minecraft.network.NetworkStateBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.state.QueryStates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Purpose of this is to register the custom query response packet, otherwise io.netty.handler.codec.EncoderException will be thrown
 */
@Mixin(QueryStates.class)
public class QueryStatesMixin {

    @Inject(method = "method_56029", at = @At(value = "TAIL"))
    private static void registerClientbound(NetworkStateBuilder builder, CallbackInfo ci) {
        builder.add(AnticheatPackets.DETAILS_RESPONSE, AnticheatDetailsS2CPacket.CODEC);
    }
}
