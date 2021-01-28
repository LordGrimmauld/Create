package com.simibubi.create.content.contraptions.base;

import com.simibubi.create.foundation.render.InstancedTileRenderer;
import com.simibubi.create.foundation.render.instancing.InstanceKey;
import com.simibubi.create.foundation.render.instancing.InstancedModel;
import com.simibubi.create.foundation.render.instancing.InstancedTileRenderRegistry;
import com.simibubi.create.foundation.render.instancing.RotatingData;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;

import static com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer.KINETIC_TILE;

public class SingleRotatingInstance extends KineticTileInstance<KineticTileEntity> {
    public static void register(TileEntityType<? extends KineticTileEntity> type) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () ->
                InstancedTileRenderRegistry.instance.register(type, SingleRotatingInstance::new));
    }

    protected LazyOptional<InstanceKey<RotatingData>> rotatingModelKey;

    public SingleRotatingInstance(InstancedTileRenderer modelManager, KineticTileEntity tile) {
        super(modelManager, tile);
    }

    @Override
    protected void init() {
        rotatingModelKey = LazyOptional.of(() -> getModel().setupInstance(setupFunc(tile.getSpeed(), ((IRotate) lastState.getBlock()).getRotationAxis(lastState))));
    }

    @Override
    public void onUpdate() {
        Direction.Axis axis = ((IRotate) lastState.getBlock()).getRotationAxis(lastState);
        rotatingModelKey.ifPresent(key -> updateRotation(key, axis));
    }

    @Override
    public void updateLight() {
        rotatingModelKey.ifPresent(key -> key.modifyInstance(this::relight));
    }

    @Override
    public void remove() {
        rotatingModelKey.ifPresent(InstanceKey::delete);
        rotatingModelKey.invalidate();
    }

    protected BlockState getRenderedBlockState() {
        return lastState;
    }

    protected InstancedModel<RotatingData> getModel() {
        return rotatingMaterial().getModel(KINETIC_TILE, getRenderedBlockState());
    }
}
