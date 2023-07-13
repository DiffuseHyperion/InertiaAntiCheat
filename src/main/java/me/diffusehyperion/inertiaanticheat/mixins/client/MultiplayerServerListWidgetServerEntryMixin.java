package me.diffusehyperion.inertiaanticheat.mixins.client;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static me.diffusehyperion.inertiaanticheat.InertiaAntiCheatConstants.MODID;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class MultiplayerServerListWidgetServerEntryMixin {
    @Shadow @Final private MultiplayerScreen screen;
    @Shadow @Final private MinecraftClient client;
    @Unique
    private boolean inertiaInstalled = false;
    @Unique
    private boolean inertiaE2EE = false;
    @Unique
    private final Identifier ICON_ENABLED = new Identifier(MODID, "textures/gui/enabled.png");
    @Inject(
            method = "render",
            at = @At(value = "HEAD")
    )
    private void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        if (inertiaInstalled) {
            int iconX = x + entryWidth - 15;
            int iconY = y + 10;
            context.drawTexture(ICON_ENABLED, iconX, iconY, 0.0f, 0.0f, 10, 10, 10, 10);
            if (mouseX > iconX && mouseX < iconX + 10 && mouseY > iconY && mouseY < iconY + 10) {
                List<Text> text = new ArrayList<>();
                text.add(Text.of("InertiaAntiCheat installed"));
                screen.setMultiplayerScreenTooltip(text);
            }
        }
    }

    @Inject(
            method = "render",
            at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/network/ServerInfo;online:Z",
            opcode = Opcodes.PUTFIELD)
    )
    private void pingServer(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
            ClientPlayNetworking.send(InertiaAntiCheatConstants.PING_PACKET_ID, PacketByteBufs.empty());
            ClientPlayNetworking.registerGlobalReceiver(InertiaAntiCheatConstants.PONG_PACKET_ID, (client, handler, buf, responseSender) -> {
                inertiaInstalled = true;
                inertiaE2EE = buf.readBoolean();
                // lambda needed here since static method wont be able to access non static fields (inertiaInstalled)
            });
    }



}
