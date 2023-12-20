package me.diffusehyperion.inertiaanticheat.interfaces;

public interface ServerInfoInterface {
    boolean inertiaAntiCheat$allowedToJoin();

    boolean inertiaAntiCheat$isInertiaInstalled();

    void inertiaAntiCheat$setAllowedToJoin(boolean value);


    void inertiaAntiCheat$setInertiaInstalled(boolean value);
}
