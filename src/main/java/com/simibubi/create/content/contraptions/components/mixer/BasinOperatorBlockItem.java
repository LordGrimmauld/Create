package com.simibubi.create.content.contraptions.components.mixer;

import com.simibubi.create.AllBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import net.minecraft.item.Item.Properties;

public class BasinOperatorBlockItem extends BlockItem {

	public BasinOperatorBlockItem(Block block, Properties builder) {
		super(block, builder);
	}

	@Override
	public ActionResultType place(BlockItemUseContext context) {
		BlockPos placedOnPos = context.getClickedPos()
			.relative(context.getClickedFace()
				.getOpposite());
		BlockState placedOnState = context.getLevel()
			.getBlockState(placedOnPos);
		if (AllBlocks.BASIN.has(placedOnState) || AllBlocks.BELT.has(placedOnState)
			|| AllBlocks.DEPOT.has(placedOnState) || AllBlocks.WEIGHTED_EJECTOR.has(placedOnState)) {
			if (context.getLevel()
				.getBlockState(placedOnPos.above(2))
				.getMaterial()
				.isReplaceable())
				context = BlockItemUseContext.at(context, placedOnPos.above(2), Direction.UP);
			else
				return ActionResultType.FAIL;
		}

		return super.place(context);
	}

}