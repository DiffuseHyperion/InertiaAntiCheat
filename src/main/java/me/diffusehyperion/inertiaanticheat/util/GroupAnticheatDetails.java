package me.diffusehyperion.inertiaanticheat.util;

public record GroupAnticheatDetails(String modpackName) implements AnticheatDetails{
    @Override
    public ModlistCheckMethod getCheckMethod() {
        return ModlistCheckMethod.GROUP;
    }
}
