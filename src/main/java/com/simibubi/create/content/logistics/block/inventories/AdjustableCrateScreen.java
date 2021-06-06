package com.simibubi.create.content.logistics.block.inventories;

import static com.simibubi.create.foundation.gui.AllGuiTextures.ADJUSTABLE_CRATE;
import static com.simibubi.create.foundation.gui.AllGuiTextures.ADJUSTABLE_DOUBLE_CRATE;
import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.packet.ConfigureFlexcratePacket;
import com.simibubi.create.foundation.gui.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.gui.widgets.Label;
import com.simibubi.create.foundation.gui.widgets.ScrollInput;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class AdjustableCrateScreen extends AbstractSimiContainerScreen<AdjustableCrateContainer> {

	private AdjustableCrateTileEntity te;
	private Label allowedItemsLabel;
	private ScrollInput allowedItems;
	private int lastModification;

	private List<Rectangle2d> extraAreas;

	private final ItemStack renderedItem = AllBlocks.ADJUSTABLE_CRATE.asStack();
	private final ITextComponent title = Lang.translate("gui.adjustable_crate.title");
	private final ITextComponent storageSpace = Lang.translate("gui.adjustable_crate.storageSpace");

	public AdjustableCrateScreen(AdjustableCrateContainer container, PlayerInventory inv, ITextComponent title) {
		super(container, inv, title);
		te = container.te;
		lastModification = -1;
	}

	@Override
	protected void init() {
		setWindowSize(PLAYER_INVENTORY.width + 100, ADJUSTABLE_CRATE.height + PLAYER_INVENTORY.height + 20);
		super.init();
		widgets.clear();

		allowedItemsLabel = new Label(leftPos + 100 + 69, topPos + 108, StringTextComponent.EMPTY).colored(0xfefefe)
			.withShadow();
		allowedItems = new ScrollInput(leftPos + 100 + 65, topPos + 104, 41, 14).titled(storageSpace.plainCopy())
			.withRange(1, (menu.doubleCrate ? 2049 : 1025))
			.writingTo(allowedItemsLabel)
			.withShiftStep(64)
			.setState(te.allowedAmount)
			.calling(s -> lastModification = 0);
		allowedItems.onChanged();
		widgets.add(allowedItemsLabel);
		widgets.add(allowedItems);

		extraAreas = new ArrayList<>();
		extraAreas.add(new Rectangle2d(leftPos + ADJUSTABLE_CRATE.width + 110, topPos + 46, 71, 70));
	}

	@Override
	protected void renderWindow(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		int crateLeft = leftPos + 100;
		int crateTop = topPos;
		int invLeft = leftPos + 50;
		int invTop = crateTop + ADJUSTABLE_CRATE.height + 10;
		int fontColor = 0x4B3A22;

		if (menu.doubleCrate) {
			crateLeft -= 72;
			ADJUSTABLE_DOUBLE_CRATE.draw(matrixStack, this, crateLeft, crateTop);
		} else
			ADJUSTABLE_CRATE.draw(matrixStack,this, crateLeft, crateTop);

		font.drawShadow(matrixStack, title, crateLeft - 3 + (ADJUSTABLE_CRATE.width - font.width(title)) / 2,
			crateTop + 3, 0xfefefe);
		String itemCount = "" + te.itemCount;
		font.draw(matrixStack, itemCount, leftPos + 100 + 53 - font.width(itemCount), crateTop + 107, fontColor);

		PLAYER_INVENTORY.draw(matrixStack, this, invLeft, invTop);
		font.draw(matrixStack, inventory.getDisplayName(), invLeft + 7, invTop + 6, 0x666666);

		for (int slot = 0; slot < (menu.doubleCrate ? 32 : 16); slot++) {
			if (allowedItems.getState() > slot * 64)
				continue;
			int slotsPerRow = (menu.doubleCrate ? 8 : 4);
			int x = crateLeft + 22 + (slot % slotsPerRow) * 18;
			int y = crateTop + 19 + (slot / slotsPerRow) * 18;
			AllGuiTextures.ADJUSTABLE_CRATE_LOCKED_SLOT.draw(matrixStack, this, x, y);
		}

		GuiGameElement.of(renderedItem)
				.<GuiGameElement.GuiRenderBuilder>at(leftPos + ADJUSTABLE_CRATE.width + 110, topPos + 70, -150)
				.scale(5)
				.render(matrixStack);
	}

	@Override
	public void removed() {
		AllPackets.channel.sendToServer(new ConfigureFlexcratePacket(te.getBlockPos(), allowedItems.getState()));
	}

	@Override
	public void tick() {
		super.tick();

		if (!AllBlocks.ADJUSTABLE_CRATE.has(minecraft.level.getBlockState(te.getBlockPos())))
			minecraft.setScreen(null);

		if (lastModification >= 0)
			lastModification++;

		if (lastModification >= 15) {
			lastModification = -1;
			AllPackets.channel.sendToServer(new ConfigureFlexcratePacket(te.getBlockPos(), allowedItems.getState()));
		}

		if (menu.doubleCrate != te.isDoubleCrate())
			menu.playerInventory.player.closeContainer();
	}

	@Override
	public List<Rectangle2d> getExtraAreas() {
		return extraAreas;
	}
}
