package com.simibubi.create.content.contraptions.relays.belt;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.KineticTileInstance;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.render.InstancedTileRenderer;
import com.simibubi.create.foundation.render.instancing.*;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.MatrixStacker;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BeltInstance extends KineticTileInstance<BeltTileEntity> {
    public static void register(TileEntityType<? extends BeltTileEntity> type) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () ->
                InstancedTileRenderRegistry.instance.register(type, BeltInstance::new));
    }

    private boolean upward;
    private boolean diagonal;
    private boolean sideways;
    private boolean vertical;
    private boolean alongX;
    private boolean alongZ;
    private BeltSlope beltSlope;
    private Direction facing;
    protected ArrayList<LazyOptional<InstanceKey<BeltData>>> keys;
    protected LazyOptional<InstanceKey<RotatingData>> pulleyKey;

    public BeltInstance(InstancedTileRenderer modelManager, BeltTileEntity tile) {
        super(modelManager, tile);
    }

    @Override
    protected void init() {
        BlockState blockState = tile.getBlockState();
        if (!AllBlocks.BELT.has(blockState))
            return;

        keys = new ArrayList<>(2);

        beltSlope = blockState.get(BeltBlock.SLOPE);
        facing = blockState.get(BeltBlock.HORIZONTAL_FACING);
        upward = beltSlope == BeltSlope.UPWARD;
        diagonal = beltSlope.isDiagonal();
        sideways = beltSlope == BeltSlope.SIDEWAYS;
        vertical = beltSlope == BeltSlope.VERTICAL;
        alongX = facing.getAxis() == Direction.Axis.X;
        alongZ = facing.getAxis() == Direction.Axis.Z;

        BeltPart part = blockState.get(BeltBlock.PART);
        boolean start = part == BeltPart.START;
        boolean end = part == BeltPart.END;

        for (boolean bottom : Iterate.trueAndFalse) {
            AllBlockPartials beltPartial = BeltRenderer.getBeltPartial(diagonal, start, end, bottom);
            SpriteShiftEntry spriteShift = BeltRenderer.getSpriteShiftEntry(diagonal, bottom);

            keys.add(LazyOptional.of(() -> beltPartial.renderOnBelt(modelManager, blockState).setupInstance(setupFunc(spriteShift))));

            if (diagonal) break;
        }

        if (tile.hasPulley()) {
            pulleyKey = LazyOptional.of(() -> getPulleyModel(blockState).setupInstance(setupFunc(tile.getSpeed(), getRotationAxis())));
        } else {
            pulleyKey = LazyOptional.empty();
        }
    }

    @Override
    public void onUpdate() {
        for (LazyOptional<InstanceKey<BeltData>> lazykey : keys) {
            lazykey.ifPresent(key -> key.modifyInstance(data -> data.setRotationalSpeed(getScrollSpeed())));
        }

        pulleyKey.ifPresent(key -> updateRotation(key, getRotationAxis()));
    }

    @Override
    public void updateLight() {
        for (LazyOptional<InstanceKey<BeltData>> lazykey : keys) {
            lazykey.ifPresent(key -> key.modifyInstance(this::relight));
        }

        pulleyKey.ifPresent(key -> key.modifyInstance(this::relight));
    }

    @Override
    public void remove() {
        keys.forEach(lazykey -> lazykey.ifPresent(InstanceKey::delete));
        keys.forEach(LazyOptional::invalidate);
        keys.clear();
        pulleyKey.ifPresent(InstanceKey::delete);
    }

    private float getScrollSpeed() {
        float speed = tile.getSpeed();
        if (((facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ^ upward) ^
                ((alongX && !diagonal) || (alongZ && diagonal)) ^ (vertical && facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)) {
            speed = -speed;
        }
        if (sideways && (facing == Direction.SOUTH || facing == Direction.WEST))
            speed = -speed;

        return speed;
    }

    private InstancedModel<RotatingData> getPulleyModel(BlockState blockState) {
        Direction dir = getOrientation(blockState);

        Direction.Axis axis = dir.getAxis();

        Supplier<MatrixStack> ms = () -> {
            MatrixStack modelTransform = new MatrixStack();
            MatrixStacker msr = MatrixStacker.of(modelTransform);
            msr.centre();
            if (axis == Direction.Axis.X)
                msr.rotateY(90);
            if (axis == Direction.Axis.Y)
                msr.rotateX(90);
            msr.rotateX(90);
            msr.unCentre();

            return modelTransform;
        };

        return rotatingMaterial().getModel(AllBlockPartials.BELT_PULLEY, blockState, dir, ms);
    }

    private Direction getOrientation(BlockState blockState) {
        Direction dir = blockState.get(BeltBlock.HORIZONTAL_FACING)
                                  .rotateY();
        if (beltSlope == BeltSlope.SIDEWAYS)
            dir = Direction.UP;

        return dir;
    }

    private Consumer<BeltData> setupFunc(SpriteShiftEntry spriteShift) {
        return data -> {
            float rotX = (!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90 : 0) + (beltSlope == BeltSlope.DOWNWARD ? 180 : 0);
            float rotY = facing.getHorizontalAngle() + (upward ? 180 : 0) + (sideways ? 90 : 0);
            float rotZ = sideways ? 90 : ((vertical && facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ? 180 : 0);

            BlockPos pos = tile.getPos();
            data.setTileEntity(tile)
                .setBlockLight(tile.getWorld().getLightLevel(LightType.BLOCK, pos))
                .setSkyLight(tile.getWorld().getLightLevel(LightType.SKY, pos))
                .setRotation(rotX, rotY, rotZ)
                .setRotationalSpeed(getScrollSpeed())
                .setRotationOffset(0)
                .setScrollTexture(spriteShift)
                .setScrollMult(diagonal ? 3f / 8f : 0.5f);
        };
    }

}
