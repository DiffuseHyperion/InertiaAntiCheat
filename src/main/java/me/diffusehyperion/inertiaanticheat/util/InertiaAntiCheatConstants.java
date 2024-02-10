package me.diffusehyperion.inertiaanticheat.util;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InertiaAntiCheatConstants {
    public static final Identifier MOD_TRANSFER_START_ID = new Identifier("inertiaanticheat", "mod_transfer_start");
    public static final Identifier MOD_TRANSFER_CONTINUE_ID = new Identifier("inertiaanticheat", "mod_transfer_continue");

    public static final Logger MODLOGGER = LoggerFactory.getLogger("InertiaAntiCheat");
    public static final String MODID = "inertiaanticheat";

    public static final long CURRENT_SERVER_CONFIG_VERSION = 6;
    public static final long CURRENT_CLIENT_CONFIG_VERSION = 2;
}
