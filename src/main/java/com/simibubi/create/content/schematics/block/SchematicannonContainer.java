package com.simibubi.create.content.schematics.block;

import com.simibubi.create.AllContainerTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.SlotItemHandler;

public class SchematicannonContainer extends Container {

	private SchematicannonTileEntity te;
	private PlayerEntity player;

	public SchematicannonContainer(ContainerType<?> type, int id, PlayerInventory inv, PacketBuffer buffer) {
		super(type, id);
		player = inv.player;
		ClientWorld world = Minecraft.getInstance().level;
		TileEntity tileEntity = world.getBlockEntity(buffer.readBlockPos());
		if (tileEntity instanceof SchematicannonTileEntity) {
			this.te = (SchematicannonTileEntity) tileEntity;
			this.te.handleUpdateTag(te.getBlockState(), buffer.readNbt());
			init();
		}
	}

	public SchematicannonContainer(ContainerType<?> type, int id, PlayerInventory inv, SchematicannonTileEntity te) {
		super(type, id);
		player = inv.player;
		this.te = te;
		init();
	}

	public static SchematicannonContainer create(int id, PlayerInventory inv, SchematicannonTileEntity te) {
		return new SchematicannonContainer(AllContainerTypes.SCHEMATICANNON.get(), id, inv, te);
	}

	protected void init() {
		int x = 20;
		int y = 0;

		addSlot(new SlotItemHandler(te.inventory, 0, x + 15, y + 65));
		addSlot(new SlotItemHandler(te.inventory, 1, x + 171, y + 65));
		addSlot(new SlotItemHandler(te.inventory, 2, x + 134, y + 19));
		addSlot(new SlotItemHandler(te.inventory, 3, x + 174, y + 19));
		addSlot(new SlotItemHandler(te.inventory, 4, x + 15, y + 19));

		// player Slots
		for (int row = 0; row < 3; ++row) 
			for (int col = 0; col < 9; ++col) 
				addSlot(new Slot(player.inventory, col + row * 9 + 9, -2 + col * 18, 163 + row * 18));
		for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) 
			addSlot(new Slot(player.inventory, hotbarSlot, -2 + hotbarSlot * 18, 221));

		broadcastChanges();
	}

	@Override
	public boolean stillValid(PlayerEntity playerIn) {
		return true;
	}

	@Override
	public void removed(PlayerEntity playerIn) {
		super.removed(playerIn);
	}

	public SchematicannonTileEntity getTileEntity() {
		return te;
	}

	@Override
	public ItemStack quickMoveStack(PlayerEntity playerIn, int index) {
		Slot clickedSlot = getSlot(index);
		if (!clickedSlot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = clickedSlot.getItem();

		if (index < 5) {
			moveItemStackTo(stack, 5, slots.size(), false);
		} else {
			if (moveItemStackTo(stack, 0, 1, false) || moveItemStackTo(stack, 2, 3, false)
					|| moveItemStackTo(stack, 4, 5, false))
				;
		}

		return ItemStack.EMPTY;
	}

}
