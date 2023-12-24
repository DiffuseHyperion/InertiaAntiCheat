package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerInfo.class)
public abstract class ServerInfoMixin implements ServerInfoInterface {
    @Unique
    @Nullable
    private Boolean inertiaInstalled;
    @Unique
    @Nullable
    private Boolean allowedToJoin;

    @Override
    @Nullable
    public Boolean inertiaAntiCheat$allowedToJoin() {
        return this.allowedToJoin;
    }
    @Override
    @Nullable
    public Boolean inertiaAntiCheat$isInertiaInstalled() {
        return this.inertiaInstalled;
    }

    @Override
    public void inertiaAntiCheat$setAllowedToJoin(@Nullable Boolean value) {
        this.allowedToJoin = value;
    }
    @Override
    public void inertiaAntiCheat$setInertiaInstalled(@Nullable Boolean value) {
        this.inertiaInstalled = value;
    }
}
