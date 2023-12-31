package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.util.AnticheatDetails;
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
