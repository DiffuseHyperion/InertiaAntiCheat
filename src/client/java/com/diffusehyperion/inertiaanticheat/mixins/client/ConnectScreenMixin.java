package com.diffusehyperion.inertiaanticheat.mixins.client;

import com.diffusehyperion.inertiaanticheat.common.interfaces.UpgradedConnectScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen implements UpgradedConnectScreen {
    @Unique
    private @Nullable Text secondaryStatus;

    protected ConnectScreenMixin(Text title) {
        super(title);
    }

    @Unique
    @Override
    public void inertiaAntiCheat$setSecondaryStatus(@Nullable Text secondaryStatus) {
        this.secondaryStatus = secondaryStatus;
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void render(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (Objects.nonNull(this.secondaryStatus)) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.secondaryStatus, this.width / 2, this.height / 2 - 35, 16777215);
        }
    }
}
