package com.simibubi.create.content.schematics.block;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.contraptions.components.structureMovement.BlockMovementChecks;
import com.simibubi.create.content.contraptions.relays.belt.BeltBlock;
import com.simibubi.create.content.contraptions.relays.belt.BeltPart;
import com.simibubi.create.content.contraptions.relays.belt.BeltSlope;
import com.simibubi.create.content.contraptions.relays.belt.BeltTileEntity;
import com.simibubi.create.content.contraptions.relays.elementary.AbstractShaftBlock;
import com.simibubi.create.content.schematics.ItemRequirement;
import com.simibubi.create.content.schematics.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.MaterialChecklist;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.content.schematics.item.SchematicItem;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.CSchematics;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.foundation.render.backend.instancing.IInstanceRendered;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.IPartialSafeNBT;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTProcessors;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.EmptyHandler;

public class SchematicannonTileEntity extends SmartTileEntity implements INamedContainerProvider, IInstanceRendered {

	public static final int NEIGHBOUR_CHECKING = 100;
	public static final int MAX_ANCHOR_DISTANCE = 256;

	public enum State {
		STOPPED, PAUSED, RUNNING;
	}

	public enum PrintStage {
		BLOCKS, DEFERRED_BLOCKS, ENTITIES
	}

	// Inventory
	public SchematicannonInventory inventory;

	public boolean sendUpdate;
	// Sync
	public boolean dontUpdateChecklist;
	public int neighbourCheckCooldown;

	// Printer
	private SchematicWorld blockReader;
	public BlockPos currentPos;
	public BlockPos schematicAnchor;
	public boolean schematicLoaded;
	public ItemStack missingItem;
	public boolean positionNotLoaded;
	public boolean hasCreativeCrate;
	private int printerCooldown;
	private int skipsLeft;
	private boolean blockSkipped;
	private int printingEntityIndex;
	private PrintStage printStage;

	public BlockPos target;
	public BlockPos previousTarget;
	public LinkedHashSet<LazyOptional<IItemHandler>> attachedInventories;
	public List<LaunchedItem> flyingBlocks;
	public MaterialChecklist checklist;
	public List<BlockPos> deferredBlocks;

	// Gui information
	public float fuelLevel;
	public float bookPrintingProgress;
	public float schematicProgress;
	public String statusMsg;
	public State state;
	public int blocksPlaced;
	public int blocksToPlace;

	// Settings
	public int replaceMode;
	public boolean skipMissing;
	public boolean replaceTileEntities;

	// Render
	public boolean firstRenderTick;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public double getViewDistance() {
		return super.getViewDistance() * 16;
	}

	public SchematicannonTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
		setLazyTickRate(30);
		attachedInventories = new LinkedHashSet<>();
		flyingBlocks = new LinkedList<>();
		inventory = new SchematicannonInventory(this);
		statusMsg = "idle";
		state = State.STOPPED;
		printingEntityIndex = 0;
		printStage = PrintStage.BLOCKS;
		deferredBlocks = new LinkedList<>();
		replaceMode = 2;
		checklist = new MaterialChecklist();
	}

	public void findInventories() {
		hasCreativeCrate = false;
		attachedInventories.clear();
		for (Direction facing : Iterate.directions) {

			if (!level.isLoaded(worldPosition.relative(facing)))
				continue;

			if (AllBlocks.CREATIVE_CRATE.has(level.getBlockState(worldPosition.relative(facing))))
				hasCreativeCrate = true;

			TileEntity tileEntity = level.getBlockEntity(worldPosition.relative(facing));
			if (tileEntity != null) {
				LazyOptional<IItemHandler> capability =
					tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
				if (capability.isPresent()) {
					attachedInventories.add(capability);
				}
			}
		}
	}

	@Override
	protected void fromTag(BlockState blockState, CompoundNBT compound, boolean clientPacket) {
		if (!clientPacket) {
			inventory.deserializeNBT(compound.getCompound("Inventory"));
			if (compound.contains("CurrentPos"))
				currentPos = NBTUtil.readBlockPos(compound.getCompound("CurrentPos"));
		}

		// Gui information
		statusMsg = compound.getString("Status");
		schematicProgress = compound.getFloat("Progress");
		bookPrintingProgress = compound.getFloat("PaperProgress");
		fuelLevel = compound.getFloat("Fuel");
		state = State.valueOf(compound.getString("State"));
		blocksPlaced = compound.getInt("AmountPlaced");
		blocksToPlace = compound.getInt("AmountToPlace");
		printingEntityIndex = compound.getInt("EntityProgress");

		missingItem = null;
		if (compound.contains("MissingItem"))
			missingItem = ItemStack.of(compound.getCompound("MissingItem"));

		// Settings
		CompoundNBT options = compound.getCompound("Options");
		replaceMode = options.getInt("ReplaceMode");
		skipMissing = options.getBoolean("SkipMissing");
		replaceTileEntities = options.getBoolean("ReplaceTileEntities");

		// Printer & Flying Blocks
		if (compound.contains("PrintStage"))
			printStage = PrintStage.valueOf(compound.getString("PrintStage"));
		if (compound.contains("Target"))
			target = NBTUtil.readBlockPos(compound.getCompound("Target"));
		if (compound.contains("DeferredBlocks"))
			compound.getList("DeferredBlocks", 10).stream()
					.map(p -> NBTUtil.readBlockPos((CompoundNBT) p))
					.collect(Collectors.toCollection(() -> deferredBlocks));
		if (compound.contains("FlyingBlocks"))
			readFlyingBlocks(compound);

		super.fromTag(blockState, compound, clientPacket);
	}

	protected void readFlyingBlocks(CompoundNBT compound) {
		ListNBT tagBlocks = compound.getList("FlyingBlocks", 10);
		if (tagBlocks.isEmpty())
			flyingBlocks.clear();

		boolean pastDead = false;

		for (int i = 0; i < tagBlocks.size(); i++) {
			CompoundNBT c = tagBlocks.getCompound(i);
			LaunchedItem launched = LaunchedItem.fromNBT(c);
			BlockPos readBlockPos = launched.target;

			// Always write to Server tile
			if (level == null || !level.isClientSide) {
				flyingBlocks.add(launched);
				continue;
			}

			// Delete all Client side blocks that are now missing on the server
			while (!pastDead && !flyingBlocks.isEmpty() && !flyingBlocks.get(0).target.equals(readBlockPos)) {
				flyingBlocks.remove(0);
			}

			pastDead = true;

			// Add new server side blocks
			if (i >= flyingBlocks.size()) {
				flyingBlocks.add(launched);
				continue;
			}

			// Don't do anything with existing
		}
	}

	@Override
	public void write(CompoundNBT compound, boolean clientPacket) {
		if (!clientPacket) {
			compound.put("Inventory", inventory.serializeNBT());
			if (state == State.RUNNING) {
				compound.putBoolean("Running", true);
				if (currentPos != null)
					compound.put("CurrentPos", NBTUtil.writeBlockPos(currentPos));
			}
		}

		// Gui information
		compound.putFloat("Progress", schematicProgress);
		compound.putFloat("PaperProgress", bookPrintingProgress);
		compound.putFloat("Fuel", fuelLevel);
		compound.putString("Status", statusMsg);
		compound.putString("State", state.name());
		compound.putInt("AmountPlaced", blocksPlaced);
		compound.putInt("AmountToPlace", blocksToPlace);
		compound.putInt("EntityProgress", printingEntityIndex);

		if (missingItem != null)
			compound.put("MissingItem", missingItem.serializeNBT());

		// Settings
		CompoundNBT options = new CompoundNBT();
		options.putInt("ReplaceMode", replaceMode);
		options.putBoolean("SkipMissing", skipMissing);
		options.putBoolean("ReplaceTileEntities", replaceTileEntities);
		compound.put("Options", options);

		// Printer & Flying Blocks
		compound.putString("PrintStage", printStage.name());

		if (target != null)
			compound.put("Target", NBTUtil.writeBlockPos(target));

		ListNBT tagDeferredBlocks = new ListNBT();
		for (BlockPos p : deferredBlocks)
			tagDeferredBlocks.add(NBTUtil.writeBlockPos(p));
		compound.put("DeferredBlocks", tagDeferredBlocks);

		ListNBT tagFlyingBlocks = new ListNBT();
		for (LaunchedItem b : flyingBlocks)
			tagFlyingBlocks.add(b.serializeNBT());
		compound.put("FlyingBlocks", tagFlyingBlocks);

		super.write(compound, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		if (state != State.STOPPED && neighbourCheckCooldown-- <= 0) {
			neighbourCheckCooldown = NEIGHBOUR_CHECKING;
			findInventories();
		}

		firstRenderTick = true;
		previousTarget = target;
		tickFlyingBlocks();

		if (level.isClientSide)
			return;

		// Update Fuel and Paper
		tickPaperPrinter();
		refillFuelIfPossible();

		// Update Printer
		skipsLeft = config().schematicannonSkips.get();
		blockSkipped = true;

		while (blockSkipped && skipsLeft-- > 0)
			tickPrinter();

		schematicProgress = 0;
		if (blocksToPlace > 0)
			schematicProgress = (float) blocksPlaced / blocksToPlace;

		// Update Client Tile
		if (sendUpdate) {
			sendUpdate = false;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 6);
		}
	}

	public CSchematics config() {
		return AllConfigs.SERVER.schematics;
	}

	protected void tickPrinter() {
		ItemStack blueprint = inventory.getStackInSlot(0);
		blockSkipped = false;

		// Skip if not Active
		if (state == State.STOPPED) {
			if (schematicLoaded)
				resetPrinter();
			return;
		}

		if (blueprint.isEmpty()) {
			state = State.STOPPED;
			statusMsg = "idle";
			sendUpdate = true;
			return;
		}

		if (state == State.PAUSED && !positionNotLoaded && missingItem == null && fuelLevel > getFuelUsageRate())
			return;

		// Initialize Printer
		if (!schematicLoaded) {
			initializePrinter(blueprint);
			return;
		}

		// Cooldown from last shot
		if (printerCooldown > 0) {
			printerCooldown--;
			return;
		}

		// Check Fuel
		if (fuelLevel <= 0 && !hasCreativeCrate) {
			fuelLevel = 0;
			state = State.PAUSED;
			statusMsg = "noGunpowder";
			sendUpdate = true;
			return;
		}

		// Update Target
		if (hasCreativeCrate) {
			if (missingItem != null) {
				missingItem = null;
				state = State.RUNNING;
			}
		}

		if (missingItem == null && !positionNotLoaded) {
			do {
				advanceCurrentPos();
				if (state == State.STOPPED)
					return;

			} while (!blockReader.getBounds()
				.isInside(currentPos));

			sendUpdate = true;
			target = schematicAnchor.offset(currentPos);
		}

		boolean entityMode = printStage == PrintStage.ENTITIES;

		// Check block
		if (!getLevel().isAreaLoaded(target, 0)) {
			positionNotLoaded = true;
			statusMsg = "targetNotLoaded";
			state = State.PAUSED;
			return;
		} else {
			if (positionNotLoaded) {
				positionNotLoaded = false;
				state = State.RUNNING;
			}
		}

		boolean shouldSkip = false;
		BlockState blockState = Blocks.AIR.defaultBlockState();
		TileEntity tileEntity = null;
		ItemRequirement requirement;

		if (entityMode) {
			requirement = ItemRequirement.of(blockReader.getEntities()
				.collect(Collectors.toList())
				.get(printingEntityIndex));

		} else {
			blockState = BlockHelper.setZeroAge(blockReader.getBlockState(target));
			tileEntity = blockReader.getBlockEntity(target);
			requirement = ItemRequirement.of(blockState, tileEntity);
			shouldSkip = !shouldPlace(target, blockState, tileEntity);
		}

		if (shouldSkip || requirement.isInvalid()) {
			statusMsg = "searching";
			blockSkipped = true;
			return;
		}

		// Find item
		List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
		if (!requirement.isEmpty()) {
			for (ItemRequirement.StackRequirement required : requiredItems) {
				if (!grabItemsFromAttachedInventories(required.item, required.usage, true)) {
					if (skipMissing) {
						statusMsg = "skipping";
						blockSkipped = true;
						if (missingItem != null) {
							missingItem = null;
							state = State.RUNNING;
						}
						return;
					}

					missingItem = required.item;
					state = State.PAUSED;
					statusMsg = "missingBlock";
					return;
				}
			}

			for (ItemRequirement.StackRequirement required : requiredItems)
				grabItemsFromAttachedInventories(required.item, required.usage, false);
		}

		// Success
		state = State.RUNNING;
		if (blockState.getBlock() != Blocks.AIR || entityMode)
			statusMsg = "placing";
		else
			statusMsg = "clearing";

		ItemStack icon = requirement.isEmpty() || requiredItems.isEmpty() ? ItemStack.EMPTY : requiredItems.get(0).item;
		if (entityMode)
			launchEntity(target, icon, blockReader.getEntities()
				.collect(Collectors.toList())
				.get(printingEntityIndex));
		else if (AllBlocks.BELT.has(blockState)) {
			TileEntity te = blockReader.getBlockEntity(currentPos.offset(schematicAnchor));
			blockState = stripBeltIfNotLast(blockState);
			if (te instanceof BeltTileEntity && AllBlocks.BELT.has(blockState))
				launchBelt(target, blockState, ((BeltTileEntity) te).beltLength);
			else
				launchBlock(target, icon, blockState, null);
		} else {
			CompoundNBT data = null;
			TileEntity tile = blockReader.getBlockEntity(target);
			if (tile != null) {
				if (AllBlockTags.SAFE_NBT.matches(blockState)) {
					data = tile.save(new CompoundNBT());
					data = NBTProcessors.process(tile, data, true);
				} else if (tile instanceof IPartialSafeNBT) {
					data = new CompoundNBT();
					((IPartialSafeNBT) tile).writeSafe(data, false);
					data = NBTProcessors.process(tile, data, true);
				}
			}

			launchBlock(target, icon, blockState, data);
		}

		printerCooldown = config().schematicannonDelay.get();
		fuelLevel -= getFuelUsageRate();
		sendUpdate = true;
		missingItem = null;
	}

	public BlockState stripBeltIfNotLast(BlockState blockState) {
		// is highest belt?
		boolean isLastSegment = false;
		Direction facing = blockState.getValue(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope = blockState.getValue(BeltBlock.SLOPE);
		boolean positive = facing.getAxisDirection() == AxisDirection.POSITIVE;
		boolean start = blockState.getValue(BeltBlock.PART) == BeltPart.START;
		boolean end = blockState.getValue(BeltBlock.PART) == BeltPart.END;

		switch (slope) {
		case DOWNWARD:
			isLastSegment = start;
			break;
		case UPWARD:
			isLastSegment = end;
			break;
		case HORIZONTAL:
		case VERTICAL:
		default:
			isLastSegment = positive && end || !positive && start;
		}
		if (!isLastSegment)
			blockState = (blockState.getValue(BeltBlock.PART) == BeltPart.MIDDLE) ? Blocks.AIR.defaultBlockState()
				: AllBlocks.SHAFT.getDefaultState()
					.setValue(AbstractShaftBlock.AXIS, facing.getClockWise()
						.getAxis());
		return blockState;
	}

	public double getFuelUsageRate() {
		return hasCreativeCrate ? 0 : config().schematicannonFuelUsage.get() / 100f;
	}

	protected void initializePrinter(ItemStack blueprint) {
		if (!blueprint.hasTag()) {
			state = State.STOPPED;
			statusMsg = "schematicInvalid";
			sendUpdate = true;
			return;
		}

		if (!blueprint.getTag()
			.getBoolean("Deployed")) {
			state = State.STOPPED;
			statusMsg = "schematicNotPlaced";
			sendUpdate = true;
			return;
		}

		// Load blocks into reader
		Template activeTemplate = SchematicItem.loadSchematic(blueprint);
		BlockPos anchor = NBTUtil.readBlockPos(blueprint.getTag()
			.getCompound("Anchor"));

		if (activeTemplate.getSize()
			.equals(BlockPos.ZERO)) {
			state = State.STOPPED;
			statusMsg = "schematicExpired";
			inventory.setStackInSlot(0, ItemStack.EMPTY);
			inventory.setStackInSlot(1, new ItemStack(AllItems.EMPTY_SCHEMATIC.get()));
			return;
		}

		if (!anchor.closerThan(getBlockPos(), MAX_ANCHOR_DISTANCE)) {
			state = State.STOPPED;
			statusMsg = "targetOutsideRange";
			return;
		}

		schematicAnchor = anchor;
		blockReader = new SchematicWorld(schematicAnchor, level);
		PlacementSettings settings = SchematicItem.getSettings(blueprint);
		activeTemplate.placeInWorldChunk(blockReader, schematicAnchor, settings, blockReader.getRandom());
		schematicLoaded = true;
		state = State.PAUSED;
		statusMsg = "ready";
		printingEntityIndex = 0;
		printStage = PrintStage.BLOCKS;
		deferredBlocks.clear();
		updateChecklist();
		sendUpdate = true;
		blocksToPlace += blocksPlaced;
		MutableBoundingBox bounds = blockReader.getBounds();
		currentPos = currentPos != null ? currentPos.west() : new BlockPos(bounds.x0 - 1, bounds.y0, bounds.z0);
	}

	protected ItemStack getItemForBlock(BlockState blockState) {
		Item item = BlockItem.BY_BLOCK.getOrDefault(blockState.getBlock(), Items.AIR);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	protected boolean grabItemsFromAttachedInventories(ItemStack required, ItemUseType usage, boolean simulate) {
		if (hasCreativeCrate)
			return true;

		attachedInventories.removeIf(cap -> !cap.isPresent());

		// Find and apply damage
		if (usage == ItemUseType.DAMAGE) {
			for (LazyOptional<IItemHandler> cap : attachedInventories) {
				IItemHandler iItemHandler = cap.orElse(EmptyHandler.INSTANCE);
				for (int slot = 0; slot < iItemHandler.getSlots(); slot++) {
					ItemStack extractItem = iItemHandler.extractItem(slot, 1, true);
					if (!ItemRequirement.validate(required, extractItem))
						continue;
					if (!extractItem.isDamageableItem())
						continue;

					if (!simulate) {
						ItemStack stack = iItemHandler.extractItem(slot, 1, false);
						stack.setDamageValue(stack.getDamageValue() + 1);
						if (stack.getDamageValue() <= stack.getMaxDamage()) {
							if (iItemHandler.getStackInSlot(slot)
								.isEmpty())
								iItemHandler.insertItem(slot, stack, false);
							else
								ItemHandlerHelper.insertItem(iItemHandler, stack, false);
						}
					}

					return true;
				}
			}
		}

		// Find and remove
		boolean success = false;
		if (usage == ItemUseType.CONSUME) {
			int amountFound = 0;
			for (LazyOptional<IItemHandler> cap : attachedInventories) {
				IItemHandler iItemHandler = cap.orElse(EmptyHandler.INSTANCE);
				amountFound += ItemHelper
					.extract(iItemHandler, s -> ItemRequirement.validate(required, s), ExtractionCountMode.UPTO,
						required.getCount(), true)
					.getCount();

				if (amountFound < required.getCount())
					continue;

				success = true;
				break;
			}
		}

		if (!simulate && success) {
			int amountFound = 0;
			for (LazyOptional<IItemHandler> cap : attachedInventories) {
				IItemHandler iItemHandler = cap.orElse(EmptyHandler.INSTANCE);
				amountFound += ItemHelper
					.extract(iItemHandler, s -> ItemRequirement.validate(required, s), ExtractionCountMode.UPTO,
						required.getCount(), false)
					.getCount();
				if (amountFound < required.getCount())
					continue;
				break;
			}
		}

		return success;
	}

	protected void advanceCurrentPos() {
		List<Entity> entities = blockReader.getEntities()
			.collect(Collectors.toList());

		if (printStage == PrintStage.BLOCKS) {
			MutableBoundingBox bounds = blockReader.getBounds();
			while (tryAdvanceCurrentPos(bounds, entities)) {
				deferredBlocks.add(currentPos);
			}
		}

		if (printStage == PrintStage.DEFERRED_BLOCKS) {
			if (deferredBlocks.isEmpty()) {
				printStage = PrintStage.ENTITIES;
			} else {
				currentPos = deferredBlocks.remove(0);
			}
		}

		if (printStage == PrintStage.ENTITIES) {
			if (printingEntityIndex < entities.size()) {
				currentPos = entities.get(printingEntityIndex).blockPosition().subtract(schematicAnchor);
				printingEntityIndex++;
			} else {
				finishedPrinting();
			}
		}
	}

	protected boolean tryAdvanceCurrentPos(MutableBoundingBox bounds, List<Entity> entities) {
		currentPos = currentPos.relative(Direction.EAST);
		BlockPos posInBounds = currentPos.offset(-bounds.x0, -bounds.y0, -bounds.z0);

		if (posInBounds.getX() > bounds.getXSpan())
			currentPos = new BlockPos(bounds.x0, currentPos.getY(), currentPos.getZ() + 1).west();
		if (posInBounds.getZ() > bounds.getZSpan())
			currentPos = new BlockPos(currentPos.getX(), currentPos.getY() + 1, bounds.z0).west();

		// End of blocks reached
		if (currentPos.getY() > bounds.getYSpan()) {
			printStage = PrintStage.DEFERRED_BLOCKS;
			return false;
		}

		return shouldDeferBlock(blockReader.getBlockState(schematicAnchor.offset(currentPos)));
	}

	public static boolean shouldDeferBlock(BlockState state) {
		return state.getBlock().is(AllBlocks.GANTRY_CARRIAGE.get()) || BlockMovementChecks.isBrittle(state);
	}

	public void finishedPrinting() {
		inventory.setStackInSlot(0, ItemStack.EMPTY);
		inventory.setStackInSlot(1, new ItemStack(AllItems.EMPTY_SCHEMATIC.get(), inventory.getStackInSlot(1)
			.getCount() + 1));
		state = State.STOPPED;
		statusMsg = "finished";
		resetPrinter();
		target = getBlockPos().offset(1, 0, 0);
		AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(level, worldPosition);
		sendUpdate = true;
	}

	protected void resetPrinter() {
		schematicLoaded = false;
		schematicAnchor = null;
		currentPos = null;
		blockReader = null;
		missingItem = null;
		sendUpdate = true;
		printingEntityIndex = 0;
		printStage = PrintStage.BLOCKS;
		schematicProgress = 0;
		blocksPlaced = 0;
		blocksToPlace = 0;
		deferredBlocks.clear();
	}

	protected boolean shouldPlace(BlockPos pos, BlockState state, TileEntity te) {
		if (level == null)
			return false;
		BlockState toReplace = level.getBlockState(pos);
		boolean placingAir = state.getBlock()
			.isAir(state, level, pos);

		BlockState toReplaceOther = null;
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
			&& state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT)
			toReplaceOther = level.getBlockState(pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
			&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)
			toReplaceOther = level.getBlockState(pos.above());

		if (!level.isLoaded(pos))
			return false;
		if (!level.getWorldBorder()
			.isWithinBounds(pos))
			return false;
		if (toReplace == state)
			return false;
		if (toReplace.getDestroySpeed(level, pos) == -1
			|| (toReplaceOther != null && toReplaceOther.getDestroySpeed(level, pos) == -1))
			return false;
		if (pos.closerThan(getBlockPos(), 2f))
			return false;
		if (!replaceTileEntities
			&& (toReplace.hasTileEntity() || (toReplaceOther != null && toReplaceOther.hasTileEntity())))
			return false;

		if (shouldIgnoreBlockState(state, te))
			return false;

		if (replaceMode == 3)
			return true;
		if (replaceMode == 2 && !placingAir)
			return true;
		if (replaceMode == 1
			&& (state.isRedstoneConductor(blockReader, pos.subtract(schematicAnchor)) || (!toReplace.isRedstoneConductor(level, pos)
				&& (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level, pos))))
			&& !placingAir)
			return true;
		if (replaceMode == 0 && !toReplace.isRedstoneConductor(level, pos)
			&& (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level, pos)) && !placingAir)
			return true;

		return false;
	}

	protected boolean shouldIgnoreBlockState(BlockState state, TileEntity te) {
		// Block doesnt have a mapping (Water, lava, etc)
		if (state.getBlock() == Blocks.STRUCTURE_VOID)
			return true;

		ItemRequirement requirement = ItemRequirement.of(state, te);
		if (requirement.isEmpty())
			return false;
		if (requirement.isInvalid())
			return false;

		// Block doesnt need to be placed twice (Doors, beds, double plants)
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
			&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
			return true;
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD)
			return true;
		if (state.getBlock() instanceof PistonHeadBlock)
			return true;

		return false;
	}

	protected void tickFlyingBlocks() {
		List<LaunchedItem> toRemove = new LinkedList<>();
		for (LaunchedItem b : flyingBlocks)
			if (b.update(level))
				toRemove.add(b);
		flyingBlocks.removeAll(toRemove);
	}

	protected void refillFuelIfPossible() {
		if (hasCreativeCrate)
			return;
		if (1 - fuelLevel + 1 / 128f < getFuelAddedByGunPowder())
			return;
		if (inventory.getStackInSlot(4)
			.isEmpty())
			return;

		inventory.getStackInSlot(4)
			.shrink(1);
		fuelLevel += getFuelAddedByGunPowder();
		sendUpdate = true;
	}

	public double getFuelAddedByGunPowder() {
		return config().schematicannonGunpowderWorth.get() / 100f;
	}

	protected void tickPaperPrinter() {
		int BookInput = 2;
		int BookOutput = 3;

		ItemStack blueprint = inventory.getStackInSlot(0);
		ItemStack paper = inventory.extractItem(BookInput, 1, true);
		boolean outputFull = inventory.getStackInSlot(BookOutput)
			.getCount() == inventory.getSlotLimit(BookOutput);

		if (paper.isEmpty() || outputFull) {
			if (bookPrintingProgress != 0)
				sendUpdate = true;
			bookPrintingProgress = 0;
			dontUpdateChecklist = false;
			return;
		}

		if (!schematicLoaded) {
			if (!blueprint.isEmpty())
				initializePrinter(blueprint);
			return;
		}

		if (bookPrintingProgress >= 1) {
			bookPrintingProgress = 0;

			if (!dontUpdateChecklist)
				updateChecklist();

			dontUpdateChecklist = true;
			inventory.extractItem(BookInput, 1, false);
			ItemStack stack = checklist.createItem();
			stack.setCount(inventory.getStackInSlot(BookOutput)
				.getCount() + 1);
			inventory.setStackInSlot(BookOutput, stack);
			sendUpdate = true;
			return;
		}

		bookPrintingProgress += 0.05f;
		sendUpdate = true;
	}

	protected void launchBelt(BlockPos target, BlockState state, int length) {
		blocksPlaced++;
		ItemStack connector = AllItems.BELT_CONNECTOR.asStack();
		flyingBlocks.add(new LaunchedItem.ForBelt(this.getBlockPos(), target, connector, state, length));
		playFiringSound();
	}

	protected void launchBlock(BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundNBT data) {
		if (state.getBlock()
			.isAir(state, level, target))
			blocksPlaced++;
		flyingBlocks.add(new LaunchedItem.ForBlockState(this.getBlockPos(), target, stack, state, data));
		playFiringSound();
	}

	protected void launchEntity(BlockPos target, ItemStack stack, Entity entity) {
		blocksPlaced++;
		flyingBlocks.add(new LaunchedItem.ForEntity(this.getBlockPos(), target, stack, entity));
		playFiringSound();
	}

	public void playFiringSound() {
		AllSoundEvents.SCHEMATICANNON_LAUNCH_BLOCK.playOnServer(level, worldPosition);
	}

	public void sendToContainer(PacketBuffer buffer) {
		buffer.writeBlockPos(getBlockPos());
		buffer.writeNbt(getUpdateTag());
	}

	@Override
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return SchematicannonContainer.create(id, inv, this);
	}

	@Override
	public ITextComponent getDisplayName() {
		return new StringTextComponent(getType().getRegistryName()
			.toString());
	}

	public void updateChecklist() {
		checklist.required.clear();
		checklist.damageRequired.clear();
		checklist.blocksNotLoaded = false;

		if (schematicLoaded) {
			blocksToPlace = blocksPlaced;
			for (BlockPos pos : blockReader.getAllPositions()) {
				BlockPos relPos = pos.offset(schematicAnchor);
				BlockState required = blockReader.getBlockState(relPos);
				TileEntity requiredTE = blockReader.getBlockEntity(relPos);

				if (!getLevel().isAreaLoaded(pos.offset(schematicAnchor), 0)) {
					checklist.warnBlockNotLoaded();
					continue;
				}
				if (!shouldPlace(pos.offset(schematicAnchor), required, requiredTE))
					continue;
				ItemRequirement requirement = ItemRequirement.of(required, blockReader.getBlockEntity(relPos));
				if (requirement.isEmpty())
					continue;
				if (requirement.isInvalid())
					continue;
				checklist.require(requirement);
				blocksToPlace++;
			}
			blockReader.getEntities()
				.forEach(entity -> {
					ItemRequirement requirement = ItemRequirement.of(entity);
					if (requirement.isEmpty())
						return;
					if (requirement.isInvalid())
						return;
					checklist.require(requirement);
				});

		}
		checklist.gathered.clear();
		findInventories();
		for (LazyOptional<IItemHandler> cap : attachedInventories) {
			if (!cap.isPresent())
				continue;
			IItemHandler inventory = cap.orElse(EmptyHandler.INSTANCE);
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				ItemStack stackInSlot = inventory.getStackInSlot(slot);
				if (inventory.extractItem(slot, 1, true)
					.isEmpty())
					continue;
				checklist.collect(stackInSlot);
			}
		}
		sendUpdate = true;
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {}

	@Override
	public void lazyTick() {
		super.lazyTick();
		findInventories();
	}

	@Override
	public boolean shouldRenderAsTE() {
		return true;
	}
}
