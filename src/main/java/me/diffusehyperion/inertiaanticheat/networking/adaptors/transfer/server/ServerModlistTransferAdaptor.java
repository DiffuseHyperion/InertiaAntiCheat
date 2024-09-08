package me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.server.InertiaAntiCheatServer;
import net.fabricmc.fabric.api.networking.v1.LoginPacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

public abstract class ServerModlistTransferAdaptor {
    protected final KeyPair keyPair;
    protected final Identifier modTransferID;

    public ServerModlistTransferAdaptor(KeyPair keyPair, Identifier modTransferID) {
        this.keyPair = keyPair;
        this.modTransferID = modTransferID;
    }

    public abstract void transferMod(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler,
                                     boolean b, PacketByteBuf packetByteBuf,
                                     ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender);

    public void completeModTransfer() {

    }
}
