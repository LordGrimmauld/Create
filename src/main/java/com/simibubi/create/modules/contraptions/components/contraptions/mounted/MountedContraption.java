package com.simibubi.create.modules.contraptions.components.contraptions.mounted;

import static com.simibubi.create.modules.contraptions.components.contraptions.mounted.CartAssemblerBlock.RAIL_SHAPE;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocksNew;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.modules.contraptions.components.contraptions.AllContraptionTypes;
import com.simibubi.create.modules.contraptions.components.contraptions.BlockMovementTraits;
import com.simibubi.create.modules.contraptions.components.contraptions.Contraption;
import com.simibubi.create.modules.contraptions.components.contraptions.mounted.CartAssemblerTileEntity.CartMovementMode;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.RailShape;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;

public class MountedContraption extends Contraption {

	public CartMovementMode rotationMode;

	@Override
	protected AllContraptionTypes getType() {
		return AllContraptionTypes.MOUNTED;
	}

	public static MountedContraption assembleMinecart(World world, BlockPos pos) {
		if (isFrozen())
			return null;

		BlockState state = world.getBlockState(pos);
		if (!state.has(RAIL_SHAPE))
			return null;

		MountedContraption contraption = new MountedContraption();
		if (!contraption.searchMovedStructure(world, pos, null))
			return null;

		Axis axis = state.get(RAIL_SHAPE) == RailShape.EAST_WEST ? Axis.X : Axis.Z;
		contraption.add(pos, Pair.of(new BlockInfo(pos,
			AllBlocksNew.MINECART_ANCHOR.getDefaultState().with(BlockStateProperties.HORIZONTAL_AXIS, axis),
				null), null));
		contraption.removeBlocksFromWorld(world, BlockPos.ZERO);
		contraption.initActors(world);
		contraption.expandBoundsAroundAxis(Axis.Y);

		return contraption;
	}

	@Override
	protected boolean addToInitialFrontier(World world, BlockPos pos, Direction direction, List<BlockPos> frontier) {
		frontier.clear();
		frontier.add(pos.up());
		BlockState state = world.getBlockState(pos);
		if (!AllBlocksNew.CART_ASSEMBLER.has(state))
			return false;
		Axis axis = state.get(CartAssemblerBlock.RAIL_SHAPE) == RailShape.EAST_WEST ? Axis.Z : Axis.X;
		for (AxisDirection axisDirection : AxisDirection.values()) {
			Direction facingFromAxis = Direction.getFacingFromAxis(axisDirection, axis);
			BlockPos offset = pos.offset(facingFromAxis);
			BlockState blockState = world.getBlockState(offset);
			if (!BlockMovementTraits.isBrittle(blockState)
					|| BlockMovementTraits.isBlockAttachedTowards(blockState, facingFromAxis.getOpposite()))
				frontier.add(offset);
		}
		return true;
	}

	@Override
	protected Pair<BlockInfo, TileEntity> capture(World world, BlockPos pos) {
		Pair<BlockInfo, TileEntity> pair = super.capture(world, pos);
		BlockInfo capture = pair.getKey();
		if (AllBlocksNew.CART_ASSEMBLER.has(capture.state))
			return Pair.of(new BlockInfo(capture.pos, CartAssemblerBlock.createAnchor(capture.state), null),
					pair.getValue());
		return pair;
	}

	@Override
	public CompoundNBT writeNBT() {
		CompoundNBT writeNBT = super.writeNBT();
		writeNBT.putString("RotationMode", NBTHelper.writeEnum(rotationMode));
		return writeNBT;
	}

	@Override
	public void readNBT(World world, CompoundNBT nbt) {
		rotationMode = NBTHelper.readEnum(nbt.getString("RotationMode"), CartMovementMode.class);
		super.readNBT(world, nbt);
	}

	@Override
	public void removeBlocksFromWorld(IWorld world, BlockPos offset) {
		super.removeBlocksFromWorld(world, offset, (pos, state) -> pos.equals(anchor));
	}

	@Override
	public void addBlocksToWorld(World world, BlockPos offset, Vec3d rotation) {
		super.addBlocksToWorld(world, offset, rotation, (pos, state) -> AllBlocksNew.MINECART_ANCHOR.has(state));
	}

}
