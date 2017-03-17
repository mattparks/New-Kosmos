/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos.entities.components;

import flounder.entities.*;
import flounder.entities.components.*;
import flounder.helpers.*;
import flounder.maths.vectors.*;
import flounder.physics.*;

import javax.swing.*;

/**
 * Component that detects collision between two engine.entities.
 * <p>
 * Note: this component requires that both engine.entities have a ComponentCollider. Should one entity not have a ComponentCollider, then no collisions will be detected, because there is no collider to detect collisions against.
 */
public class ComponentCollision extends IComponentEntity implements IComponentMove, IComponentEditor {
	public static final int ID = EntityIDAssigner.getId();

	/**
	 * Creates a new ComponentCollision.
	 *
	 * @param entity The entity this component is attached to.
	 */
	public ComponentCollision(Entity entity) {
		super(entity, ID);
	}

	@Override
	public void update() {
	}

	/**
	 * Resolves AABB collisions with any other CollisionComponents encountered.
	 *
	 * @param amount The amount attempting to be moved.
	 *
	 * @return New verifyMove vector that will not cause collisions after movement.
	 */
	public Vector3f resolveAABBCollisions(Vector3f amount) {
		Vector3f result = new Vector3f(amount.getX(), amount.getY(), amount.getZ());
		ComponentCollider collider1 = (ComponentCollider) getEntity().getComponent(ComponentCollider.ID);

		if (collider1 == null) {
			return result;
		}

		AABB aabb1 = collider1.getAABB();
		final AABB collisionRange = AABB.stretch(aabb1, null, amount); // The range in where there can be collisions!

		getEntity().visitInRange(ComponentCollision.ID, collisionRange, (Entity entity, IComponentEntity component) -> {
			if (entity.equals(getEntity())) {
				return;
			}

			ComponentCollider collider2 = (ComponentCollider) entity.getComponent(ComponentCollider.ID);

			if (collider2 == null) {
				return;
			}

			AABB aabb2 = collider2.getAABB();

			if (aabb2 != null && aabb2.intersects(collisionRange).isIntersection()) {
				result.set((float) resolveCollisionX(aabb1, aabb2, result.getX()), (float) resolveCollisionY(aabb1, aabb2, result.getY()), (float) resolveCollisionZ(aabb1, aabb2, result.getZ()));
			}
		});

		return result;
	}

	public double resolveCollisionX(AABB thisAABB, AABB other, double moveAmountX) {
		double newAmtX;

		if (moveAmountX == 0.0) {
			return moveAmountX;
		}

		if (moveAmountX > 0.0) { // This max == other min
			newAmtX = other.getMinExtents().getX() - thisAABB.getMaxExtents().getX();
		} else { // This min == other max
			newAmtX = other.getMaxExtents().getX() - thisAABB.getMinExtents().getX();
		}

		if (Math.abs(newAmtX) < Math.abs(moveAmountX)) {
			moveAmountX = newAmtX;
		}

		return moveAmountX;
	}

	public double resolveCollisionY(AABB thisAABB, AABB other, double moveAmountY) {
		double newAmtY;

		if (moveAmountY == 0.0) {
			return moveAmountY;
		}

		if (moveAmountY > 0.0) { // This max == other min.
			newAmtY = other.getMinExtents().getY() - thisAABB.getMaxExtents().getY();
		} else { // This min == other max.
			newAmtY = other.getMaxExtents().getY() - thisAABB.getMinExtents().getY();
		}

		if (Math.abs(newAmtY) < Math.abs(moveAmountY)) {
			moveAmountY = newAmtY;
		}

		return moveAmountY;
	}

	public double resolveCollisionZ(AABB thisAABB, AABB other, double moveAmountZ) {
		double newAmtZ;

		if (moveAmountZ == 0.0) {
			return moveAmountZ;
		}

		if (moveAmountZ > 0.0) { // This max == other min.
			newAmtZ = other.getMinExtents().getZ() - thisAABB.getMaxExtents().getZ();
		} else { // This min == other max.
			newAmtZ = other.getMaxExtents().getZ() - thisAABB.getMinExtents().getZ();
		}

		if (Math.abs(newAmtZ) < Math.abs(moveAmountZ)) {
			moveAmountZ = newAmtZ;
		}

		return moveAmountZ;
	}

	@Override
	public void verifyMove(Entity entity, Vector3f moveAmount, Vector3f rotateAmount) {
		moveAmount.set(resolveAABBCollisions(moveAmount));
		// rotateAmount = rotateAmount; // TODO: Stop some rotations?
	}

	@Override
	public void addToPanel(JPanel panel) {
	}

	@Override
	public void editorUpdate() {
	}

	@Override
	public Pair<String[], String[]> getSaveValues(String entityName) {
		return new Pair<>(
				new String[]{}, // Static variables
				new String[]{} // Class constructor
		);
	}

	@Override
	public void dispose() {
	}
}
