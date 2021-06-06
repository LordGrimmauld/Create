package com.simibubi.create.content.contraptions.components.structureMovement.render;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionLighter;
import com.simibubi.create.foundation.render.backend.Backend;
import com.simibubi.create.foundation.render.backend.instancing.IInstanceRendered;
import com.simibubi.create.foundation.render.backend.light.GridAlignedBB;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.worldWrappers.PlacementSimulationWorld;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;

public class RenderedContraption {
    private final HashMap<RenderType, ContraptionModel> renderLayers = new HashMap<>();

    public final PlacementSimulationWorld renderWorld;

    private final ContraptionLighter<?> lighter;

    public final ContraptionKineticRenderer kinetics;

    public Contraption contraption;

    private Matrix4f model;
    private AxisAlignedBB lightBox;

    public RenderedContraption(World world, Contraption contraption) {
        this.contraption = contraption;
        this.lighter = contraption.makeLighter();
        this.kinetics = new ContraptionKineticRenderer(this);
        this.renderWorld = setupRenderWorld(world, contraption);

        buildLayers();
        if (Backend.canUseInstancing()) {
            buildInstancedTiles();
            buildActors();
        }
    }

    public int getEntityId() {
        return contraption.entity.getId();
    }

    public boolean isDead() {
        return !contraption.entity.isAlive();
    }

    public ContraptionLighter<?> getLighter() {
        return lighter;
    }

    public void doRenderLayer(RenderType layer, ContraptionProgram shader) {
        ContraptionModel structure = renderLayers.get(layer);
        if (structure != null) {
            setup(shader);
            structure.render();
            teardown();
        }
    }

    public void beginFrame(ActiveRenderInfo info, double camX, double camY, double camZ) {
        kinetics.beginFrame(info, camX, camY, camZ);

        AbstractContraptionEntity entity = contraption.entity;
        float pt = AnimationTickHolder.getPartialTicks();

        MatrixStack stack = new MatrixStack();

        double x = MathHelper.lerp(pt, entity.xOld, entity.getX()) - camX;
        double y = MathHelper.lerp(pt, entity.yOld, entity.getY()) - camY;
        double z = MathHelper.lerp(pt, entity.zOld, entity.getZ()) - camZ;
        stack.translate(x, y, z);

        entity.doLocalTransforms(pt, new MatrixStack[]{ stack });

        model = stack.last().pose();

        AxisAlignedBB lightBox = GridAlignedBB.toAABB(lighter.lightVolume.getTextureVolume());

        this.lightBox = lightBox.move(-camX, -camY, -camZ);
    }

    void setup(ContraptionProgram shader) {
        if (model == null || lightBox == null) return;
        shader.bind(model, lightBox);
        lighter.lightVolume.bind();
    }

    void teardown() {
        lighter.lightVolume.unbind();
    }

    void invalidate() {
        for (ContraptionModel buffer : renderLayers.values()) {
            buffer.delete();
        }
        renderLayers.clear();

        lighter.lightVolume.delete();

        kinetics.invalidate();
    }

    private void buildLayers() {
        for (ContraptionModel buffer : renderLayers.values()) {
            buffer.delete();
        }

        renderLayers.clear();

        List<RenderType> blockLayers = RenderType.chunkBufferLayers();

        for (RenderType layer : blockLayers) {
            renderLayers.put(layer, buildStructureModel(renderWorld, contraption, layer));
        }
    }

    private void buildInstancedTiles() {
        Collection<TileEntity> tileEntities = contraption.maybeInstancedTileEntities;
        if (!tileEntities.isEmpty()) {
            for (TileEntity te : tileEntities) {
                if (te instanceof IInstanceRendered) {
                    World world = te.getLevel();
                    BlockPos pos = te.getBlockPos();
                    te.setLevelAndPosition(renderWorld, pos);
                    kinetics.add(te);
                    te.setLevelAndPosition(world, pos);
                }
            }
        }
    }

    private void buildActors() {
        contraption.getActors().forEach(kinetics::createActor);
    }

    private static ContraptionModel buildStructureModel(PlacementSimulationWorld renderWorld, Contraption c, RenderType layer) {
        BufferBuilder builder = buildStructure(renderWorld, c, layer);
        return new ContraptionModel(builder);
    }

    private static PlacementSimulationWorld setupRenderWorld(World world, Contraption c) {
        PlacementSimulationWorld renderWorld = new PlacementSimulationWorld(world);

        renderWorld.setTileEntities(c.presentTileEntities.values());

        for (Template.BlockInfo info : c.getBlocks()
                                        .values())
            renderWorld.setBlockAndUpdate(info.pos, info.state);

        WorldLightManager lighter = renderWorld.lighter;

        renderWorld.chunkProvider.getLightSources().forEach((pos) -> lighter.onBlockEmissionIncrease(pos, renderWorld.getLightEmission(pos)));

        lighter.runUpdates(Integer.MAX_VALUE, true, false);

        return renderWorld;
    }

    private static BufferBuilder buildStructure(PlacementSimulationWorld renderWorld, Contraption c, RenderType layer) {

        ForgeHooksClient.setRenderLayer(layer);
        MatrixStack ms = new MatrixStack();
        BlockRendererDispatcher dispatcher = Minecraft.getInstance()
                                                      .getBlockRenderer();
        BlockModelRenderer blockRenderer = dispatcher.getModelRenderer();
        Random random = new Random();
        BufferBuilder builder = new BufferBuilder(DefaultVertexFormats.BLOCK.getIntegerSize());
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        for (Template.BlockInfo info : c.getBlocks()
                                        .values()) {
            BlockState state = info.state;

            if (state.getRenderShape() == BlockRenderType.ENTITYBLOCK_ANIMATED)
                continue;
            if (!RenderTypeLookup.canRenderInLayer(state, layer))
                continue;

            IBakedModel originalModel = dispatcher.getBlockModel(state);
            ms.pushPose();
            ms.translate(info.pos.getX(), info.pos.getY(), info.pos.getZ());
            blockRenderer.renderModel(renderWorld, originalModel, state, info.pos, ms, builder, true, random, 42,
                                      OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
            ms.popPose();
        }

        builder.end();
        return builder;
    }
}
