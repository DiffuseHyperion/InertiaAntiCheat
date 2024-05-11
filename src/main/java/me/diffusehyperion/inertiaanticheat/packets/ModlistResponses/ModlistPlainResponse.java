package me.diffusehyperion.inertiaanticheat.packets.ModlistResponses;

import me.diffusehyperion.inertiaanticheat.packets.ModListResponse;

public record ModlistPlainResponse(String modlist) implements ModListResponse {
    @Override
    public ModlistResponseTypes getType() {
        return ModlistResponseTypes.PLAIN;
    }
}
