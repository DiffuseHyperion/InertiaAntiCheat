package me.diffusehyperion.inertiaanticheat.client;

import com.moandjiezana.toml.Toml;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.diffusehyperion.inertiaanticheat.InertiaAntiCheat;
import me.diffusehyperion.inertiaanticheat.util.InertiaAntiCheatConstants;
import me.diffusehyperion.inertiaanticheat.util.Scheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class InertiaAntiCheatClient implements ClientModInitializer {
    public static Toml clientConfig;
    public static final Scheduler clientScheduler = new Scheduler();
    public static final List<byte[]> allModData = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        InertiaAntiCheatClient.clientConfig = InertiaAntiCheat.initializeConfig("/config/client/InertiaAntiCheat.toml", InertiaAntiCheatConstants.CURRENT_CLIENT_CONFIG_VERSION);

        this.setupModDataList();
        ClientLoginModlistTransferHandler.init();
    }



    public void setupModDataList() {
        try {
            File modDirectory = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
            for (File modFile : Objects.requireNonNull(modDirectory.listFiles())) {
                InertiaAntiCheatClient.allModData.add(Files.readAllBytes(modFile.toPath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
