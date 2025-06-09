package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ClientConnectionMixinInterface;
import me.diffusehyperion.inertiaanticheat.networking.packets.UpgradedClientQueryPacketListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.state.NetworkState;
import net.minecraft.network.listener.ClientPacketListener;
import net.minecraft.network.listener.ServerPacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.state.QueryStates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements ClientConnectionMixinInterface {
    @Unique
    @Override
    public void inertiaAntiCheat$connect(String address, int i, UpgradedClientQueryPacketListener upgradedClientQueryPacketListener) {
        this.connect(address, i, QueryStates.C2S, QueryStates.S2C, upgradedClientQueryPacketListener, ConnectionIntent.STATUS);
    }

    @Shadow
    private <S extends ServerPacketListener, C extends ClientPacketListener> void connect(String address, int port, NetworkState<S> outboundState, NetworkState<C> inboundState, C prePlayStateListener, ConnectionIntent intent) {}
}
