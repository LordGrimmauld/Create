package com.simibubi.create.content.contraptions.components.structureMovement.bearing;

import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ActorInstance;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionKineticRenderer;
import com.simibubi.create.foundation.render.backend.core.OrientedData;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

public class StabilizedBearingInstance extends ActorInstance {

	final OrientedData topInstance;

	final Direction facing;
	final Vector3f rotationAxis;
	final Quaternion blockOrientation;

	public StabilizedBearingInstance(ContraptionKineticRenderer modelManager, MovementContext context) {
		super(modelManager, context);

		BlockState blockState = context.state;

		facing = blockState.getValue(BlockStateProperties.FACING);
		rotationAxis = Direction.get(Direction.AxisDirection.POSITIVE, facing.getAxis()).step();

		blockOrientation = BearingInstance.getBlockStateOrientation(facing);

		topInstance = modelManager.getOrientedMaterial().getModel(AllBlockPartials.BEARING_TOP, blockState).createInstance();

		topInstance.setPosition(context.localPos)
				.setRotation(blockOrientation)
				.setBlockLight(localBlockLight());
	}

	@Override
	public void beginFrame() {
		float counterRotationAngle = StabilizedBearingMovementBehaviour.getCounterRotationAngle(context, facing, AnimationTickHolder.getPartialTicks());

		Quaternion rotation = rotationAxis.rotationDegrees(counterRotationAngle);

		rotation.mul(blockOrientation);

		topInstance.setRotation(rotation);
	}
}
