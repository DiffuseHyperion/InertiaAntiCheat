package me.diffusehyperion.inertiaanticheat.util;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InertiaAntiCheatConstants {
    public static final Identifier ANTICHEAT_DETAILS_ID = Identifier.of("inertiaanticheat", "anticheat_details");

    public static final Identifier CHECK_CONNECTION = Identifier.of("inertiaanticheat", "check_connection");
    public static final Identifier INITIATE_E2EE = Identifier.of("inertiaanticheat", "initiate_e2ee");
    public static final Identifier SET_ADAPTOR = Identifier.of("inertiaanticheat", "set_adapter");
    public static final Identifier SEND_MOD = Identifier.of("inertiaanticheat", "send_mod");

    public static final Logger MODLOGGER = LoggerFactory.getLogger("InertiaAntiCheat");
    public static final String MODID = "inertiaanticheat";

    public static final long CURRENT_SERVER_CONFIG_VERSION = 8;
    public static final long CURRENT_CLIENT_CONFIG_VERSION = 2;
}
