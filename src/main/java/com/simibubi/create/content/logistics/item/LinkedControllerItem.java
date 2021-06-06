package com.simibubi.create.content.logistics.item;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllContainerTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.utility.Couple;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.item.Item.Properties;

public class LinkedControllerItem extends Item implements INamedContainerProvider {

	public LinkedControllerItem(Properties p_i48487_1_) {
		super(p_i48487_1_);
	}

	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player == null)
			return ActionResultType.PASS;
		World world = ctx.getLevel();

		if (!player.isShiftKeyDown() && player.mayBuild()
			&& AllBlocks.REDSTONE_LINK.has(world.getBlockState(ctx.getClickedPos()))) {
			if (world.isClientSide)
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.toggleBindMode(ctx.getClickedPos()));
			player.getCooldowns()
				.addCooldown(this, 2);
			return ActionResultType.SUCCESS;
		}

		return use(world, player, ctx.getHand()).getResult();
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack heldItem = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && hand == Hand.MAIN_HAND) {
			if (!world.isClientSide && player instanceof ServerPlayerEntity && player.mayBuild())
				NetworkHooks.openGui((ServerPlayerEntity) player, this, buf -> {
					buf.writeItem(heldItem);
				});
			return ActionResult.success(heldItem);
		}

		if (!player.isShiftKeyDown()) {
			if (world.isClientSide)
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::toggleActive);
			player.getCooldowns()
				.addCooldown(this, 2);
		}

		return ActionResult.pass(heldItem);
	}

	@OnlyIn(Dist.CLIENT)
	private void toggleBindMode(BlockPos pos) {
		LinkedControllerClientHandler.toggleBindMode(pos);
	}

	@OnlyIn(Dist.CLIENT)
	private void toggleActive() {
		LinkedControllerClientHandler.toggle();
	}

	public static ItemStackHandler getFrequencyItems(ItemStack stack) {
		ItemStackHandler newInv = new ItemStackHandler(12);
		if (AllItems.LINKED_CONTROLLER.get() != stack.getItem())
			throw new IllegalArgumentException("Cannot get frequency items from non-controller: " + stack);
		CompoundNBT invNBT = stack.getOrCreateTagElement("Items");
		if (!invNBT.isEmpty())
			newInv.deserializeNBT(invNBT);
		return newInv;
	}

	public static Couple<RedstoneLinkNetworkHandler.Frequency> toFrequency(ItemStack controller, int slot) {
		ItemStackHandler frequencyItems = getFrequencyItems(controller);
		return Couple.create(Frequency.of(frequencyItems.getStackInSlot(slot * 2)),
			Frequency.of(frequencyItems.getStackInSlot(slot * 2 + 1)));
	}

	@Override
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		ItemStack heldItem = player.getMainHandItem();
		return new LinkedControllerContainer(AllContainerTypes.LINKED_CONTROLLER.get(), id, inv, heldItem);
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent(getDescriptionId());
	}

}
