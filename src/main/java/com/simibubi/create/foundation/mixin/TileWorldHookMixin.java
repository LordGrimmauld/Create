package com.simibubi.create.foundation.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.render.KineticRenderer;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(value = World.class, priority = 1100) // this and create.mixins.json have high priority to load after Performant
public class TileWorldHookMixin {

	final World self = (World) (Object) this;

	@Shadow
	@Final
	public boolean isClientSide;

	@Shadow
	@Final
	protected Set<TileEntity> blockEntitiesToUnload;

	@Inject(at = @At("TAIL"), method = "addBlockEntity")
	private void onAddTile(TileEntity te, CallbackInfoReturnable<Boolean> cir) {
		if (isClientSide) {
			CreateClient.KINETIC_RENDERER.get(self)
				.queueAdd(te);
		}
	}

	/**
	 * Without this we don't unload instances when a chunk unloads.
	 */
	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V", ordinal = 0), method = "tickBlockEntities")
	private void onChunkUnload(CallbackInfo ci) {
		if (isClientSide) {
			KineticRenderer kineticRenderer = CreateClient.KINETIC_RENDERER.get(self);
			for (TileEntity tile : blockEntitiesToUnload) {
				kineticRenderer.remove(tile);
			}
		}
	}
}
