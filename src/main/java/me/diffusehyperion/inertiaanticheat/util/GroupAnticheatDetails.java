package me.diffusehyperion.inertiaanticheat.util;

import java.util.List;

public record GroupAnticheatDetails(List<String> modpackDetails) implements AnticheatDetails{
    @Override
    public ModlistCheckMethod getCheckMethod() {
        return ModlistCheckMethod.GROUP;
    }
}
