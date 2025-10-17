package com.diffusehyperion.inertiaanticheat.mixins.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ConnectScreen.class)
public interface ConnectScreenAccessor {
    @Accessor("connection")
    ClientConnection getConnection();

    @Accessor("parent")
    Screen getParent();

    @Invoker("setStatus")
    void invokeSetStatus(Text status);
}
