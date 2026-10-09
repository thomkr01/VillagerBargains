package com.villagerbargains.mixin;

import com.villagerbargains.command.BargainCommands;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds {@code /bargain} to every new command dispatcher (server start and {@code /reload}),
 * at the end of the {@code Commands} constructor where vanilla has registered its own commands.
 * This replaces Fabric API's command callback, so the mod still runs without Fabric API.
 */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void villagerbargains$registerBargainCommand(CallbackInfo ci) {
        BargainCommands.register(((Commands) (Object) this).getDispatcher());
    }
}
