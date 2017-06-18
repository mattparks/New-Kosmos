/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved.
 *
 * This source file is part of New Kosmos.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package kosmos.world.chunks.biomes;

import flounder.maths.*;
import flounder.resources.*;
import flounder.textures.*;
import kosmos.world.chunks.*;

public class BiomeTemperateDesert extends IBiome {
	private static final EntitySpawn[] SPAWNS = new EntitySpawn[]{
	};
	private static final TextureObject TEXTURE = TextureFactory.newBuilder().setFile(new MyFile(KosmosChunks.TERRAINS_FOLDER, "temperateDesert.png")).clampEdges().create();
	private static final Colour COLOUR = new Colour(0.8941f, 0.9098f, 0.7922f);

	public BiomeTemperateDesert() {
		super();
	}

	@Override
	public String getBiomeName() {
		return "temperateDesert";
	}

	@Override
	public EntitySpawn[] getEntitySpawns() {
		return SPAWNS;
	}

	@Override
	public TextureObject getTexture() {
		return TEXTURE;
	}

	@Override
	public Colour getColour() {
		return COLOUR;
	}
}