/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos.chunks;

import flounder.camera.*;
import flounder.entities.*;
import flounder.entities.components.*;
import flounder.events.*;
import flounder.framework.*;
import flounder.guis.*;
import flounder.helpers.*;
import flounder.inputs.*;
import flounder.logger.*;
import flounder.maths.*;
import flounder.maths.Timer;
import flounder.maths.vectors.*;
import flounder.models.*;
import flounder.physics.*;
import flounder.profiling.*;
import flounder.resources.*;
import flounder.textures.*;
import kosmos.entities.components.*;
import kosmos.entities.instances.*;
import kosmos.world.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import static flounder.platform.Constants.*;

public class KosmosChunks extends Module {
	public static final MyFile TERRAINS_FOLDER = new MyFile(MyFile.RES_FOLDER, "terrains");

	// The size of the rendered map image.
	private static final int MAP_SIZE = 512;

	private Sphere chunkRange;

	private ModelObject modelHexagon;

	private Vector3f lastPlayerPos;
	private Chunk currentChunk;

	private TextureObject mapTexture;
	private Timer mapUpdate;

	public KosmosChunks() {
		super(FlounderEvents.class, FlounderEntities.class, FlounderModels.class, FlounderTextures.class);
	}

	@Handler.Function(Handler.FLAG_INIT)
	public void init() {
		this.chunkRange = new Sphere(40.0f); // 3.0f * Chunk.CHUNK_WORLD_SIZE

		this.modelHexagon = ModelFactory.newBuilder().setFile(new MyFile(MyFile.RES_FOLDER, "terrains", "hexagon.obj")).create();

		this.lastPlayerPos = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		this.currentChunk = null;

		this.mapTexture = null;
		this.mapUpdate = new Timer(2.0f);

		FlounderEvents.get().addEvent(new IEvent() {
			private MouseButton buttonRemove = new MouseButton(GLFW_MOUSE_BUTTON_RIGHT);

			@Override
			public boolean eventTriggered() {
				return buttonRemove.wasDown() && !FlounderGuis.get().getGuiMaster().isGamePaused();
			}

			@Override
			public void onEvent() {
				Ray cameraRay = FlounderCamera.get().getCamera().getViewRay();

				for (Entity entity : FlounderEntities.get().getEntities().getAll()) {
					if (entity.getCollider() != null && entity.getComponent(ComponentPlayer.class) == null && entity.getComponent(ComponentMultiplayer.class) == null && entity.getComponent(ComponentChunk.class) == null) {
						IntersectData data = entity.getCollider().intersects(cameraRay);
						float distance = Vector3f.getDistance(entity.getPosition(), KosmosWorld.get().getEntityPlayer().getPosition());

						if (data.isIntersection() && distance < 2.0f) {
							entity.forceRemove();
							return;
						}
					}
				}
			}
		});

		FlounderEvents.get().addEvent(new IEvent() {
			private final int RECURSION_COUNT = 256;
			private final float RAY_RANGE = 70.0f;

			private MouseButton buttonPlace = new MouseButton(GLFW_MOUSE_BUTTON_LEFT);

			@Override
			public boolean eventTriggered() {
				return buttonPlace.wasDown() && !FlounderGuis.get().getGuiMaster().isGamePaused();
			}

			@Override
			public void onEvent() {
				Ray cameraRay = FlounderCamera.get().getCamera().getViewRay();

				if (intersectionInRange(cameraRay, 0.0f, RAY_RANGE)) {
					Vector3f terrainPosition = binarySearch(cameraRay, 0, 0, RAY_RANGE);

					if (terrainPosition.getY() >= 0.0f) {
						Chunk inChunk = null;

						for (Entity entity : FlounderEntities.get().getEntities().getAll()) {
							if (entity != null && entity instanceof Chunk) {
								Chunk chunk = (Chunk) entity;

								if (chunk.getSphere().contains(new Vector3f(terrainPosition.x, 0.0f, terrainPosition.z))) {
									inChunk = chunk;
								}
							}
						}

						if (inChunk == null) {
							FlounderLogger.get().error("Could not find chunk for terrain position: " + terrainPosition);
							return;
						}

						Vector2f tilePosition = Chunk.convertWorldToTile(inChunk, terrainPosition);
						tilePosition.x = Math.round(tilePosition.x);
						tilePosition.y = Math.round(tilePosition.y);
						Vector3f roundedPosition = Chunk.convertTileToWorld(inChunk, tilePosition.x, tilePosition.y);
						roundedPosition.y = Chunk.getWorldHeight(roundedPosition.x, roundedPosition.z);

						Entity entity = new InstanceBush(FlounderEntities.get().getEntities(),
								new Vector3f(
										roundedPosition.x,
										0.5f + roundedPosition.y * 0.5f,
										roundedPosition.z
								),
								new Vector3f()
						);
						new ComponentChild(entity, inChunk);
					}
				}
			}

			private Vector3f getPointOnRay(Ray cameraRay, float distance) {
				Vector3f camPos = FlounderCamera.get().getCamera().getPosition();
				Vector3f start = new Vector3f(camPos.x, camPos.y, camPos.z);
				Vector3f scaledRay = new Vector3f(cameraRay.getCurrentRay().x * distance, cameraRay.getCurrentRay().y * distance, cameraRay.getCurrentRay().z * distance);
				return Vector3f.add(start, scaledRay, null);
			}

			private Vector3f binarySearch(Ray cameraRay, int count, float start, float finish) {
				float half = start + ((finish - start) / 2.0f);

				if (count >= RECURSION_COUNT) {
					return getPointOnRay(cameraRay, half);
				}

				if (intersectionInRange(cameraRay, start, half)) {
					return binarySearch(cameraRay, count + 1, start, half);
				} else {
					return binarySearch(cameraRay, count + 1, half, finish);
				}
			}

			private boolean intersectionInRange(Ray cameraRay, float start, float finish) {
				Vector3f startPoint = getPointOnRay(cameraRay, start);
				Vector3f endPoint = getPointOnRay(cameraRay, finish);

				if (!isUnderGround(startPoint) && isUnderGround(endPoint)) {
					return true;
				} else {
					return false;
				}
			}

			private boolean isUnderGround(Vector3f testPoint) {
				float height = Chunk.getWorldHeight(testPoint.getX(), testPoint.getZ());

				if (height < 0.0f || testPoint.y < height) {
					return true;
				} else {
					return false;
				}
			}
		});
	}

	@Handler.Function(Handler.FLAG_UPDATE_PRE)
	public void update() {
		if (FlounderCamera.get().getPlayer() != null) {
			Vector3f playerPos = new Vector3f(FlounderCamera.get().getPlayer().getPosition());
			playerPos.y = 0.0f;

			Chunk playerChunk = null;

			if (!playerPos.equals(lastPlayerPos)) {
				chunkRange.update(playerPos, null, 1.0f, chunkRange);
			}

			// Goes though all chunks looking for changes.
			for (Entity entity : new ArrayList<>(FlounderEntities.get().getEntities().getAll())) {
				if (entity != null && entity instanceof Chunk) {
					Chunk chunk = (Chunk) entity;

					// Checks if the player position is in this chunk.
					if (chunk.isLoaded() && chunk.getSphere() != null && chunk.getSphere().contains(playerPos)) {
						// This chunk is now the chunk with the player in it.
						playerChunk = chunk;
					}

					// Updates the chunk.
					chunk.update();
				}
			}

			// This chunk is now the current chunk.
			setCurrent(playerChunk);

			// Updates the last player position value.
			lastPlayerPos.set(playerPos);
		}

		// Renders the chunks range.
		// FlounderBounding.addShapeRender(chunkRange);
	}

	@Handler.Function(Handler.FLAG_PROFILE)
	public void profile() {
		//	FlounderProfiler.get().add(getTab(), "Chunks Size", chunks.getSize());
		FlounderProfiler.get().add(getTab(), "Chunks Current", currentChunk);
	}

	/**
	 * Generates a map for the current seed.
	 */
	private void generateMap() {
		int seed = KosmosWorld.get().getNoise().getSeed();
		BufferedImage imageOutput = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB);

		FlounderLogger.get().log("Generating map for seed: " + seed);

		for (int y = 0; y < MAP_SIZE; y++) {
			for (int x = 0; x < MAP_SIZE; x++) {
				Colour mapColour = Chunk.getWorldBiome((x / ((float) MAP_SIZE / (float) Chunk.WORLD_SIZE)) - ((float) Chunk.WORLD_SIZE / 2.0f), (y / ((float) MAP_SIZE / (float) Chunk.WORLD_SIZE)) - ((float) Chunk.WORLD_SIZE / 2.0f)).getBiome().getColour();
				// float typeHeight = 0.1f + Chunk.getWorldHeight(x - (Chunk.WORLD_SIZE / 2.0f), y - (Chunk.WORLD_SIZE / 2.0f)) / 10.0f;

				int rgb = (int) (255.0f * mapColour.r);
				rgb = (rgb << 8) + ((int) (255.0f * mapColour.g));
				rgb = (rgb << 8) + ((int) (255.0f * mapColour.b));
				imageOutput.setRGB(x, y, rgb);
			}
		}

		File fileOutput = new File(Framework.getRoamingFolder().getPath() + "/saves/map-" + seed + ".png");
		MyFile fileSeed = new MyFile(Framework.getRoamingFolder(), "saves", "map-" + seed + ".png");

		try {
			// Save the map texture..
			ImageIO.write(imageOutput, "png", fileOutput);

			// Load the map texture.
			mapTexture = TextureFactory.newBuilder().setFile(fileSeed).create();
		} catch (IOException e) {
			FlounderLogger.get().error("Could not save map image to file: " + fileOutput);
			FlounderLogger.get().exception(e);
		}
	}

	/**
	 * Gets the default hexagon model.
	 *
	 * @return The hexagon model.
	 */
	public ModelObject getModelHexagon() {
		return this.modelHexagon;
	}

	public Chunk getCurrent() {
		return this.currentChunk;
	}

	/**
	 * Sets the current chunk that that player is contained in. This will generate surrounding chunks.
	 *
	 * @param currentChunk The chunk to be set as the current.
	 */
	public void setCurrent(Chunk currentChunk) {
		if (currentChunk != null && this.currentChunk != currentChunk) {
			// Creates the children chunks for the new current chunk.
			currentChunk.createChunksAround(new Single<>(2)); // TODO: Make work?

			// Removes any old chunks that are out of range.
			Iterator<Entity> it = FlounderEntities.get().getEntities().getAll().iterator();

			while (it.hasNext()) {
				Entity entity = it.next();

				if (entity != null && entity instanceof Chunk) {
					Chunk chunk = (Chunk) entity;

					if (chunk != currentChunk && chunk.isLoaded()) {
						if (!chunk.getSphere().intersects(this.chunkRange).isIntersection() && !this.chunkRange.contains(chunk.getSphere())) {
							chunk.delete();
							it.remove();
						}
					}
				}
			}

			// The current instance chunk is what was calculated for in this function.
			this.currentChunk = currentChunk;
		}
	}

	/**
	 * Clears all chunks, then creates a current at the previous chunks position.
	 *
	 * @param loadCurrent If the current chunk will be replaced.
	 */
	public void clear(boolean loadCurrent) {
		// Removes any chunks in the entity list.
		Iterator<Entity> it = FlounderEntities.get().getEntities().getAll().iterator();

		while (it.hasNext()) {
			Entity entity = it.next();

			if (entity != null && entity instanceof Chunk) {
				Chunk chunk = (Chunk) entity;
				chunk.delete();
				it.remove();
			}
		}

		// Sets up the new root chunk.
		if (loadCurrent && currentChunk != null) {
			setCurrent(new Chunk(FlounderEntities.get().getEntities(), currentChunk.getPosition()));
		}
	}

	public TextureObject getMapTexture() {
		if (mapUpdate.isPassedTime() && KosmosWorld.get().getNoise().getSeed() != -1) {
			if (mapTexture == null) {
				generateMap();
			} else if (mapTexture.getFile() != null) {
				String textureSeed = mapTexture.getFile().getName().replace(".png", "").replace("map-", "").trim();
				int lastSeed = Integer.parseInt(textureSeed);

				if (lastSeed != KosmosWorld.get().getNoise().getSeed()) {
					generateMap();
				}
			}

			mapUpdate.resetStartTime();
		}

		return mapTexture;
	}

	@Handler.Function(Handler.FLAG_DISPOSE)
	public void dispose() {
		clear(false);
	}

	@Module.Instance
	public static KosmosChunks get() {
		return (KosmosChunks) Framework.getInstance(KosmosChunks.class);
	}

	@Module.TabName
	public static String getTab() {
		return "Kosmos Chunks";
	}
}
