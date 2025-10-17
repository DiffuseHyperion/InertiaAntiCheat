package com.diffusehyperion.inertiaanticheat.mixins.client;

import com.diffusehyperion.inertiaanticheat.common.interfaces.UpgradedServerInfo;
import com.diffusehyperion.inertiaanticheat.common.util.AnticheatDetails;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerInfo.class)
public abstract class ServerInfoMixin implements UpgradedServerInfo {
    @Unique
    @Nullable
    private Boolean inertiaInstalled;
    @Unique
    @Nullable
    private AnticheatDetails anticheatDetails;

    @Override
    public AnticheatDetails inertiaAntiCheat$getAnticheatDetails() {
        return this.anticheatDetails;
    }

    @Override
    @Nullable
    public Boolean inertiaAntiCheat$isInertiaInstalled() {
        return this.inertiaInstalled;
    }

    @Override
    public void inertiaAntiCheat$setAnticheatDetails(AnticheatDetails anticheatDetails) {
        this.anticheatDetails = anticheatDetails;
    }

    @Override
    public void inertiaAntiCheat$setInertiaInstalled(@Nullable Boolean value) {
        this.inertiaInstalled = value;
    }
}
