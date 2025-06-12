package com.diffusehyperion.inertiaanticheat.util;

public abstract class AnticheatDetails {
    private final boolean showInstalled;

    public abstract ModlistCheckMethod getCheckMethod();

    public AnticheatDetails(boolean showInstalled) {
        this.showInstalled = showInstalled;
    }

    public boolean showInstalled() {
        return showInstalled;
    }
}
