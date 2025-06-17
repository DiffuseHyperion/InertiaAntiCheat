package com.diffusehyperion.inertiaanticheat.mixins.client;

import com.diffusehyperion.inertiaanticheat.interfaces.UpgradedServerInfo;
import com.diffusehyperion.inertiaanticheat.util.AnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.GroupAnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.IndividualAnticheatDetails;
import com.diffusehyperion.inertiaanticheat.util.ValidationMethod;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
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

import java.util.Objects;

import static com.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants.MODID;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class MultiplayerServerListWidgetServerEntryMixin {
    @Shadow @Final private ServerInfo server;

    @Unique
    private static final Identifier ICON_ENABLED = Identifier.of(MODID, "textures/gui/enabled.png");
    @Unique
    private static final Identifier ICON_WHITELIST = Identifier.of(MODID, "textures/gui/whitelist.png");
    @Unique
    private static final Identifier ICON_BLACKLIST = Identifier.of(MODID, "textures/gui/blacklist.png");
    @Unique
    private static final Identifier ICON_MODPACK = Identifier.of(MODID, "textures/gui/modpack.png");

    @Inject(
            method = "render",
            at = @At(value = "TAIL")
    )
    private void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        UpgradedServerInfo upgradedServerInfo = ((UpgradedServerInfo) server);
        Boolean installed = upgradedServerInfo.inertiaAntiCheat$isInertiaInstalled();
        AnticheatDetails anticheatDetails = upgradedServerInfo.inertiaAntiCheat$getAnticheatDetails();
        if (Objects.nonNull(installed) && installed.equals(true) && anticheatDetails.showInstalled()) {
            int iconX = x + entryWidth - 15;
            int iconY = y + 10;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_ENABLED, iconX, iconY, 0.0f, 0.0f, 10, 10, 10, 10);
            if (mouseX > iconX && mouseX < iconX + 10 && mouseY > iconY && mouseY < iconY + 10) {
                context.drawTooltip(Text.of("InertiaAntiCheat installed"), mouseX, mouseY);
            }
        }
        if (Objects.nonNull(anticheatDetails)) {
            if (anticheatDetails.getValidationMethod() == ValidationMethod.INDIVIDUAL) {
                IndividualAnticheatDetails details = (IndividualAnticheatDetails) anticheatDetails;

                if ((details.getWhitelistedMods().size() == 1 && !Objects.equals(details.getWhitelistedMods().getFirst(), "")) || details.getWhitelistedMods().size() >= 2) {
                    int whitelistIconX = x + entryWidth - 25;
                    int whitelistIconY = y + 20;
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_WHITELIST, whitelistIconX, whitelistIconY, 0.0f, 0.0f, 10, 10, 10, 10);
                    if (mouseX > whitelistIconX && mouseX < whitelistIconX + 10 && mouseY > whitelistIconY && mouseY < whitelistIconY + 10) {
                        context.drawTooltip(details.getWhitelistedMods().stream().map(Text::of).map(Text::asOrderedText).toList(), mouseX, mouseY);
                    }
                }

                if ((details.getBlacklistedMods().size() == 1 && !Objects.equals(details.getBlacklistedMods().getFirst(), "")) || details.getBlacklistedMods().size() >= 2) {
                    int blacklistIconX = x + entryWidth - 15;
                    int blacklistIconY = y + 20;
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_BLACKLIST, blacklistIconX, blacklistIconY, 0.0f, 0.0f, 10, 10, 10, 10);
                    if (mouseX > blacklistIconX && mouseX < blacklistIconX + 10 && mouseY > blacklistIconY && mouseY < blacklistIconY + 10) {
                        context.drawTooltip(details.getBlacklistedMods().stream().map(Text::of).map(Text::asOrderedText).toList(), mouseX, mouseY);
                    }
                }
            } else {
                GroupAnticheatDetails details = (GroupAnticheatDetails) anticheatDetails;
                if ((details.getModpackDetails().size() == 1 && !Objects.equals(details.getModpackDetails().getFirst(), "")) || details.getModpackDetails().size() >= 2) {
                    int blacklistIconX = x + entryWidth - 15;
                    int blacklistIconY = y + 20;
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON_MODPACK, blacklistIconX, blacklistIconY, 0.0f, 0.0f, 10, 10, 10, 10);
                    if (mouseX > blacklistIconX && mouseX < blacklistIconX + 10 && mouseY > blacklistIconY && mouseY < blacklistIconY + 10) {
                        context.drawTooltip(details.getModpackDetails().stream().map(Text::of).map(Text::asOrderedText).toList(), mouseX, mouseY);
                    }
                }
            }
        }
    }
}
