package me.diffusehyperion.inertiaanticheat.packets.ModlistResponses;

import me.diffusehyperion.inertiaanticheat.packets.ModListResponse;

import javax.crypto.SecretKey;

public record ModlistEncryptedResponse(SecretKey secretKey, String modlist) implements ModListResponse {

    @Override
    public ModlistResponseTypes getType() {
        return ModlistResponseTypes.ENCRYPTED;
    }
}
