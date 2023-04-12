package com.mrcrayfish.controllable.mixin.client;

import com.mrcrayfish.controllable.Config;
import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.ButtonBindings;
import com.mrcrayfish.controllable.client.Controller;
import com.mrcrayfish.controllable.client.InputProcessor;
import com.mrcrayfish.controllable.client.KeyUseOverride;
import com.mrcrayfish.controllable.platform.ClientServices;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Author: MrCrayfish
 */
@Mixin(Minecraft.class)
public class MinecraftMixin
{
    @Shadow
    public LocalPlayer player;

    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"), index = 0)
    private boolean controllableSendClickBlockToController(boolean original)
    {
        return original || isLeftClicking();
    }

    /*@Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"), slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V")))
    private boolean controllableOnKeyDown(KeyMapping mapping)
    {
        return mapping.isDown() || KeyUseOverride.isRightClicking();
    }*/

    /**
     * Checks if a controller is connected and if the attack button is down. A special except is
     * added when virtual mouse is enabled and it will ignore if the mouse is grabbed or not.
     */
    private static boolean isLeftClicking()
    {
        Minecraft mc = Minecraft.getInstance();
        Controller controller = Controllable.getController();
        if(controller != null && ButtonBindings.ATTACK.isButtonDown())
        {
            boolean usingVirtualMouse = (Config.CLIENT.client.options.virtualCursor.get() && Controllable.getInput().getLastUse() > 0);
            return mc.screen == null && (mc.mouseHandler.isMouseGrabbed() || usingVirtualMouse);
        }
        return false;
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At(value = "HEAD"), cancellable = true)
    private void controllableIsEntityGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        if(this.player != null && this.player.isSpectator() && ButtonBindings.HIGHLIGHT_PLAYERS.isButtonDown() && entity.getType() == EntityType.PLAYER)
        {
            cir.setReturnValue(true);
        }
    }

    // Prevents the game from pausing (when losing focus) when a controller is plugged in.
    @Inject(method = "isWindowActive", at = @At(value = "HEAD"), cancellable = true)
    private void controllableIsWindowActiveHead(CallbackInfoReturnable<Boolean> cir)
    {
        if(Controllable.getController() != null)
        {
            cir.setReturnValue(true);
        }
    }

    // Note: Minecraft Development plugin is failing to process this correctly.
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(method = "runTick", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;getFramerateLimit()I"), index = 8)
    private int controllableModifyFramerate(int originalFps)
    {
        Minecraft mc = (Minecraft) (Object) this;
        if(mc.getOverlay() == null)
        {
            if(Config.CLIENT.client.options.fpsPollingFix.get() && ClientServices.CLIENT.getMinecraftFramerateLimit() < 40)
            {
                return 260; // To bypass "fps < 260" condition
            }
        }
        return originalFps;
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getFramerateLimit()I"))
    private void controllableWaitEvents(boolean outOfMemory, CallbackInfo ci)
    {
        Minecraft mc = (Minecraft) (Object) this;
        if(mc.getOverlay() == null)
        {
            if(Config.CLIENT.client.options.fpsPollingFix.get() && ClientServices.CLIENT.getMinecraftFramerateLimit() < 40)
            {
                InputProcessor.queueInputsWait();
            }
        }
    }
}
