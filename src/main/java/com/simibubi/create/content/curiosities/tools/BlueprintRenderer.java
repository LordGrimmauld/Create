package com.simibubi.create.content.curiosities.tools;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.curiosities.tools.BlueprintEntity.BlueprintSection;
import com.simibubi.create.foundation.render.PartialBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.backend.core.PartialModel;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.MatrixStacker;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;

public class BlueprintRenderer extends EntityRenderer<BlueprintEntity> {

	public BlueprintRenderer(EntityRendererManager p_i46179_1_) {
		super(p_i46179_1_);
	}

	@Override
	public void render(BlueprintEntity entity, float yaw, float pt, MatrixStack ms, IRenderTypeBuffer buffer,
		int overlay) {
		PartialModel partialModel = entity.size == 3 ? AllBlockPartials.CRAFTING_BLUEPRINT_3x3
			: entity.size == 2 ? AllBlockPartials.CRAFTING_BLUEPRINT_2x2 : AllBlockPartials.CRAFTING_BLUEPRINT_1x1;
		SuperByteBuffer sbb = PartialBufferer.get(partialModel, Blocks.AIR.defaultBlockState());
		int light = WorldRenderer.getLightColor(entity.level, entity.blockPosition());
		sbb.matrixStacker()
			.rotateY(-yaw)
			.rotateX(90.0F + entity.xRot)
			.translate(-.5, -1 / 32f, -.5);
		if (entity.size == 2)
			sbb.translate(.5, 0, -.5);
		sbb.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));
		super.render(entity, yaw, pt, ms, buffer, light);

		ms.pushPose();

		MatrixStacker.of(ms)
			.rotateY(-yaw)
			.rotateX(entity.xRot == -90 ? -45 : entity.xRot == 0 ? -15 : -5);
		Matrix3f copy = ms.last()
			.normal()
			.copy();

		ms.popPose();
		ms.pushPose();

		MatrixStacker.of(ms)
			.rotateY(-yaw)
			.rotateX(entity.xRot)
			.translate(0, 0, 1 / 32f + .001);
		
		if (entity.size == 3)
			ms.translate(-1, -1, 0);

		for (int x = 0; x < entity.size; x++) {
			ms.pushPose();
			for (int y = 0; y < entity.size; y++) {
				BlueprintSection section = entity.getSection(x * entity.size + y);
				Couple<ItemStack> displayItems = section.getDisplayItems();
				ms.pushPose();
				ms.scale(.5f, .5f, 1 / 1024f);
				displayItems.forEachWithContext((stack, primary) -> {
					if (stack.isEmpty())
						return;

					ms.pushPose();
					if (!primary) {
						ms.translate(0.325f, -0.325f, 1);
						ms.scale(.625f, .625f, 1);
					}

					Matrix3f n = ms.last()
						.normal();
					n.m00 = copy.m00;
					n.m01 = copy.m01;
					n.m02 = copy.m02;
					n.m10 = copy.m10;
					n.m11 = copy.m11;
					n.m12 = copy.m12;
					n.m20 = copy.m20;
					n.m21 = copy.m21;
					n.m22 = copy.m22;

					Minecraft.getInstance()
						.getItemRenderer()
						.renderStatic(stack, TransformType.GUI, light, overlay, ms, buffer);
					ms.popPose();
				});
				ms.popPose();
				ms.translate(1, 0, 0);
			}
			ms.popPose();
			ms.translate(0, 1, 0);
		}

		ms.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(BlueprintEntity p_110775_1_) {
		return null;
	}

}
