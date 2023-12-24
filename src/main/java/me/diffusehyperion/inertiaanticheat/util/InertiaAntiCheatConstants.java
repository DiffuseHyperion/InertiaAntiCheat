package me.diffusehyperion.inertiaanticheat.util;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InertiaAntiCheatConstants {
    public static final Identifier KEY_COMMUNICATION_ID = new Identifier("inertiaanticheat", "key");

    public static final Logger MODLOGGER = LoggerFactory.getLogger("InertiaAntiCheat");
    public static final String MODID = "inertiaanticheat";

    public static final long CURRENT_SERVER_CONFIG_VERSION = 5;
    public static final long CURRENT_CLIENT_CONFIG_VERSION = 2;
}
