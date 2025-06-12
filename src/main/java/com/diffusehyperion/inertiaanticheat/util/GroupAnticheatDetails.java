package com.diffusehyperion.inertiaanticheat.util;

import java.util.List;

public class GroupAnticheatDetails extends AnticheatDetails {
    private final List<String> modpackDetails;

    public GroupAnticheatDetails(boolean showInstalled, List<String> modpackDetails) {
        super(showInstalled);
        this.modpackDetails = modpackDetails;
    }

    public List<String> getModpackDetails() {
        return modpackDetails;
    }

    @Override
    public ModlistCheckMethod getCheckMethod() {
        return ModlistCheckMethod.GROUP;
    }
}
