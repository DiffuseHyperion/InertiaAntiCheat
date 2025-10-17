package com.diffusehyperion.inertiaanticheat.common.interfaces;

import com.diffusehyperion.inertiaanticheat.common.util.AnticheatDetails;

public interface UpgradedServerInfo {
    AnticheatDetails inertiaAntiCheat$getAnticheatDetails();
    Boolean inertiaAntiCheat$isInertiaInstalled();

    void inertiaAntiCheat$setAnticheatDetails(AnticheatDetails anticheatDetails);
    void inertiaAntiCheat$setInertiaInstalled(Boolean value);
}
