package me.diffusehyperion.inertiaanticheat;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InertiaAntiCheatConstants {
    public static final Identifier REQUEST_PACKET_ID = new Identifier("inertiaanticheat", "request");
    public static final Identifier RESPONSE_PACKET_ID = new Identifier("inertiaanticheat", "response");

    public static final Logger LOGGER = LoggerFactory.getLogger("InertiaAntiCheat");

    public static final int CURRENT_CONFIG_VERSION = 1;
}
