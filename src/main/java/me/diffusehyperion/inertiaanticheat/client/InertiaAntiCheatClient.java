package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.packets.ModListRequestS2CPayload;
import me.diffusehyperion.inertiaanticheat.packets.ModListResponseC2SPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION;

public class InertiaAntiCheatClient implements ClientModInitializer {

    public static Toml clientConfig;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(ModListRequestS2CPayload.ID, ModListRequestS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModListResponseC2SPayload.ID, ModListResponseC2SPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ModListRequestS2CPayload.ID, ModListRequestS2CPayload::onReceive);
        clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", CURRENT_CLIENT_CONFIG_VERSION);
    }
}
