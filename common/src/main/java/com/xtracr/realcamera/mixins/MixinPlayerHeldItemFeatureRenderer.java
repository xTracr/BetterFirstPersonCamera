package com.xtracr.realcamera.mixins;

import com.xtracr.realcamera.api.VirtualRenderer;
import com.xtracr.realcamera.utils.Flags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeldItemFeatureRenderer.class)
public abstract class MixinPlayerHeldItemFeatureRenderer {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void realCamera$onRenderItemHEAD(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode,
            Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo cInfo) {
        if (!Flags.isRenderingClientPlayer) return;
        if (VirtualRenderer.shouldDisableRender("heldItem")) cInfo.cancel();
    }
}
