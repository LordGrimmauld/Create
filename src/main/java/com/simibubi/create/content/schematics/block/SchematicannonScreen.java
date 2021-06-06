package com.simibubi.create.content.schematics.block;

import static net.minecraft.util.text.TextFormatting.BLUE;
import static net.minecraft.util.text.TextFormatting.DARK_PURPLE;
import static net.minecraft.util.text.TextFormatting.GRAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.schematics.packet.ConfigureSchematicannonPacket;
import com.simibubi.create.content.schematics.packet.ConfigureSchematicannonPacket.Option;
import com.simibubi.create.foundation.gui.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.GuiGameElement;
import com.simibubi.create.foundation.gui.widgets.IconButton;
import com.simibubi.create.foundation.gui.widgets.Indicator;
import com.simibubi.create.foundation.gui.widgets.Indicator.State;
import com.simibubi.create.foundation.item.ItemDescription.Palette;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class SchematicannonScreen extends AbstractSimiContainerScreen<SchematicannonContainer> {

	private static final AllGuiTextures BG_BOTTOM = AllGuiTextures.SCHEMATICANNON_BOTTOM;
	private static final AllGuiTextures BG_TOP = AllGuiTextures.SCHEMATICANNON_TOP;

	protected Vector<Indicator> replaceLevelIndicators;
	protected Vector<IconButton> replaceLevelButtons;

	protected IconButton skipMissingButton;
	protected Indicator skipMissingIndicator;
	protected IconButton skipTilesButton;
	protected Indicator skipTilesIndicator;

	protected IconButton playButton;
	protected Indicator playIndicator;
	protected IconButton pauseButton;
	protected Indicator pauseIndicator;
	protected IconButton resetButton;
	protected Indicator resetIndicator;

	private List<Rectangle2d> extraAreas;
	protected List<Widget> placementSettingWidgets;

	private final ITextComponent title = Lang.translate("gui.schematicannon.title");
	private final ITextComponent listPrinter = Lang.translate("gui.schematicannon.listPrinter");
	private final String _gunpowderLevel = "gui.schematicannon.gunpowderLevel";
	private final String _shotsRemaining = "gui.schematicannon.shotsRemaining";
	private final String _showSettings = "gui.schematicannon.showOptions";
	private final String _shotsRemainingWithBackup = "gui.schematicannon.shotsRemainingWithBackup";

	private final String _slotGunpowder = "gui.schematicannon.slot.gunpowder";
	private final String _slotListPrinter = "gui.schematicannon.slot.listPrinter";
	private final String _slotSchematic = "gui.schematicannon.slot.schematic";

	private final ITextComponent optionEnabled = Lang.translate("gui.schematicannon.optionEnabled");
	private final ITextComponent optionDisabled = Lang.translate("gui.schematicannon.optionDisabled");

	private final ItemStack renderedItem = AllBlocks.SCHEMATICANNON.asStack();

	private IconButton confirmButton;
	private IconButton showSettingsButton;
	private Indicator showSettingsIndicator;

	public SchematicannonScreen(SchematicannonContainer container, PlayerInventory inventory,
								ITextComponent p_i51105_3_) {
		super(container, inventory, p_i51105_3_);
		placementSettingWidgets = new ArrayList<>();
	}

	@Override
	protected void init() {
		setWindowSize(BG_TOP.width + 50, BG_BOTTOM.height + BG_TOP.height + 80);
		super.init();

		int x = leftPos + 20;
		int y = topPos;

		widgets.clear();

		// Play Pause Stop
		playButton = new IconButton(x + 75, y + 86, AllIcons.I_PLAY);
		playIndicator = new Indicator(x + 75, y + 79, StringTextComponent.EMPTY);
		pauseButton = new IconButton(x + 93, y + 86, AllIcons.I_PAUSE);
		pauseIndicator = new Indicator(x + 93, y + 79, StringTextComponent.EMPTY);
		resetButton = new IconButton(x + 111, y + 86, AllIcons.I_STOP);
		resetIndicator = new Indicator(x + 111, y + 79, StringTextComponent.EMPTY);
		resetIndicator.state = State.RED;
		Collections.addAll(widgets, playButton, playIndicator, pauseButton, pauseIndicator, resetButton,
			resetIndicator);

		extraAreas = new ArrayList<>();
		extraAreas.add(new Rectangle2d(leftPos + 240, topPos + 88, 84, 113));

		confirmButton = new IconButton(x + 180, topPos + 117, AllIcons.I_CONFIRM);
		widgets.add(confirmButton);
		showSettingsButton = new IconButton(leftPos + 29, topPos + 117, AllIcons.I_PLACEMENT_SETTINGS);
		showSettingsButton.setToolTip(Lang.translate(_showSettings));
		widgets.add(showSettingsButton);
		showSettingsIndicator = new Indicator(leftPos + 29, topPos + 111, StringTextComponent.EMPTY);
		widgets.add(showSettingsIndicator);

		tick();
	}

	private void initPlacementSettings() {
		widgets.removeAll(placementSettingWidgets);
		placementSettingWidgets.clear();

		if (placementSettingsHidden())
			return;

		int x = leftPos + 20;
		int y = topPos;

		// Replace settings
		replaceLevelButtons = new Vector<>(4);
		replaceLevelIndicators = new Vector<>(4);
		List<AllIcons> icons = ImmutableList.of(AllIcons.I_DONT_REPLACE, AllIcons.I_REPLACE_SOLID,
			AllIcons.I_REPLACE_ANY, AllIcons.I_REPLACE_EMPTY);
		List<ITextComponent> toolTips = ImmutableList.of(Lang.translate("gui.schematicannon.option.dontReplaceSolid"),
			Lang.translate("gui.schematicannon.option.replaceWithSolid"),
			Lang.translate("gui.schematicannon.option.replaceWithAny"),
			Lang.translate("gui.schematicannon.option.replaceWithEmpty"));

		for (int i = 0; i < 4; i++) {
			replaceLevelIndicators.add(new Indicator(x + 33 + i * 18, y + 111, StringTextComponent.EMPTY));
			replaceLevelButtons.add(new IconButton(x + 33 + i * 18, y + 117, icons.get(i)));
			replaceLevelButtons.get(i)
				.setToolTip(toolTips.get(i));
		}
		placementSettingWidgets.addAll(replaceLevelButtons);
		placementSettingWidgets.addAll(replaceLevelIndicators);

		// Other Settings
		skipMissingButton = new IconButton(x + 111, y + 117, AllIcons.I_SKIP_MISSING);
		skipMissingButton.setToolTip(Lang.translate("gui.schematicannon.option.skipMissing"));
		skipMissingIndicator = new Indicator(x + 111, y + 111, StringTextComponent.EMPTY);
		Collections.addAll(placementSettingWidgets, skipMissingButton, skipMissingIndicator);

		skipTilesButton = new IconButton(x + 129, y + 117, AllIcons.I_SKIP_TILES);
		skipTilesButton.setToolTip(Lang.translate("gui.schematicannon.option.skipTileEntities"));
		skipTilesIndicator = new Indicator(x + 129, y + 111, StringTextComponent.EMPTY);
		Collections.addAll(placementSettingWidgets, skipTilesButton, skipTilesIndicator);

		widgets.addAll(placementSettingWidgets);
	}

	protected boolean placementSettingsHidden() {
		return showSettingsIndicator.state == State.OFF;
	}

	@Override
	public void tick() {
		SchematicannonTileEntity te = menu.getTileEntity();

		if (!placementSettingsHidden()) {
			for (int replaceMode = 0; replaceMode < replaceLevelButtons.size(); replaceMode++)
				replaceLevelIndicators.get(replaceMode).state = replaceMode == te.replaceMode ? State.ON : State.OFF;
			skipMissingIndicator.state = te.skipMissing ? State.ON : State.OFF;
			skipTilesIndicator.state = !te.replaceTileEntities ? State.ON : State.OFF;
		}

		playIndicator.state = State.OFF;
		pauseIndicator.state = State.OFF;
		resetIndicator.state = State.OFF;

		switch (te.state) {
			case PAUSED:
				pauseIndicator.state = State.YELLOW;
				playButton.active = true;
				pauseButton.active = false;
				resetButton.active = true;
				break;
			case RUNNING:
				playIndicator.state = State.GREEN;
				playButton.active = false;
				pauseButton.active = true;
				resetButton.active = true;
				break;
			case STOPPED:
				resetIndicator.state = State.RED;
				playButton.active = true;
				pauseButton.active = false;
				resetButton.active = false;
				break;
			default:
				break;
		}

		handleTooltips();

		super.tick();
	}

	protected void handleTooltips() {
		if (placementSettingsHidden())
			return;

		for (Widget w : placementSettingWidgets)
			if (w instanceof IconButton) {
				IconButton button = (IconButton) w;
				if (!button.getToolTip()
					.isEmpty()) {
					button.setToolTip(button.getToolTip()
						.get(0));
					button.getToolTip()
						.add(TooltipHelper.holdShift(Palette.Blue, hasShiftDown()));
				}
			}

		if (hasShiftDown()) {
			fillToolTip(skipMissingButton, skipMissingIndicator, "skipMissing");
			fillToolTip(skipTilesButton, skipTilesIndicator, "skipTileEntities");
			fillToolTip(replaceLevelButtons.get(0), replaceLevelIndicators.get(0), "dontReplaceSolid");
			fillToolTip(replaceLevelButtons.get(1), replaceLevelIndicators.get(1), "replaceWithSolid");
			fillToolTip(replaceLevelButtons.get(2), replaceLevelIndicators.get(2), "replaceWithAny");
			fillToolTip(replaceLevelButtons.get(3), replaceLevelIndicators.get(3), "replaceWithEmpty");
		}
	}

	private void fillToolTip(IconButton button, Indicator indicator, String tooltipKey) {
		if (!button.isHovered())
			return;
		boolean enabled = indicator.state == State.ON;
		List<ITextComponent> tip = button.getToolTip();
		tip.add((enabled ? optionEnabled : optionDisabled).plainCopy().withStyle(BLUE));
		tip.addAll(TooltipHelper.cutTextComponent(Lang.translate("gui.schematicannon.option." + tooltipKey + ".description"),
			GRAY, GRAY));
	}

	@Override
	protected void renderWindow(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		AllGuiTextures.PLAYER_INVENTORY.draw(matrixStack, this, leftPos - 10, topPos + 145);
		BG_TOP.draw(matrixStack, this, leftPos + 20, topPos);
		BG_BOTTOM.draw(matrixStack, this, leftPos + 20, topPos + BG_TOP.height);

		SchematicannonTileEntity te = menu.getTileEntity();
		renderPrintingProgress(matrixStack, te.schematicProgress);
		renderFuelBar(matrixStack, te.fuelLevel);
		renderChecklistPrinterProgress(matrixStack, te.bookPrintingProgress);

		if (!te.inventory.getStackInSlot(0)
			.isEmpty())
			renderBlueprintHighlight(matrixStack);

		GuiGameElement.of(renderedItem)
			.<GuiGameElement.GuiRenderBuilder>at(leftPos + 230, topPos + 110, -200)
			.scale(5)
			.render(matrixStack);

		font.drawShadow(matrixStack, title, leftPos + 80, topPos + 3, 0xfefefe);

		ITextComponent msg = Lang.translate("schematicannon.status." + te.statusMsg);
		int stringWidth = font.width(msg);

		if (te.missingItem != null) {
			stringWidth += 15;
			GuiGameElement.of(te.missingItem)
				.<GuiGameElement.GuiRenderBuilder>at(leftPos + 150, topPos + 46, 100)
				.scale(1)
				.render(matrixStack);
		}

		font.drawShadow(matrixStack, msg, leftPos + 20 + 102 - stringWidth / 2, topPos + 50, 0xCCDDFF);
		font.draw(matrixStack, inventory.getDisplayName(), leftPos - 10 + 7, topPos + 145 + 6, 0x666666);

		// to see or debug the bounds of the extra area uncomment the following lines
		// Rectangle2d r = extraAreas.get(0);
		// fill(r.getX() + r.getWidth(), r.getY() + r.getHeight(), r.getX(), r.getY(),
		// 0xd3d3d3d3);
	}

	protected void renderBlueprintHighlight(MatrixStack matrixStack) {
		AllGuiTextures.SCHEMATICANNON_HIGHLIGHT.draw(matrixStack, this, leftPos + 20 + 10, topPos + 60);
	}

	protected void renderPrintingProgress(MatrixStack matrixStack, float progress) {
		progress = Math.min(progress, 1);
		AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_PROGRESS;
		minecraft.getTextureManager()
			.bind(sprite.location);
		blit(matrixStack, leftPos + 20 + 44, topPos + 64, sprite.startX, sprite.startY, (int) (sprite.width * progress),
			sprite.height);
	}

	protected void renderChecklistPrinterProgress(MatrixStack matrixStack, float progress) {
		AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_CHECKLIST_PROGRESS;
		minecraft.getTextureManager()
			.bind(sprite.location);
		blit(matrixStack, leftPos + 20 + 154, topPos + 20, sprite.startX, sprite.startY, (int) (sprite.width * progress),
			sprite.height);
	}

	protected void renderFuelBar(MatrixStack matrixStack, float amount) {
		AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_FUEL;
		if (menu.getTileEntity().hasCreativeCrate) {
			AllGuiTextures.SCHEMATICANNON_FUEL_CREATIVE.draw(matrixStack, this, leftPos + 20 + 36, topPos + 19);
			return;
		}
		minecraft.getTextureManager()
			.bind(sprite.location);
		blit(matrixStack, leftPos + 20 + 36, topPos + 19, sprite.startX, sprite.startY, (int) (sprite.width * amount),
			sprite.height);
	}

	@Override
	protected void renderWindowForeground(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		SchematicannonTileEntity te = menu.getTileEntity();

		int fuelX = leftPos + 20 + 36, fuelY = topPos + 19;
		if (mouseX >= fuelX && mouseY >= fuelY && mouseX <= fuelX + AllGuiTextures.SCHEMATICANNON_FUEL.width
			&& mouseY <= fuelY + AllGuiTextures.SCHEMATICANNON_FUEL.height) {
			List<ITextComponent> tooltip = getFuelLevelTooltip(te);
			renderComponentTooltip(matrixStack, tooltip, mouseX, mouseY);
		}

		if (hoveredSlot != null && !hoveredSlot.hasItem()) {
			if (hoveredSlot.index == 0)
				renderComponentTooltip(matrixStack,
					TooltipHelper.cutTextComponent(Lang.translate(_slotSchematic), GRAY, TextFormatting.BLUE),
					mouseX, mouseY);
			if (hoveredSlot.index == 2)
				renderComponentTooltip(matrixStack,
					TooltipHelper.cutTextComponent(Lang.translate(_slotListPrinter), GRAY, TextFormatting.BLUE),
					mouseX, mouseY);
			if (hoveredSlot.index == 4)
				renderComponentTooltip(matrixStack,
					TooltipHelper.cutTextComponent(Lang.translate(_slotGunpowder), GRAY, TextFormatting.BLUE),
					mouseX, mouseY);
		}

		if (te.missingItem != null) {
			int missingBlockX = leftPos + 150, missingBlockY = topPos + 46;
			if (mouseX >= missingBlockX && mouseY >= missingBlockY && mouseX <= missingBlockX + 16
				&& mouseY <= missingBlockY + 16) {
				renderTooltip(matrixStack, te.missingItem, mouseX, mouseY);
			}
		}

		int paperX = leftPos + 132, paperY = topPos + 19;
		if (mouseX >= paperX && mouseY >= paperY && mouseX <= paperX + 16 && mouseY <= paperY + 16)
			renderTooltip(matrixStack, listPrinter, mouseX, mouseY);

		super.renderWindowForeground(matrixStack, mouseX, mouseY, partialTicks);
	}

	protected List<ITextComponent> getFuelLevelTooltip(SchematicannonTileEntity te) {
		double fuelUsageRate = te.getFuelUsageRate();
		int shotsLeft = (int) (te.fuelLevel / fuelUsageRate);
		int shotsLeftWithItems = (int) (shotsLeft + te.inventory.getStackInSlot(4)
			.getCount() * (te.getFuelAddedByGunPowder() / fuelUsageRate));
		List<ITextComponent> tooltip = new ArrayList<>();

		if (te.hasCreativeCrate) {
			tooltip.add(Lang.translate(_gunpowderLevel, "" + 100));
			tooltip.add(new StringTextComponent("(").append(new TranslationTextComponent(AllBlocks.CREATIVE_CRATE.get()
				.getDescriptionId())).append(")").withStyle(DARK_PURPLE));
			return tooltip;
		}

		int fillPercent = (int) (te.fuelLevel * 100);
		tooltip.add(Lang.translate(_gunpowderLevel, fillPercent));
		tooltip.add(Lang.translate(_shotsRemaining, new StringTextComponent(Integer.toString(shotsLeft)).withStyle(BLUE)).withStyle(GRAY));
		if (shotsLeftWithItems != shotsLeft)
			tooltip.add(Lang.translate(_shotsRemainingWithBackup, new StringTextComponent(Integer.toString(shotsLeftWithItems)).withStyle(BLUE)).withStyle(GRAY));

		return tooltip;
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (showSettingsButton.isHovered()) {
			showSettingsIndicator.state = placementSettingsHidden() ? State.GREEN : State.OFF;
			initPlacementSettings();
		}

		if (confirmButton.isHovered()) {
			Minecraft.getInstance().player.closeContainer();
			return true;
		}

		if (!placementSettingsHidden()) {
			for (int replaceMode = 0; replaceMode < replaceLevelButtons.size(); replaceMode++) {
				if (!replaceLevelButtons.get(replaceMode)
					.isHovered())
					continue;
				if (menu.getTileEntity().replaceMode == replaceMode)
					continue;
				sendOptionUpdate(Option.values()[replaceMode], true);
			}
			if (skipMissingButton.isHovered())
				sendOptionUpdate(Option.SKIP_MISSING, !menu.getTileEntity().skipMissing);
			if (skipTilesButton.isHovered())
				sendOptionUpdate(Option.SKIP_TILES, !menu.getTileEntity().replaceTileEntities);
		}

		if (playButton.isHovered() && playButton.active)
			sendOptionUpdate(Option.PLAY, true);
		if (pauseButton.isHovered() && pauseButton.active)
			sendOptionUpdate(Option.PAUSE, true);
		if (resetButton.isHovered() && resetButton.active)
			sendOptionUpdate(Option.STOP, true);

		return super.mouseClicked(x, y, button);
	}

	@Override
	public List<Rectangle2d> getExtraAreas() {
		return extraAreas;
	}

	protected void sendOptionUpdate(Option option, boolean set) {
		AllPackets.channel.sendToServer(new ConfigureSchematicannonPacket(option, set));
	}

}

