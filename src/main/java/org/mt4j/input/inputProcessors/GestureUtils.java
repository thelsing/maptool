package org.mt4j.input.inputProcessors;

import org.mt4j.input.inputData.InputCursor;

public class GestureUtils {
	
	/**
	 * Checks if the distance between a reference cursor and a cursor is greater than the distance to another cursor.
	 *
	 * @param reference the reference
	 * @param oldCursor the old cursor
	 * @param newCursor the new cursor
	 * @return true, if is cursor distance greater
	 */
	public static boolean isCursorDistanceGreater(InputCursor reference, InputCursor oldCursor, InputCursor newCursor){
//		float distanceToOldCursor = reference.getPosition().distance2D(oldCursor.getPosition());
//		float distanceToNewCursor = reference.getPosition().distance2D(newCursor.getPosition());
//		return distanceToNewCursor > distanceToOldCursor;
		return getDistance(reference, newCursor) > getDistance(reference, oldCursor);
	}
	
	/**
	 * Gets the distance between two cursors.
	 *
	 * @param a the a
	 * @param b the b
	 * @return the distance
	 */
	public static float getDistance(InputCursor a, InputCursor b){
		return a.getPosition().distance2D(b.getPosition());
	}

}
