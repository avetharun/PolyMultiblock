package dev.feintha.polymultiblock.mixin;

import net.minecraft.state.StateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(StateManager.class)
public class StateManagerMixin {
    @Mutable
    @Shadow @Final private static Pattern VALID_NAME_PATTERN;

    @Inject(method="<clinit>", at=@At("TAIL"))
    private static void modifyPatternMixin(CallbackInfo ci){
        VALID_NAME_PATTERN = Pattern.compile("^-?[a-z0-9_]+$");
    }
}
