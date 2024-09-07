package me.diffusehyperion.inertiaanticheat.server;

import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.interfaces.ServerLoginNetworkHandlerInterface;
import me.diffusehyperion.inertiaanticheat.server.adaptors.DataValidatorAdaptor;
import me.diffusehyperion.inertiaanticheat.util.HashAlgorithm;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.util.ModlistCheckMethod;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerLoginModlistTransferHandler {
    public static void init() {
        ServerLoginConnectionEvents.QUERY_START.register(ServerLoginModlistTransferHandler::requestModTransfer);
    }

    private static void requestModTransfer(ServerLoginNetworkHandler handler, MinecraftServer minecraftServer, LoginPacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
        ServerLoginNetworkHandlerInterface upgradedHandler = (ServerLoginNetworkHandlerInterface) handler;

        InertiaAntiCheat.debugLine();
        InertiaAntiCheat.debugInfo("Checking if " + upgradedHandler.inertiaAntiCheat$getGameProfile().getName() + " has bypass permissions");
        boolean allowed = Permissions.check(upgradedHandler.inertiaAntiCheat$getGameProfile(), "inertiaanticheat.bypass").join();
        if (allowed) {
            InertiaAntiCheat.debugInfo(upgradedHandler.inertiaAntiCheat$getGameProfile().getName() + " is allowed to bypass");
            InertiaAntiCheat.debugLine();
            return;
        }
        InertiaAntiCheat.debugInfo("Not allowed to bypass, sending request to address " + upgradedHandler.inertiaAntiCheat$getConnection().getAddress());

        PacketByteBuf response = PacketByteBufs.create();
        KeyPair keyPair = InertiaAntiCheat.createRSAPair();
        response.writeBytes(keyPair.getPublic().getEncoded());

        DataValidatorAdaptor transferHandler = new DataValidatorAdaptor(keyPair, InertiaAntiCheatConstants.MOD_TRANSFER_CONTINUE_ID);
        ServerLoginNetworking.registerReceiver(handler, InertiaAntiCheatConstants.MOD_TRANSFER_START_ID, transferHandler::startModTransfer);
        sender.sendPacket(InertiaAntiCheatConstants.MOD_TRANSFER_START_ID, response);
        synchronizer.waitFor(transferHandler.future);

        InertiaAntiCheat.debugLine();
    }
}
