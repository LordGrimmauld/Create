package com.simibubi.create.content.contraptions.relays.gearbox;

import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.KineticTileInstance;
import com.simibubi.create.foundation.render.InstancedTileRenderer;
import com.simibubi.create.foundation.render.instancing.InstanceKey;
import com.simibubi.create.foundation.render.instancing.InstancedModel;
import com.simibubi.create.foundation.render.instancing.InstancedTileRenderRegistry;
import com.simibubi.create.foundation.render.instancing.RotatingData;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;

import java.util.EnumMap;
import java.util.Map;

public class GearboxInstance extends KineticTileInstance<GearboxTileEntity> {
    public static void register(TileEntityType<? extends GearboxTileEntity> type) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () ->
                InstancedTileRenderRegistry.instance.register(type, GearboxInstance::new));
    }

    protected EnumMap<Direction, LazyOptional<InstanceKey<RotatingData>>> keys;
    protected Direction sourceFacing;

    public GearboxInstance(InstancedTileRenderer modelManager, GearboxTileEntity tile) {
        super(modelManager, tile);
    }

    @Override
    protected void init() {
        keys = new EnumMap<>(Direction.class);

        BlockState state = tile.getBlockState();

        final Direction.Axis boxAxis = state.get(BlockStateProperties.AXIS);

        BlockPos pos = tile.getPos();
        int blockLight = tile.getWorld().getLightLevel(LightType.BLOCK, pos);
        int skyLight = tile.getWorld().getLightLevel(LightType.SKY, pos);
        updateSourceFacing();

        for (Direction direction : Iterate.directions) {
            final Direction.Axis axis = direction.getAxis();
            if (boxAxis == axis)
                continue;

            InstancedModel<RotatingData> shaft = AllBlockPartials.SHAFT_HALF.renderOnDirectionalSouthRotating(modelManager, state, direction);

            LazyOptional<InstanceKey<RotatingData>> key = LazyOptional.of(() -> shaft.setupInstance(data -> data.setBlockLight(blockLight)
                .setSkyLight(skyLight)
                .setRotationalSpeed(getSpeed(direction))
                .setRotationOffset(getRotationOffset(axis))
                .setRotationAxis(Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, axis).getUnitVector())
                .setTileEntity(tile)));
            keys.put(direction, key);
        }
    }

    private float getSpeed(Direction direction) {
        float speed = tile.getSpeed();

        if (speed != 0 && sourceFacing != null) {
            if (sourceFacing.getAxis() == direction.getAxis())
                speed *= sourceFacing == direction ? 1 : -1;
            else if (sourceFacing.getAxisDirection() == direction.getAxisDirection())
                speed *= -1;
        }
        return speed;
    }

    protected void updateSourceFacing() {
        if (tile.hasSource()) {
            BlockPos source = tile.source.subtract(tile.getPos());
            sourceFacing = Direction.getFacingFromVector(source.getX(), source.getY(), source.getZ());
        } else {
            sourceFacing = null;
        }
    }

    @Override
    public void onUpdate() {
        updateSourceFacing();
        BlockPos pos = tile.getPos();
        for (Map.Entry<Direction, LazyOptional<InstanceKey<RotatingData>>> lazykey : keys.entrySet()) {
            lazykey.getValue().ifPresent(key -> key.modifyInstance(data -> {
                Direction direction = lazykey.getKey();
                Direction.Axis axis = direction.getAxis();

                data.setRotationalSpeed(getSpeed(direction))
                    .setRotationOffset(getRotationOffset(axis))
                    .setRotationAxis(Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, axis).getUnitVector());
            }));
        }
    }

    @Override
    public void updateLight() {
        BlockPos pos = tile.getPos();
        int blockLight = tile.getWorld().getLightLevel(LightType.BLOCK, pos);
        int skyLight = tile.getWorld().getLightLevel(LightType.SKY, pos);

        for (LazyOptional<InstanceKey<RotatingData>> lazykey : keys.values()) {
            lazykey.ifPresent(key -> key.modifyInstance(data -> data.setBlockLight(blockLight).setSkyLight(skyLight)));
        }
    }

    @Override
    public void remove() {
        keys.values().forEach(lazykey -> lazykey.ifPresent(InstanceKey::delete));
        keys.values().forEach(LazyOptional::invalidate);
        keys.clear();
    }
}
