package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.render.backend.RenderWork;
import com.simibubi.create.foundation.render.backend.light.LightUpdater;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.world.chunk.Chunk;

@Mixin(ClientPlayNetHandler.class)
public class NetworkLightUpdateMixin {

	@Inject(at = @At("TAIL"), method = "handleLightUpdatePacked")
	private void onLightPacket(SUpdateLightPacket packet, CallbackInfo ci) {
		RenderWork.enqueue(() -> {
			ClientWorld world = Minecraft.getInstance().level;

			if (world == null)
				return;

			int chunkX = packet.getX();
			int chunkZ = packet.getZ();

			Chunk chunk = world.getChunkSource()
				.getChunk(chunkX, chunkZ, false);

			if (chunk != null) {
				chunk.getBlockEntities()
					.values()
					.forEach(tile -> {
						CreateClient.KINETIC_RENDERER.get(world)
							.onLightUpdate(tile);
					});
			}

			LightUpdater.getInstance()
				.onLightPacket(world, chunkX, chunkZ);
		});
	}
}
