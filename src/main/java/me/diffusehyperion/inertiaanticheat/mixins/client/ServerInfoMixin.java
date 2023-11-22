package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerInfo.class)
public abstract class ServerInfoMixin implements ServerInfoInterface {
    @Unique
    private boolean inertiaInstalled;
    @Override
    public boolean inertiaAntiCheat$isInertiaInstalled() {
        return inertiaInstalled;
    }

    @Override
    public void inertiaAntiCheat$setInertiaInstalled(boolean value) {
        this.inertiaInstalled = value;
    }
}
