/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos.camera;

import flounder.camera.*;
import flounder.maths.*;
import flounder.maths.vectors.*;
import flounder.networking.*;
import kosmos.chunks.*;
import kosmos.network.packets.*;

public class PlayerBasic extends Player {
	public static final float PLAYER_OFFSET_Y = (float) (Math.sqrt(2.0) * 0.25);

	public static final float RUN_SPEED = 5.0f;
	public static final float BOOST_SPEED = 10.0f;
	public static final float JUMP_POWER = 5.5f;
	public static final float TURN_SPEED = 300.0f;

	private Vector3f position;
	private Vector3f rotation;

	private Timer timer;
	private boolean needSendData;

	public PlayerBasic() {
		super();
	}

	@Override
	public void init() {
		this.position = new Vector3f();
		this.rotation = new Vector3f();

		this.timer = new Timer(1.0 / 20.0); // 20 ticks per second.
		this.needSendData = true;
	}

	@Override
	public void update() {
		// Gets the current position and deltas.
		Vector3f newPosition = KosmosChunks.getEntityPlayer().getPosition();
		Vector3f newRotation = KosmosChunks.getEntityPlayer().getRotation();
		float dx = newPosition.x - position.x;
		float dy = newPosition.y - position.y;
		float dz = newPosition.z - position.z;
		float ry = newRotation.y - rotation.y;

		// Try to send data to the server if needed.
		if (dx != 0.0f || dy != 0.0f || dz != 0.0f || ry != 0.0f) {
			if (!needSendData) {
				needSendData = true;
				timer.resetStartTime();
			}
		}

		// Stop spam requests by using a tick rate.
		if (needSendData && timer.isPassedTime()) {
			sendData();
		}

		// Sets the current player position to the current entity.
		this.position.set(newPosition);
		this.rotation.set(newRotation);
	}

	/**
	 * Sends this players data to the server.
	 */
	public void sendData() {
		if (FlounderNetwork.getUsername() != null && FlounderNetwork.getSocketClient() != null && KosmosChunks.getCurrent() != null) {
			new PacketMove(FlounderNetwork.getUsername(), position, rotation, KosmosChunks.getCurrent().getPosition().x, KosmosChunks.getCurrent().getPosition().z).writeData(FlounderNetwork.getSocketClient());
			needSendData = false;
			timer.resetStartTime();
		}
	}

	@Override
	public Vector3f getPosition() {
		return position;
	}

	@Override
	public Vector3f getRotation() {
		return rotation;
	}

	@Override
	public boolean isActive() {
		return true;
	}
}
