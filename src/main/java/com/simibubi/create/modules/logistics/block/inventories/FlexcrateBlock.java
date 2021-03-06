package com.simibubi.create.modules.logistics.block.inventories;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class FlexcrateBlock extends CrateBlock {

	public FlexcrateBlock() {
		super(Properties.from(Blocks.ANDESITE));
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new FlexcrateTileEntity();
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (oldState.getBlock() != state.getBlock() && state.hasTileEntity() && state.get(DOUBLE)
				&& state.get(FACING).getAxisDirection() == AxisDirection.POSITIVE) {
			TileEntity tileEntity = worldIn.getTileEntity(pos);
			if (!(tileEntity instanceof FlexcrateTileEntity))
				return;

			FlexcrateTileEntity te = (FlexcrateTileEntity) tileEntity;
			FlexcrateTileEntity other = te.getOtherCrate();
			if (other == null)
				return;

			for (int slot = 0; slot < other.inventory.getSlots(); slot++) {
				te.inventory.setStackInSlot(slot, other.inventory.getStackInSlot(slot));
				other.inventory.setStackInSlot(slot, ItemStack.EMPTY);
			}
			te.allowedAmount = other.allowedAmount;
			other.invHandler.invalidate();
		}
	}

	@Override
	public ActionResultType onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
			BlockRayTraceResult hit) {

		if (worldIn.isRemote) {
			return ActionResultType.SUCCESS;
		} else {
			TileEntity te = worldIn.getTileEntity(pos);
			if (te instanceof FlexcrateTileEntity) {
				FlexcrateTileEntity fte = (FlexcrateTileEntity) te;
				fte = fte.getMainCrate();
				NetworkHooks.openGui((ServerPlayerEntity) player, fte, fte::sendToContainer);
			}
			return ActionResultType.SUCCESS;
		}
	}

	public static void splitCrate(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (!AllBlocks.FLEXCRATE.typeOf(state))
			return;
		if (!state.get(DOUBLE))
			return;
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof FlexcrateTileEntity))
			return;
		FlexcrateTileEntity crateTe = (FlexcrateTileEntity) te;
		crateTe.onSplit();
		world.setBlockState(pos, state.with(DOUBLE, false));
		world.setBlockState(crateTe.getOtherCrate().getPos(), state.with(DOUBLE, false));
	}

	@Override
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!(worldIn.getTileEntity(pos) instanceof FlexcrateTileEntity))
			return;

		if (state.hasTileEntity() && state.getBlock() != newState.getBlock()) {
			FlexcrateTileEntity te = (FlexcrateTileEntity) worldIn.getTileEntity(pos);
			if (!isMoving)
				te.onDestroyed();
			worldIn.removeTileEntity(pos);
		}

	}

	@Override
	public boolean hasComparatorInputOverride(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(BlockState blockState, World worldIn, BlockPos pos) {
		TileEntity te = worldIn.getTileEntity(pos);
		if (te instanceof FlexcrateTileEntity) {
			FlexcrateTileEntity flexcrateTileEntity = (FlexcrateTileEntity) te;
			return ItemHelper.calcRedstoneFromInventory(flexcrateTileEntity.inventory);
		}
		return 0;
	}

}
