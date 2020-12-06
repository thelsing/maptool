/***********************************************************************
 * mt4j Copyright (c) 2008 - 2009, C.Ruff, Fraunhofer-Gesellschaft All rights reserved.
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
package org.mt4j.input.inputProcessors.componentProcessors.zoomProcessor;

import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IInputProcessor;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.util.math.Vector3D;

import java.awt.*;


/**
 * The Class ZoomEvent.
 * @author Christopher Ruff
 */
public class ZoomEvent extends MTGestureEvent {

	/** The first cursor. */
	private InputCursor firstCursor;
	
	/** The second cursor. */
	private InputCursor secondCursor;

	private Vector3D centerPoint;

	/** The cam zoom amount. */
	private float camZoomAmount;
	
	/**
	 * Instantiates a new zoom event.
	 * 
	 * @param source the source
	 * @param id the id
	 * @param targetComponent the target component
	 * @param firstCursor the first cursor
	 * @param secondCursor the second cursor
	 * @param camZoomAmount the cam zoom amount

	 */
	public ZoomEvent(IInputProcessor source, int id, Component targetComponent, InputCursor firstCursor, InputCursor secondCursor, float camZoomAmount) {
		super(source, id, targetComponent);
		this.firstCursor = firstCursor;
		this.secondCursor = secondCursor;
		this.camZoomAmount = camZoomAmount;
		this.centerPoint = getMiddlePointBetweenFingers(firstCursor.getPosition(), secondCursor.getPosition());
	}

	private Vector3D getMiddlePointBetweenFingers(Vector3D firstFinger, Vector3D secondFinger){
		Vector3D bla = secondFinger.getSubtracted(firstFinger); //= direction vector of 1. to 2. finger
		bla.scaleLocal(0.5f); //take the half
		return (new Vector3D(firstFinger.getX() + bla.getX(), firstFinger.getY() + bla.getY(), firstFinger.getZ() + bla.getZ()));
	}

	/**
	 * Gets the cam zoom amount.
	 * 
	 * @return the cam zoom amount
	 */
	public float getCamZoomAmount() {
		return camZoomAmount;
	}

	/**
	 * Gets the first cursor.
	 * 
	 * @return the first cursor
	 */
	public InputCursor getFirstCursor() {
		return firstCursor;
	}

	/**
	 * Gets the second cursor.
	 * 
	 * @return the second cursor
	 */
	public InputCursor getSecondCursor() {
		return secondCursor;
	}

	public Vector3D getCenterPoint() { return centerPoint; }
}
