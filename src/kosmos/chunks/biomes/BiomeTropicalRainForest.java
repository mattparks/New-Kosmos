/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos.chunks.biomes;

import flounder.maths.*;
import flounder.particles.*;
import flounder.resources.*;
import flounder.textures.*;
import kosmos.chunks.*;
import kosmos.entities.instances.*;
import kosmos.materials.*;

public class BiomeTropicalRainForest extends IBiome {
	private static final EntitySpawn[] SPAWNS = new EntitySpawn[]{
			new EntitySpawn(InstanceTallGrass::new, 1.0f, 0.25f),
			new EntitySpawn(InstanceFlowerpatch1::new, 1.0f, 0.375f),
			new EntitySpawn(InstanceBush::new, 1.0f, 0.625f),
	};
	private static final TextureObject TEXTURE = TextureFactory.newBuilder().setFile(new MyFile(KosmosChunks.TERRAINS_FOLDER, "tropicalRainForest.png")).clampEdges().create();
	private static final Colour COLOUR = new Colour(13.0f, 120.0f, 7.0f, true);
	private static final ParticleType PARTICLE = new ParticleType("rain", TextureFactory.newBuilder().setFile(new MyFile(FlounderParticles.PARTICLES_FOLDER, "rainParticle.png")).setNumberOfRows(4).create(), 4.75f, 0.15f);

	public BiomeTropicalRainForest() {
		super();
	}

	@Override
	public String getBiomeName() {
		return "tropicalRainForest";
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

	@Override
	public ParticleType getWeatherParticle() {
		return PARTICLE;
	}

	@Override
	public IMaterial getMaterial() {
		return IMaterial.Materials.GRASS.getMaterial();
	}
}