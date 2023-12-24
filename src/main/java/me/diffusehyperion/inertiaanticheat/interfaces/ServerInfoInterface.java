package me.diffusehyperion.inertiaanticheat.interfaces;

public interface ServerInfoInterface {
    Boolean inertiaAntiCheat$allowedToJoin();
    Boolean inertiaAntiCheat$isInertiaInstalled();

    void inertiaAntiCheat$setAllowedToJoin(Boolean value);
    void inertiaAntiCheat$setInertiaInstalled(Boolean value);
}
