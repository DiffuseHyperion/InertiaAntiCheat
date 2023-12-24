package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants.MODID;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class MultiplayerServerListWidgetServerEntryMixin {
    @Shadow @Final private MultiplayerScreen screen;
    @Shadow @Final private ServerInfo server;

    @Unique
    private final Identifier ICON_ENABLED = new Identifier(MODID, "textures/gui/enabled.png");
    @Unique
    private final Identifier ICON_ALLOWED = new Identifier(MODID, "textures/gui/tick.png");
    @Unique
    private final Identifier ICON_DISALLOWED = new Identifier(MODID, "textures/gui/cross.png");

    @Unique
    private final List<Text> ICON_ENABLED_TEXT = new ArrayList<>();
    @Unique
    private final List<Text> ICON_ALLOWED_TEXT = new ArrayList<>();
    @Unique
    private final List<Text> ICON_DISALLOWED_TEXT = new ArrayList<>();

    public MultiplayerServerListWidgetServerEntryMixin() {
        this.ICON_ENABLED_TEXT.add(Text.of("InertiaAntiCheat installed"));
        this.ICON_ALLOWED_TEXT.add(Text.of("Allowed to join"));
        this.ICON_DISALLOWED_TEXT.add(Text.of("Not allowed to join"));
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I")
    )
    private void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        ServerInfoInterface upgradedServerInfo = ((ServerInfoInterface) server);
        if (Objects.nonNull(upgradedServerInfo.inertiaAntiCheat$isInertiaInstalled()) && upgradedServerInfo.inertiaAntiCheat$isInertiaInstalled().equals(true)) {
            int iconX = x + entryWidth - 15;
            int iconY = y + 10;
            context.drawTexture(ICON_ENABLED, iconX, iconY, 0.0f, 0.0f, 10, 10, 10, 10);
            if (mouseX > iconX && mouseX < iconX + 10 && mouseY > iconY && mouseY < iconY + 10) {
                screen.setMultiplayerScreenTooltip(ICON_ENABLED_TEXT);
            }
        }
        if (Objects.nonNull(upgradedServerInfo.inertiaAntiCheat$allowedToJoin())) {
            if (upgradedServerInfo.inertiaAntiCheat$allowedToJoin()) {
                int iconX = x + entryWidth - 15;
                int iconY = y + 20;
                context.drawTexture(ICON_ALLOWED, iconX, iconY, 0.0f, 0.0f, 10, 10, 10, 10);
                if (mouseX > iconX && mouseX < iconX + 10 && mouseY > iconY && mouseY < iconY + 10) {
                    screen.setMultiplayerScreenTooltip(ICON_ALLOWED_TEXT);
                }
            } else {
                int iconX = x + entryWidth - 15;
                int iconY = y + 20;
                context.drawTexture(ICON_DISALLOWED, iconX, iconY, 0.0f, 0.0f, 10, 10, 10, 10);
                if (mouseX > iconX && mouseX < iconX + 10 && mouseY > iconY && mouseY < iconY + 10) {
                    screen.setMultiplayerScreenTooltip(ICON_DISALLOWED_TEXT);
                }
            }

        }
    }
}
