package me.diffusehyperion.inertiaanticheat.interfaces;

import me.diffusehyperion.inertiaanticheat.util.AnticheatDetails;

public interface ServerInfoInterface {
    AnticheatDetails inertiaAntiCheat$getAnticheatDetails();
    Boolean inertiaAntiCheat$isInertiaInstalled();

    void inertiaAntiCheat$setAnticheatDetails(AnticheatDetails anticheatDetails);
    void inertiaAntiCheat$setInertiaInstalled(Boolean value);
}
