/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package dev.uis;

import dev.uis.screens.*;
import flounder.devices.*;
import flounder.fonts.*;
import flounder.framework.*;
import flounder.guis.*;
import flounder.maths.vectors.*;
import flounder.resources.*;
import flounder.textures.*;
import flounder.visual.*;

import java.util.*;

public class MenuStart extends GuiComponent {
	private static final Vector2f PARALAX_START = new Vector2f(0.5f, 0.5f);
	private static final float PARALAX_PERIOD = 10.0f;

	private MasterMenu superMenu;
	private MasterSlider masterSlider;

	private ScreenAddons screenAddons;
	private ScreenMultiplayer screenMultiplayer;
	private ScreenOffline screenOffline;
	private ScreenSettings screenSettings;

	private LinearDriver paralaxDriver;
	private Vector2f paralaxPosition;
	private GuiTexture t1 = new GuiTexture(Texture.newTexture(new MyFile(MyFile.RES_FOLDER, "1.png")).create());
	private GuiTexture t2 = new GuiTexture(Texture.newTexture(new MyFile(MyFile.RES_FOLDER, "2.png")).create());
	private GuiTexture t3 = new GuiTexture(Texture.newTexture(new MyFile(MyFile.RES_FOLDER, "3.png")).create());
	private GuiTexture t4 = new GuiTexture(Texture.newTexture(new MyFile(MyFile.RES_FOLDER, "4.png")).create());

	public MenuStart(MasterMenu superMenu, MasterSlider masterSlider) {
		this.superMenu = superMenu;
		this.masterSlider = masterSlider;

		this.screenAddons = new ScreenAddons(masterSlider);
		this.screenMultiplayer = new ScreenMultiplayer(masterSlider);
		this.screenOffline = new ScreenOffline(masterSlider);
		this.screenSettings = new ScreenSettings(masterSlider);

		this.paralaxDriver = new LinearDriver(0.0f, 360.0f, PARALAX_PERIOD);
		this.paralaxPosition = new Vector2f();

		/*float currentY = 1.0f + MasterSlider.BUTTONS_Y_SEPARATION;
		createQuitButton(currentY -= MasterSlider.BUTTONS_Y_SEPARATION);
		currentY -= MasterSlider.BUTTONS_Y_SEPARATION * MasterSlider.BUTTONS_Y_SEPARATION;

		createPlayButton(currentY -= MasterSlider.BUTTONS_Y_SEPARATION);*/

		createOfflineButton(0.1f);
		createMultiplayerButton(0.2f);
		createSettingsButton(0.3f);
		createAddonsButton(0.4f);
		createQuitButton(0.5f);

		Text text0 = Text.newText("New Kosmos").textAlign(GuiAlign.CENTRE).setFontSize(3.0f).create();
		text0.setColour(MasterSlider.TEXT_COLOUR);
		addText(text0, 0.5f, 0.8f, 1.0f);

		Text text1 = Text.newText("Created with the Flounder Engine").textAlign(GuiAlign.CENTRE).setFontSize(1.2f).create();
		text1.setColour(MasterSlider.TEXT_COLOUR);
		addText(text1, 0.5f, 0.9f, 1.0f);

		Text text2 = Text.newText("Copyright (C) 2017, Equilibrium Games - All Rights Reserved").textAlign(GuiAlign.CENTRE).setFontSize(1.0f).create();
		text2.setColour(MasterSlider.TEXT_COLOUR);
		addText(text2, 0.5f, 0.95f, 1.0f);
	}

	private void createOfflineButton(float yPos) {
		GuiTextButton button = MasterSlider.createButton("Offline", yPos, this);
		button.addLeftListener(() -> masterSlider.setNewSecondaryScreen(screenOffline, true));
	}

	private void createMultiplayerButton(float yPos) {
		GuiTextButton button = MasterSlider.createButton("Multiplayer", yPos, this);
		button.addLeftListener(() -> masterSlider.setNewSecondaryScreen(screenMultiplayer, true));
	}

	private void createSettingsButton(float yPos) {
		GuiTextButton button = MasterSlider.createButton("Settings", yPos, this);
		button.addLeftListener(() -> masterSlider.setNewSecondaryScreen(screenSettings, true));
	}

	private void createAddonsButton(float yPos) {
		GuiTextButton button = MasterSlider.createButton("Addons", yPos, this);
		button.addLeftListener(() -> masterSlider.setNewSecondaryScreen(screenAddons, true));
	}

	private void createQuitButton(float yPos) {
		GuiTextButton button = MasterSlider.createButton("Quit", yPos, this);
		button.addLeftListener(FlounderFramework::requestClose);
	}

	@Override
	protected void updateSelf() {
		Vector2f.rotate(PARALAX_START, paralaxDriver.update(FlounderFramework.getDelta()), paralaxPosition);

		float extraWidth = 0.15f;
		float extraHeight = 0.15f;

		float moveX = (FlounderMouse.getPositionX() - 1.0f) + paralaxPosition.x;
		float moveY = (FlounderMouse.getPositionY() - 1.0f) + paralaxPosition.y;

		t1.setPosition(FlounderDisplay.getAspectRatio() / 2.0f, 0.5f, FlounderDisplay.getAspectRatio(), 1.0f);
		t1.update();

		t2.setPosition(
				(FlounderDisplay.getAspectRatio() - (extraWidth * 0.1f * -moveX)) / 2.0f,
				(1.0f - (extraHeight * 0.1f * moveY)) / 2.0f,
				FlounderDisplay.getAspectRatio() + extraWidth,
				1.0f + extraHeight
		);
		t2.update();

		t3.setPosition(
				(FlounderDisplay.getAspectRatio() - (extraWidth * 0.2f * moveX)) / 2.0f,
				(1.0f - (extraHeight * 0.2f * -moveY)) / 2.0f,
				FlounderDisplay.getAspectRatio() + extraWidth,
				1.0f + extraHeight
		);
		t3.update();

		t4.setPosition(
				(FlounderDisplay.getAspectRatio() - (extraWidth * 0.3f * moveX)) / 2.0f,
				(1.0f - (extraHeight * 0.3f * moveY)) / 2.0f,
				FlounderDisplay.getAspectRatio() + extraWidth,
				1.0f + extraHeight
		);
		t4.update();
	}

	@Override
	protected void getGuiTextures(List<GuiTexture> guiTextures) {
		//if (isShown()) {
		guiTextures.add(t1);
		guiTextures.add(t2);
		guiTextures.add(t3);
		guiTextures.add(t4);
		//}
	}
}