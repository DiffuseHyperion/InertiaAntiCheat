package com.diffusehyperion.inertiaanticheat.mixins.client;

import com.diffusehyperion.inertiaanticheat.interfaces.UpgradedClientLoginNetworkHandler;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

@Mixin(ClientLoginNetworkHandler.class)
public abstract class ClientLoginNetworkHandlerMixin implements UpgradedClientLoginNetworkHandler {
    @Unique
    private Consumer<Text> secondaryStatusConsumer = (Text text) -> {
        throw new RuntimeException("Tried setting secondary status to " + text + " when it was uninitialized");
    };

    @Override
    public void inertiaAntiCheat$setSecondaryStatusConsumer(Consumer<Text> consumer) {
        this.secondaryStatusConsumer = consumer;
    }

    @Override
    public Consumer<Text> inertiaAntiCheat$getSecondaryStatusConsumer() {
        return this.secondaryStatusConsumer;
    }
}
