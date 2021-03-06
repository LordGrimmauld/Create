package com.simibubi.create.modules.contraptions.relays.advanced;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.behaviour.base.SmartTileEntityRenderer;
import com.simibubi.create.foundation.utility.SuperByteBuffer;
import com.simibubi.create.modules.contraptions.base.KineticTileEntityRenderer;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

public class SpeedControllerRenderer extends SmartTileEntityRenderer<SpeedControllerTileEntity> {

	public SpeedControllerRenderer(TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	protected void renderSafe(SpeedControllerTileEntity tileEntityIn, float partialTicks, MatrixStack ms,
			IRenderTypeBuffer buffer, int light, int overlay) {
		super.renderSafe(tileEntityIn, partialTicks, ms, buffer, light, overlay);

		KineticTileEntityRenderer.renderRotatingBuffer(tileEntityIn, getRotatedModel(tileEntityIn), ms,
				buffer.getBuffer(RenderType.getSolid()));
	}

	private SuperByteBuffer getRotatedModel(SpeedControllerTileEntity te) {
		return CreateClient.bufferCache.renderBlockIn(KineticTileEntityRenderer.KINETIC_TILE,
				KineticTileEntityRenderer.shaft(KineticTileEntityRenderer.getRotationAxisOf(te)));
	}

}
