package me.diffusehyperion.inertiaanticheat.util;

import java.util.List;

public record IndividualAnticheatDetails(List<String> blacklistedMods,
                                         List<String> whitelistedMods) implements AnticheatDetails {

    @Override
    public ModlistCheckMethod getCheckMethod() {
        return ModlistCheckMethod.INDIVIDUAL;
    }
}
