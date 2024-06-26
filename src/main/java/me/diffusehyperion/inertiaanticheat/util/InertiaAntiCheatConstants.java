package me.diffusehyperion.inertiaanticheat.util;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InertiaAntiCheatConstants {
    public static final Identifier ANTICHEAT_DETAILS_ID = Identifier.of("inertiaanticheat", "anticheat_details");

    public static final Identifier MOD_TRANSFER_START_ID = Identifier.of("inertiaanticheat", "mod_transfer_start");
    public static final Identifier MOD_TRANSFER_CONTINUE_ID = Identifier.of("inertiaanticheat", "mod_transfer_continue");

    public static final Logger MODLOGGER = LoggerFactory.getLogger("InertiaAntiCheat");
    public static final String MODID = "inertiaanticheat";

    public static final long CURRENT_SERVER_CONFIG_VERSION = 7;
    public static final long CURRENT_CLIENT_CONFIG_VERSION = 2;
}
