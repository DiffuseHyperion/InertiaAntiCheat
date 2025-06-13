package com.diffusehyperion.inertiaanticheat.interfaces;

import net.minecraft.text.Text;

import java.util.function.Consumer;

public interface UpgradedClientLoginNetworkHandler {
    void inertiaAntiCheat$setSecondaryStatusConsumer(Consumer<Text> consumer);

    Consumer<Text> inertiaAntiCheat$getSecondaryStatusConsumer();
}
