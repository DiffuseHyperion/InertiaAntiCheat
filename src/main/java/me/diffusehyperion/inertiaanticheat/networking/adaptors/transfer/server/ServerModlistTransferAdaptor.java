package me.diffusehyperion.inertiaanticheat.networking.adaptors.transfer.server;

import me.diffusehyperion.inertiaanticheat.networking.adaptors.validator.ServerModlistValidatorAdaptor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.security.KeyPair;

public abstract class ServerModlistTransferAdaptor {
    protected final KeyPair keyPair;
    protected final Identifier modTransferID;
    protected final ServerModlistValidatorAdaptor validator;

    public ServerModlistTransferAdaptor(KeyPair keyPair, Identifier modTransferID, ServerModlistValidatorAdaptor validator) {
        this.keyPair = keyPair;
        this.modTransferID = modTransferID;
        this.validator = validator;
    }

    public abstract void transferMod(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler,
                                     boolean b, PacketByteBuf packetByteBuf,
                                     ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender);

    public void completeModTransfer() {
        validator.checkModlist();
    }
}
