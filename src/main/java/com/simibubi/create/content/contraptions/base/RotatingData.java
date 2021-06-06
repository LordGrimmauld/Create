package com.simibubi.create.content.contraptions.base;

import java.nio.ByteBuffer;

import com.simibubi.create.foundation.render.backend.instancing.InstancedModel;

import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class RotatingData extends KineticData {
    private byte rotationAxisX;
    private byte rotationAxisY;
    private byte rotationAxisZ;

    protected RotatingData(InstancedModel<?> owner) {
        super(owner);
    }

    public RotatingData setRotationAxis(Direction.Axis axis) {
        Direction orientation = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        setRotationAxis(orientation.step());
        return this;
    }

    public RotatingData setRotationAxis(Vector3f axis) {
        setRotationAxis(axis.x(), axis.y(), axis.z());
        return this;
    }

    public RotatingData setRotationAxis(float rotationAxisX, float rotationAxisY, float rotationAxisZ) {
        this.rotationAxisX = (byte) (rotationAxisX * 127);
        this.rotationAxisY = (byte) (rotationAxisY * 127);
        this.rotationAxisZ = (byte) (rotationAxisZ * 127);
        markDirty();
        return this;
    }

    @Override
    public void write(ByteBuffer buf) {
        super.write(buf);

        putVec3(buf, rotationAxisX, rotationAxisY, rotationAxisZ);
    }
}
