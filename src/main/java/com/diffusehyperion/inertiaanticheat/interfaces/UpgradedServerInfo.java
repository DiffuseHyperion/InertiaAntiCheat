package com.diffusehyperion.inertiaanticheat.interfaces;

import com.diffusehyperion.inertiaanticheat.util.AnticheatDetails;

public interface UpgradedServerInfo {
    AnticheatDetails inertiaAntiCheat$getAnticheatDetails();
    Boolean inertiaAntiCheat$isInertiaInstalled();

    void inertiaAntiCheat$setAnticheatDetails(AnticheatDetails anticheatDetails);
    void inertiaAntiCheat$setInertiaInstalled(Boolean value);
}
