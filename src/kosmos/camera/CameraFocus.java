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
import flounder.devices.*;
import flounder.framework.*;
import flounder.guis.*;
import flounder.logger.*;
import flounder.maths.*;
import flounder.maths.matrices.*;
import flounder.maths.vectors.*;
import flounder.physics.*;
import flounder.profiling.*;
import flounder.space.*;
import kosmos.*;

public class CameraFocus extends Camera {
	// Defines basic view frustum sizes.
	private static final float NEAR_PLANE = 0.1f;
	private static final float FAR_PLANE = 300.0f;
	private static final float FIELD_OF_VIEW = 45.0f;

	// Defines how snappy these camera functions will be.
	private static final float ZOOM_AGILITY = 20.0f;
	private static final float ROTATE_AGILITY = 20.0f;
	private static final float PITCH_AGILITY = 20.0f;

	// Defines the strength of motion from the mouse.
	private static final float INFLUENCE_OF_MOUSE_DY = 300.0f;
	private static final float INFLUENCE_OF_MOUSE_DX = INFLUENCE_OF_MOUSE_DY * 100.0f;
	private static final float INFLUENCE_OF_MOUSE_WHEEL = 0.05f;

	private static final float MAX_HORIZONTAL_CHANGE = 30.0f;
	private static final float MAX_VERTICAL_CHANGE = 30.0f;
	private static final float MAX_ZOOM_CHANGE = 0.5f;

	private static final float CAMERA_AIM_OFFSET = 2.0f;
	private static final float MAX_ANGLE_OF_ELEVATION = (float) Math.PI / 4.0f;
	private static final float MIN_ANGLE_OF_ELEVATION = 0.0f;
	private static final float PITCH_OFFSET = 0.0f;
	private static final float MINIMUM_ZOOM = 0.5f;
	private static final float MAXIMUM_ZOOM = 28.0f;
	private static final float NORMAL_ZOOM = 8.0f;

	private Vector3f position;
	private Vector3f rotation;

	private Frustum viewFrustum;
	private Ray viewRay;
	private Matrix4f viewMatrix;
	private Matrix4f projectionMatrix;

	private float angleOfElevation;
	private float angleAroundPlayer;

	private Vector3f targetPosition;
	private Vector3f targetRotation;
	private float targetZoom;
	private float targetElevation;
	private float targetRotationAngle;

	private float actualDistanceFromPoint;
	private float horizontalDistanceFromFocus;
	private float verticalDistanceFromFocus;

	private int reangleButton;

	public CameraFocus() {
		super(FlounderLogger.class, FlounderProfiler.class, FlounderJoysticks.class, FlounderKeyboard.class, FlounderMouse.class);
	}

	@Override
	public void init() {
		/*FlounderEvents.addEvent(new IEvent() {
			private KeyButton k = new KeyButton(GLFW.GLFW_KEY_KP_ADD);
			@Override public boolean eventTriggered() {return k.wasDown();}
			@Override public void onEvent() {FIELD_OF_VIEW += 2.0f; FlounderLogger.log("FOV: " + FIELD_OF_VIEW);}
		});
		FlounderEvents.addEvent(new IEvent() {
			private KeyButton k = new KeyButton(org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT);
			@Override public boolean eventTriggered() {return k.wasDown();}
			@Override public void onEvent() {FIELD_OF_VIEW -= 2.0f; FlounderLogger.log("FOV: " + FIELD_OF_VIEW);}
		});*/

		this.position = new Vector3f();
		this.rotation = new Vector3f();

		this.viewFrustum = new Frustum();
		this.viewRay = new Ray(true, new Vector2f());
		this.viewMatrix = new Matrix4f();
		this.projectionMatrix = new Matrix4f();

		this.angleOfElevation = (float) Math.PI / 8.0f;
		this.angleAroundPlayer = 0.0f;

		this.targetPosition = new Vector3f();
		this.targetRotation = new Vector3f();
		this.targetZoom = NORMAL_ZOOM;
		this.targetElevation = angleOfElevation;
		this.targetRotationAngle = angleAroundPlayer;

		this.actualDistanceFromPoint = targetZoom;
		this.horizontalDistanceFromFocus = 0.0f;
		this.verticalDistanceFromFocus = 0.0f;

		this.reangleButton = KosmosConfigs.CAMERA_REANGLE.setReference(() -> reangleButton).getInteger();
		// GLFW_MOUSE_BUTTON_LEFT   = GLFW_MOUSE_BUTTON_1,
		// GLFW_MOUSE_BUTTON_RIGHT  = GLFW_MOUSE_BUTTON_2,
		// GLFW_MOUSE_BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_3;

		calculateDistances();
	}

	@Override
	public float getNearPlane() {
		return NEAR_PLANE;
	}

	@Override
	public float getFarPlane() {
		return FAR_PLANE;
	}

	@Override
	public float getFOV() {
		return FIELD_OF_VIEW;
	}

	@Override
	public void update(Player player) {
		calculateHorizontalAngle();
		calculateVerticalAngle();
		calculateZoom();

		if (player != null) {
			this.targetPosition.set(player.getPosition());
			// TODO: Rotate if camera rotation is 90 degrees below player rotation (astroneer like player camera).
			//this.targetRotation.set(player.getRotation());
		}

		updateActualZoom();
		updateHorizontalAngle();
		updatePitchAngle();
		calculateDistances();
		calculatePosition();

		if (FlounderProfiler.isOpen()) {
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Angle Of Elevation", angleOfElevation);
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Rotation", rotation);
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Angle Around MainPlayer", angleAroundPlayer);
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Actual Distance From Point", actualDistanceFromPoint);
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Target Zoom", targetZoom);
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Target Elevation", targetElevation);
			FlounderProfiler.add(FlounderCamera.PROFILE_TAB_NAME, "Camera Target Rotation Angle", targetRotationAngle);
		}
	}

	private void calculateHorizontalAngle() {
		float angleChange = 0.0f;

		if (FlounderGuis.getGuiMaster() != null && !FlounderGuis.getGuiMaster().isGamePaused()) {
			if (FlounderMouse.getMouse(reangleButton)) {
				angleChange = -FlounderMouse.getDeltaX() * INFLUENCE_OF_MOUSE_DX;
			}
		}

		if (angleChange > MAX_HORIZONTAL_CHANGE) {
			angleChange = MAX_HORIZONTAL_CHANGE;
		} else if (angleChange < -MAX_HORIZONTAL_CHANGE) {
			angleChange = -MAX_HORIZONTAL_CHANGE;
		}

		targetRotationAngle -= angleChange;

		if (targetRotationAngle >= Maths.DEGREES_IN_HALF_CIRCLE) {
			targetRotationAngle -= Maths.DEGREES_IN_CIRCLE;
		} else if (targetRotationAngle <= -Maths.DEGREES_IN_HALF_CIRCLE) {
			targetRotationAngle += Maths.DEGREES_IN_CIRCLE;
		}
	}

	private void calculateVerticalAngle() {
		float angleChange = 0.0f;

		if (FlounderGuis.getGuiMaster() != null && !FlounderGuis.getGuiMaster().isGamePaused()) {
			if (FlounderMouse.getMouse(reangleButton)) {
				angleChange = FlounderMouse.getDeltaY() * INFLUENCE_OF_MOUSE_DY;
			}
		}

		if (angleChange > MAX_VERTICAL_CHANGE) {
			angleChange = MAX_VERTICAL_CHANGE;
		} else if (angleChange < -MAX_VERTICAL_CHANGE) {
			angleChange = -MAX_VERTICAL_CHANGE;
		}

		targetElevation -= angleChange;

		if (targetElevation >= MAX_ANGLE_OF_ELEVATION) {
			targetElevation = MAX_ANGLE_OF_ELEVATION;
		} else if (targetElevation <= MIN_ANGLE_OF_ELEVATION) {
			targetElevation = MIN_ANGLE_OF_ELEVATION;
		}
	}

	private void calculateZoom() {
		float zoomChange = 0.0f;

		if (FlounderGuis.getGuiMaster() != null && !FlounderGuis.getGuiMaster().isGamePaused()) {
			if (Math.abs(FlounderMouse.getDeltaWheel()) > 0.1f) {
				zoomChange = FlounderMouse.getDeltaWheel() * INFLUENCE_OF_MOUSE_WHEEL;
			}
		}

		if (zoomChange > MAX_VERTICAL_CHANGE) {
			zoomChange = MAX_VERTICAL_CHANGE;
		} else if (zoomChange < -MAX_VERTICAL_CHANGE) {
			zoomChange = -MAX_ZOOM_CHANGE;
		}

		targetZoom -= zoomChange;

		if (targetZoom < MINIMUM_ZOOM) {
			targetZoom = MINIMUM_ZOOM;
		} else if (targetZoom > MAXIMUM_ZOOM) {
			targetZoom = MAXIMUM_ZOOM;
		}
	}

	private void updateActualZoom() {
		float offset = targetZoom - actualDistanceFromPoint;
		float change = offset * Framework.getDelta() * ZOOM_AGILITY;
		actualDistanceFromPoint += change;
	}

	private void updateHorizontalAngle() {
		float offset = targetRotationAngle - angleAroundPlayer;

		if (Math.abs(offset) > Maths.DEGREES_IN_HALF_CIRCLE) {
			if (offset < 0) {
				offset = targetRotationAngle + Maths.DEGREES_IN_CIRCLE - angleAroundPlayer;
			} else {
				offset = targetRotationAngle - Maths.DEGREES_IN_CIRCLE - angleAroundPlayer;
			}
		}

		angleAroundPlayer += offset * Framework.getDelta() * ROTATE_AGILITY;

		if (angleAroundPlayer >= Maths.DEGREES_IN_HALF_CIRCLE) {
			angleAroundPlayer -= Maths.DEGREES_IN_CIRCLE;
		} else if (angleAroundPlayer <= -Maths.DEGREES_IN_HALF_CIRCLE) {
			angleAroundPlayer += Maths.DEGREES_IN_CIRCLE;
		}
	}

	private void updatePitchAngle() {
		float offset = targetElevation - angleOfElevation;

		if (Math.abs(offset) > Maths.DEGREES_IN_HALF_CIRCLE) {
			if (offset < 0) {
				offset = targetElevation + Maths.DEGREES_IN_CIRCLE - angleOfElevation;
			} else {
				offset = targetElevation - Maths.DEGREES_IN_CIRCLE - angleOfElevation;
			}
		}

		angleOfElevation += offset * Framework.getDelta() * PITCH_AGILITY;

		if (angleOfElevation >= Maths.DEGREES_IN_HALF_CIRCLE) {
			angleOfElevation -= Maths.DEGREES_IN_CIRCLE;
		} else if (angleOfElevation <= -Maths.DEGREES_IN_HALF_CIRCLE) {
			angleOfElevation += Maths.DEGREES_IN_CIRCLE;
		}
	}

	private void calculateDistances() {
		horizontalDistanceFromFocus = (float) (actualDistanceFromPoint * Math.cos(angleOfElevation));
		verticalDistanceFromFocus = (float) (actualDistanceFromPoint * Math.sin(angleOfElevation));
	}

	private void calculatePosition() {
		float theta = targetRotation.y + angleAroundPlayer;
		position.x = targetPosition.x - (float) (horizontalDistanceFromFocus * Math.sin(Math.toRadians(theta)));
		position.z = targetPosition.z - (float) (horizontalDistanceFromFocus * Math.cos(Math.toRadians(theta)));
		position.y = targetPosition.y + (verticalDistanceFromFocus + CAMERA_AIM_OFFSET);

		rotation.y = targetRotation.y + Maths.DEGREES_IN_HALF_CIRCLE + angleAroundPlayer;
		rotation.x = (float) Math.toDegrees(angleOfElevation) - PITCH_OFFSET;
	}

	private void updateViewMatrix() {
		viewMatrix.setIdentity();
		position.negate();
		Matrix4f.rotate(viewMatrix, Matrix4f.REUSABLE_VECTOR.set(1.0f, 0.0f, 0.0f), (float) Math.toRadians(rotation.x), viewMatrix);
		Matrix4f.rotate(viewMatrix, Matrix4f.REUSABLE_VECTOR.set(0.0f, 1.0f, 0.0f), (float) Math.toRadians(-rotation.y), viewMatrix);
		Matrix4f.rotate(viewMatrix, Matrix4f.REUSABLE_VECTOR.set(0.0f, 0.0f, 1.0f), (float) Math.toRadians(rotation.z), viewMatrix);
		Matrix4f.translate(viewMatrix, position, viewMatrix);
		position.negate();
	}

	private void updateProjectionMatrix() {
		Matrix4f.perspectiveMatrix(FIELD_OF_VIEW, FlounderDisplay.getAspectRatio(), NEAR_PLANE, FAR_PLANE, projectionMatrix);
	}

	@Override
	public Matrix4f getViewMatrix() {
		updateViewMatrix();
		return viewMatrix;
	}

	@Override
	public Frustum getViewFrustum() {
		viewFrustum.recalculateFrustum(getProjectionMatrix(), viewMatrix);
		return viewFrustum;
	}

	@Override
	public Ray getViewRay() {
		viewRay.recalculateRay(position);
		return viewRay;
	}

	@Override
	public Matrix4f getProjectionMatrix() {
		updateProjectionMatrix();
		return projectionMatrix;
	}

	@Override
	public void reflect(float waterHeight) {
		position.y -= 2.0f * (position.y - waterHeight);
		rotation.x = -rotation.x;
		updateViewMatrix();
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
	public void setRotation(Vector3f rotation) {
		this.rotation.set(rotation);
	}

	@Override
	public boolean isActive() {
		return true;
	}
}
