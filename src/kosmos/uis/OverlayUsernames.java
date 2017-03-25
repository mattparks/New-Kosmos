package kosmos.uis;

import flounder.camera.*;
import flounder.devices.*;
import flounder.entities.*;
import flounder.fonts.*;
import flounder.guis.*;
import flounder.helpers.*;
import flounder.maths.*;
import flounder.maths.vectors.*;
import flounder.networking.*;
import flounder.resources.*;
import flounder.textures.*;
import flounder.visual.*;
import kosmos.camera.*;
import kosmos.world.*;

import java.util.*;

public class OverlayUsernames extends ScreenObject {
	private Vector3f screenPosition;

	private Pair<TextObject, GuiObject> playerUsername;
	private List<Pair<TextObject, GuiObject>> multiplayerNames;

	public OverlayUsernames(ScreenObject parent) {
		super(parent, new Vector2f(0.5f, 0.5f), new Vector2f(1.0f, 1.0f));
		super.setInScreenCoords(false);

		this.screenPosition = new Vector3f();

		this.playerUsername = generate(FlounderNetwork.getUsername());
		this.multiplayerNames = new ArrayList<>();
	}

	@Override
	public void updateObject() {
		updateUsername(playerUsername, KosmosWorld.getEntityPlayer());

		for (Pair<TextObject, GuiObject> object : multiplayerNames) {
			updateUsername(object, KosmosWorld.getPlayer(object.getFirst().getTextString()));
		}
	}

	public void addMultiplayer(String username) {
		multiplayerNames.add(generate(username));
	}

	public void removeMultiplayer(String username) {
		Iterator<Pair<TextObject, GuiObject>> iterator = multiplayerNames.iterator();

		while (iterator.hasNext()) {
			Pair<TextObject, GuiObject> object = iterator.next();

			if (object.getFirst().getTextString().equals(username)) {
				object.getFirst().delete();
				object.getSecond().delete();
				iterator.remove();
				return;
			}
		}
	}

	private Pair<TextObject, GuiObject> generate(String username) {
		TextObject text = new TextObject(this, new Vector2f(0.5f, 0.5f), username, 1.0f, FlounderFonts.CANDARA, 0.2f, GuiAlign.CENTRE);
		text.setInScreenCoords(false);
		text.setColour(new Colour(1.0f, 1.0f, 1.0f));
		text.setBorderColour(new Colour(0.0f, 0.0f, 0.0f));
		text.setBorder(new ConstantDriver(0.175f));
		text.setAlphaDriver(new ConstantDriver(0.75f));

		GuiObject gui = new GuiObject(this, this.getPosition(), new Vector2f(), TextureFactory.newBuilder().setFile(new MyFile(FlounderGuis.GUIS_LOC, "username.png")).create(), 1);
		gui.setInScreenCoords(false);
		gui.setColourOffset(new Colour());

		return new Pair<>(text, gui);
	}

	private void updateUsername(Pair<TextObject, GuiObject> pair, Entity entity) {
		// Get 2D label space.
		screenPosition.set(entity.getPosition());
		screenPosition.y += KosmosPlayer.PLAYER_OFFSET_Y;
		float distance = Vector3f.getDistance(screenPosition, FlounderCamera.getCamera().getPosition());
		Maths.worldToScreenSpace(screenPosition, FlounderCamera.getCamera().getViewMatrix(), FlounderCamera.getCamera().getProjectionMatrix(), this.screenPosition);
		// FlounderLogger.log(screenPosition.x + ", " + screenPosition.y);

		pair.getFirst().setVisible(distance < 50.0f);
		pair.getSecond().setVisible(distance < 50.0f);

		// Updates the text positioning.
		pair.getFirst().getPosition().set(screenPosition.x + (FlounderDisplay.getAspectRatio() / 2.0f), -screenPosition.y);

		// Update background size.
		pair.getSecond().getDimensions().set(pair.getFirst().getMeshSize());
		pair.getSecond().getDimensions().y = 0.5f * (float) pair.getFirst().getFont().getMaxSizeY();
		Vector2f.multiply(pair.getFirst().getDimensions(), pair.getSecond().getDimensions(), pair.getSecond().getDimensions());
		pair.getSecond().getDimensions().scale(2.0f * pair.getFirst().getScale());
		pair.getSecond().getPositionOffsets().set(pair.getFirst().getPositionOffsets());
		pair.getSecond().getPosition().set(pair.getFirst().getPosition());
	}

	@Override
	public void deleteObject() {
	}
}
