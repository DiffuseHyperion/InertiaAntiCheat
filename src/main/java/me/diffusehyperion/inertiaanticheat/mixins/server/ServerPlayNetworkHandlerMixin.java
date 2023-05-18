package me.diffusehyperion.inertiaanticheat.mixins.server;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer.impendingPlayers;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    // injects here taken or inspired from https://github.com/samolego/SimpleAuth/blob/fabric/src/main/java/org/samo_lego/simpleauth/mixin/MixinServerPlayNetworkHandler.java

    @Shadow
    public ServerPlayerEntity player;
    @Inject(
            method="onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket, CallbackInfo ci) {
        if (impendingPlayers.containsKey(player)) {
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            ci.cancel();
        }
    }
    @Inject(
            method = "onPlayerAction(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerAction(PlayerActionC2SPacket playerActionC2SPacket, CallbackInfo ci) {
        if (impendingPlayers.containsKey(player)) {
            ci.cancel();
        }
    }
    @Inject(
            method = "onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onPlayerChat(ChatMessageC2SPacket chatMessageC2SPacket, CallbackInfo ci) {
        if (impendingPlayers.containsKey(player)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onCommandExecution(Lnet/minecraft/network/packet/c2s/play/CommandExecutionC2SPacket;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onCommandExecution(CommandExecutionC2SPacket commandExecutionC2SPacket, CallbackInfo ci) {
        if (impendingPlayers.containsKey(player)) {
            ci.cancel();
        }
    }
}
