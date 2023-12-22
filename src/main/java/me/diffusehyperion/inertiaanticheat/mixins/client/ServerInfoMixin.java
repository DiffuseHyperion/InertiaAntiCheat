package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerInfo.class)
public abstract class ServerInfoMixin implements ServerInfoInterface {
    @Unique
    private boolean inertiaInstalled;

    @Unique
    private boolean allowedToJoin;

    @Override
    public boolean inertiaAntiCheat$allowedToJoin() {
        return this.allowedToJoin;
    }

    @Override
    public boolean inertiaAntiCheat$isInertiaInstalled() {
        return this.inertiaInstalled;
    }

    @Override
    public void inertiaAntiCheat$setAllowedToJoin(boolean value) {
        this.allowedToJoin = value;
    }

    @Override
    public void inertiaAntiCheat$setInertiaInstalled(boolean value) {
        this.inertiaInstalled = value;
    }
}
