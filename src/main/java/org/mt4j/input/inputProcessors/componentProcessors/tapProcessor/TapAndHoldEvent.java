/***********************************************************************
 * mt4j Copyright (c) 2008 - 2009 Christopher Ruff, Fraunhofer-Gesellschaft All rights reserved.
 *  
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************************************************/
package org.mt4j.input.inputProcessors.componentProcessors.tapProcessor;

import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IInputProcessor;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.util.math.Vector3D;

import java.awt.*;

/**
 * The Class TapAndHoldEvent.
 * 
 * @author Christopher Ruff
 */
public class TapAndHoldEvent extends MTGestureEvent {
	
	/** The cursor. */
	private InputCursor cursor;

	/** The click point. */
	private Vector3D clickPoint;

	private int holdTime;
	
	private float elapsedTime;
	
	/**
	 * Instantiates a new tap and hold event.
	 * 
	 * @param source the source
	 * @param id the id
	 * @param targetComponent the target component
	 * @param m the m
	 * @param clickPoint the click point
	 */
	public TapAndHoldEvent(IInputProcessor source, int id, Component targetComponent, InputCursor m, Vector3D clickPoint, int holdTime, float elapsedTime) {
		super(source, id, targetComponent);
		this.cursor = m;
		this.clickPoint = clickPoint;
		this.holdTime = holdTime;
		this.elapsedTime = elapsedTime;
	}
	
	
	
	/**
	 * Gets the total time required to hold for a successfully completed gesture.
	 * 
	 * @return the hold time
	 */
	public int getHoldTime() {
		return holdTime;
	}



	/**
	 * Checks if the tap and hold gesture was completed successfully or if it was aborted/not finished yet.
	 * 
	 * @return true, if is hold complete
	 */
	public boolean isHoldComplete() {

		return elapsedTime >= holdTime
				&& (getId() == MTGestureEvent.GESTURE_ENDED || getId() == MTGestureEvent.GESTURE_UPDATED);
	}


	/**
	 * Gets the elapsed holding time in milliseconds.
	 * 
	 * @return the elapsed time
	 */
	public float getElapsedTime() {
		return elapsedTime;
	}



	/**
	 * Gets the elapsed time normalized from 0..1.
	 * Clamps the value to 1.0 if it would be slightly higher.
	 * 
	 * @return the elapsed time normalized
	 */
	public float getElapsedTimeNormalized() {
		return  (float)elapsedTime / (float)holdTime;
	}


	/**
	 * Gets the click point.
	 *
	 * @return the click point
	 */
	public Vector3D getLocationOnScreen() {
		return clickPoint;
	}

	public Point getLocationOn(Component component)
	{
		Point cpos = component.getLocationOnScreen();
		Point p = new Point();
		p.x = (int)clickPoint.x - cpos.x;
		p.y = (int)clickPoint.y - cpos.y;
		return p;
	}

	/**
	 * Gets the cursor.
	 *
	 * @return the cursor
	 */
	public InputCursor getCursor() {
		return cursor;
	}

}
