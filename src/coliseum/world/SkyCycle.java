package coliseum.world;

import flounder.framework.*;
import flounder.maths.*;
import flounder.visual.*;

public class SkyCycle {
	private static final Colour SKY_COLOUR_DAY = new Colour(0.0f, 0.498f, 1.0f);
	private static final Colour SKY_COLOUR_SUNRISE = new Colour(0.9921f, 0.490f, 0.004f);
	private static final Colour SKY_COLOUR_NIGHT = new Colour(0.01f, 0.01f, 0.01f);

	private float dayFactor;
	private Colour skyColour;
	private SinWaveDriver skyDriver;
	// private KeyFrameDriver skyDriver;

	public SkyCycle() {
		this.dayFactor = 0.0f;
		this.skyColour = new Colour(SKY_COLOUR_DAY);
		this.skyDriver = new SinWaveDriver(0.0f, 100.0f, 60.0f);
		// this.skyDriver = new KeyFrameDriver(new KeyFrame[]{ new KeyFrame(0.0f, 0), new KeyFrame(15.0f, 2), new KeyFrame(30.0f, 1), new KeyFrame(45.0f, 3) }, 60.0f);
	}

	public void update() {
		dayFactor = (float) (skyDriver.update(FlounderFramework.getDelta()) / 100.0);
		Colour.interpolate(SKY_COLOUR_DAY, SKY_COLOUR_NIGHT, dayFactor, skyColour);
	}

	public float getDayFactor() {
		return dayFactor;
	}

	public Colour getSkyColourDay() {
		return SKY_COLOUR_DAY;
	}

	public Colour getSkyColourNight() {
		return SKY_COLOUR_NIGHT;
	}

	public Colour getSkyColour() {
		return skyColour;
	}
}