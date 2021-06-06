package com.simibubi.create.foundation.mixin;

import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.render.KineticRenderer;
import com.simibubi.create.foundation.render.backend.Backend;
import com.simibubi.create.foundation.render.backend.FastRenderDispatcher;
import com.simibubi.create.foundation.render.backend.OptifineHandler;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(WorldRenderer.class)
public class RenderHooksMixin {

	@Shadow
	private ClientWorld level;

	/**
	 * JUSTIFICATION: This method is called once per layer per frame. It allows us to perform
	 * layer-correct custom rendering. RenderWorldLast is not refined enough for rendering world objects.
	 * This should probably be a forge event.
	 */
	@Inject(at = @At("TAIL"), method = "renderChunkLayer")
	private void renderLayer(RenderType type, MatrixStack stack, double camX, double camY, double camZ,
		CallbackInfo ci) {
		if (!Backend.available())
			return;

		Matrix4f viewProjection = stack.last()
			.pose()
			.copy();
		viewProjection.multiplyBackward(Backend.projectionMatrix);

		FastRenderDispatcher.renderLayer(type, viewProjection, camX, camY, camZ);

		ContraptionRenderDispatcher.renderLayer(type, viewProjection, camX, camY, camZ);

		GL20.glUseProgram(0);
	}

	@Inject(at = @At(value = "INVOKE", target = "net.minecraft.client.renderer.WorldRenderer.compileChunksUntil(J)V"), method = "renderLevel")
	private void setupFrame(MatrixStack p_228426_1_, float p_228426_2_, long p_228426_3_, boolean p_228426_5_,
		ActiveRenderInfo info, GameRenderer p_228426_7_, LightTexture p_228426_8_, Matrix4f p_228426_9_,
		CallbackInfo ci) {
		Vector3d cameraPos = info.getPosition();
		double camX = cameraPos.x();
		double camY = cameraPos.y();
		double camZ = cameraPos.z();

		CreateClient.KINETIC_RENDERER.get(level)
			.beginFrame(info, camX, camY, camZ);
		ContraptionRenderDispatcher.beginFrame(info, camX, camY, camZ);
	}

	@Inject(at = @At("TAIL"), method = "Lnet/minecraft/client/renderer/WorldRenderer;setBlockDirty(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V")
	private void checkUpdate(BlockPos pos, BlockState lastState, BlockState newState, CallbackInfo ci) {
		CreateClient.KINETIC_RENDERER.get(level)
			.update(level.getBlockEntity(pos));
	}

	@Inject(at = @At("TAIL"), method = "allChanged")
	private void refresh(CallbackInfo ci) {
		ContraptionRenderDispatcher.invalidateAll();
		OptifineHandler.refresh();
		Backend.refresh();

		if (Backend.canUseInstancing() && level != null) {
			KineticRenderer kineticRenderer = CreateClient.KINETIC_RENDERER.get(level);
			kineticRenderer.invalidate();
			level.blockEntityList.forEach(kineticRenderer::add);
		}
	}
}
