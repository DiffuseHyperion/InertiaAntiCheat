package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.interfaces.ServerInfoInterface;
import me.diffusehyperion.inertiaanticheat.util.AnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
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
    private final Identifier ICON_WHITELIST = new Identifier(MODID, "textures/gui/whitelist.png");
    @Unique
    private final Identifier ICON_BLACKLIST = new Identifier(MODID, "textures/gui/blacklist.png");
    @Unique
    private final Identifier ICON_MODPACK = new Identifier(MODID, "textures/gui/modpack.png");

    @Inject(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I")
    )
    private void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        ServerInfoInterface upgradedServerInfo = ((ServerInfoInterface) server);
        Boolean installed = upgradedServerInfo.inertiaAntiCheat$isInertiaInstalled();
        AnticheatDetails anticheatDetails = upgradedServerInfo.inertiaAntiCheat$getAnticheatDetails();
        if (Objects.nonNull(installed) && installed.equals(true) && anticheatDetails.showInstalled()) {
            int iconX = x + entryWidth - 15;
            int iconY = y + 10;
            context.drawTexture(ICON_ENABLED, iconX, iconY, 0.0f, 0.0f, 10, 10, 10, 10);
            if (mouseX > iconX && mouseX < iconX + 10 && mouseY > iconY && mouseY < iconY + 10) {
                screen.setMultiplayerScreenTooltip(List.of(Text.of("InertiaAntiCheat installed")));
            }
        }
        if (Objects.nonNull(anticheatDetails)) {
            if (anticheatDetails.getCheckMethod() == ModlistCheckMethod.INDIVIDUAL) {
                IndividualAnticheatDetails details = (IndividualAnticheatDetails) anticheatDetails;

                if ((details.getWhitelistedMods().size() == 1 && !Objects.equals(details.getWhitelistedMods().get(0), "")) || details.getWhitelistedMods().size() >= 2) {
                    int whitelistIconX = x + entryWidth - 25;
                    int whitelistIconY = y + 20;
                    context.drawTexture(ICON_WHITELIST, whitelistIconX, whitelistIconY, 0.0f, 0.0f, 10, 10, 10, 10);
                    if (mouseX > whitelistIconX && mouseX < whitelistIconX + 10 && mouseY > whitelistIconY && mouseY < whitelistIconY + 10) {
                        screen.setMultiplayerScreenTooltip(details.getWhitelistedMods().stream().map(Text::of).toList());
                    }
                }

                if ((details.getBlacklistedMods().size() == 1 && !Objects.equals(details.getBlacklistedMods().get(0), "")) || details.getBlacklistedMods().size() >= 2) {
                    int blacklistIconX = x + entryWidth - 15;
                    int blacklistIconY = y + 20;
                    context.drawTexture(ICON_BLACKLIST, blacklistIconX, blacklistIconY, 0.0f, 0.0f, 10, 10, 10, 10);
                    if (mouseX > blacklistIconX && mouseX < blacklistIconX + 10 && mouseY > blacklistIconY && mouseY < blacklistIconY + 10) {
                        screen.setMultiplayerScreenTooltip(details.getBlacklistedMods().stream().map(Text::of).toList());
                    }
                }
            } else {
                GroupAnticheatDetails details = (GroupAnticheatDetails) anticheatDetails;
                if ((details.getModpackDetails().size() == 1 && !Objects.equals(details.getModpackDetails().get(0), "")) || details.getModpackDetails().size() >= 2) {
                    int blacklistIconX = x + entryWidth - 15;
                    int blacklistIconY = y + 20;
                    context.drawTexture(ICON_MODPACK, blacklistIconX, blacklistIconY, 0.0f, 0.0f, 10, 10, 10, 10);
                    if (mouseX > blacklistIconX && mouseX < blacklistIconX + 10 && mouseY > blacklistIconY && mouseY < blacklistIconY + 10) {
                        screen.setMultiplayerScreenTooltip(details.getModpackDetails().stream().map(Text::of).toList());
                    }
                }
            }
        }
    }
}
