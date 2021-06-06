package com.simibubi.create.content.logistics.block.redstone;

import java.util.Random;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.foundation.tileEntity.renderer.SafeTileEntityRenderer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ColorHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.MatrixStacker;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.TexturedGlyph;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.IRenderTypeBuffer.Impl;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.text.Style;

public class NixieTubeRenderer extends SafeTileEntityRenderer<NixieTubeTileEntity> {

	Random r = new Random();

	public NixieTubeRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void renderSafe(NixieTubeTileEntity te, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer,
		int light, int overlay) {
		ms.pushPose();
		BlockState blockState = te.getBlockState();
		MatrixStacker.of(ms)
			.centre()
			.rotateY(AngleHelper.horizontalAngle(blockState.getValue(NixieTubeBlock.FACING)));

		float height = blockState.getValue(NixieTubeBlock.CEILING) ? 2 : 6;
		float scale = 1 / 20f;

		Couple<String> s = te.getVisibleText();

		ms.pushPose();
		ms.translate(-4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		drawTube(ms, buffer, s.getFirst(), height);
		ms.popPose();

		ms.pushPose();
		ms.translate(4 / 16f, 0, 0);
		ms.scale(scale, -scale, scale);
		drawTube(ms, buffer, s.getSecond(), height);
		ms.popPose();

		ms.popPose();
	}

	private void drawTube(MatrixStack ms, IRenderTypeBuffer buffer, String c, float height) {
		FontRenderer fontRenderer = Minecraft.getInstance().font;
		float charWidth = fontRenderer.width(c);
		float shadowOffset = .5f;
		float flicker = r.nextFloat();
		int brightColor = 0xFF982B;
		int darkColor = 0xE03221;
		int flickeringBrightColor = ColorHelper.mixColors(brightColor, darkColor, flicker / 4);

		ms.pushPose();
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		drawChar(ms, buffer, c, flickeringBrightColor);
		ms.pushPose();
		ms.translate(shadowOffset, shadowOffset, -1 / 16f);
		drawChar(ms, buffer, c, darkColor);
		ms.popPose();
		ms.popPose();

		ms.pushPose();
		ms.scale(-1, 1, 1);
		ms.translate((charWidth - shadowOffset) / -2f, -height, 0);
		drawChar(ms, buffer, c, darkColor);
		ms.pushPose();
		ms.translate(-shadowOffset, shadowOffset, -1 / 16f);
		drawChar(ms, buffer, c, 0x99180F);
		ms.popPose();
		ms.popPose();
	}

	private static void drawChar(MatrixStack ms, IRenderTypeBuffer buffer, String c, int color) {
		FontRenderer fontRenderer = Minecraft.getInstance().font;
		fontRenderer.drawInBatch(c, 0, 0, color, false, ms.last()
			.pose(), buffer, false, 0, 15728880);
		if (buffer instanceof Impl) {
			TexturedGlyph texturedglyph = fontRenderer.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
			((Impl) buffer).endBatch(texturedglyph.renderType(false));
		}
	}

	private static float getCharWidth(char p_211125_1_, FontRenderer fontRenderer) {
		return p_211125_1_ == 167 ? 0.0F : fontRenderer.getFontSet(Style.DEFAULT_FONT).getGlyphInfo(p_211125_1_).getAdvance(false);
	}
}
