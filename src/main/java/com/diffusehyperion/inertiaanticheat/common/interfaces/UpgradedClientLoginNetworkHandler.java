package com.diffusehyperion.inertiaanticheat.common.interfaces;

import net.minecraft.text.Text;

import java.util.function.Consumer;

public interface UpgradedClientLoginNetworkHandler {
    void inertiaAntiCheat$setSecondaryStatusConsumer(Consumer<Text> consumer);

    Consumer<Text> inertiaAntiCheat$getSecondaryStatusConsumer();
}
