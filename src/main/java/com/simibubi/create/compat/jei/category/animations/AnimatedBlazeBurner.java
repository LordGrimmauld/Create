package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.render.backend.core.PartialModel;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.util.math.vector.Vector3f;

public class AnimatedBlazeBurner implements IDrawable {

	private HeatLevel heatLevel;

	public AnimatedBlazeBurner withHeat(HeatLevel heatLevel) {
		this.heatLevel = heatLevel;
		return this;
	}

	public void draw(MatrixStack matrixStack, int xOffset, int yOffset) {
		matrixStack.pushPose();
		matrixStack.translate(xOffset, yOffset, 200);
		matrixStack.mulPose(Vector3f.XP.rotationDegrees(-15.5f));
		matrixStack.mulPose(Vector3f.YP.rotationDegrees(22.5f));
		int scale = 23;

		GuiGameElement.of(AllBlocks.BLAZE_BURNER.getDefaultState())
			.atLocal(0, 1.65, 0)
			.scale(scale)
			.render(matrixStack);

		PartialModel blaze = AllBlockPartials.BLAZES.get(heatLevel);
		GuiGameElement.of(blaze)
			.atLocal(1, 1.65, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.render(matrixStack);

		matrixStack.popPose();
	}

	@Override
	public int getWidth() {
		return 50;
	}

	@Override
	public int getHeight() {
		return 50;
	}
}
