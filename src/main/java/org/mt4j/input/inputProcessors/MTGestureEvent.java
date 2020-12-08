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
package org.mt4j.input.inputProcessors;

import org.mt4j.input.inputData.MTInputEvent;

import javax.swing.*;
import java.awt.*;


/**
 * The Class GestureEvent.
 * @author Christopher Ruff
 */
public abstract class MTGestureEvent extends MTInputEvent {
	
	/** The id. */
	private int id;
	

	/** The Constant GESTURE_STARTED. */
	public static final int GESTURE_STARTED 	= 0;
	
	/** The Constant GESTURE_UPDATED. */
	public static final int GESTURE_UPDATED 	= 1;
	
	/** The Constant GESTURE_ENDED. */
	public static final int GESTURE_ENDED		= 2;
	
	
	/** The Constant GESTURE_CANCELED. 
	 * Used when the gesture/input processing is aborted.
	 * It is not guaranteed atm that GESTURE_ENDED will be called in the future.
	 * */
	public static final int GESTURE_CANCELED	= 3;
	
	
	/** The Constant GESTURE_RESUMED. 
	 * Used when a gesture is resumed because it has the highest priority again for example.
	 * */
	public static final int GESTURE_RESUMED		= 4;
	
	/**
	 * Instantiates a new gesture event.
	 * 
	 * @param source the source
	 * @param id the id
	 * @param targetComponent the target component
	 */
	public MTGestureEvent(IInputProcessor source, int id, Component targetComponent) {
		super(source, targetComponent, false);
		this.id = id;
		this.setCurrentTarget(targetComponent); //test ..
	}
	
	
	

	@Override
	public IInputProcessor getSource() {
		return (IInputProcessor)super.getSource();
	}


	/**
	 * Gets the id.
	 * <br>Can be a value of:
	 * <ul>
	 * <li>GESTURE_DETECTED
	 * <li>GESTURE_UPDATED
	 * <li>GESTURE_ENDED
	 * </ul>
	 * 
	 * @return the id
	 */
	public int getId() {
		return id;
	}


	/**
	 * Sets the id.
	 * 
	 * @param id the new id
	 */
	public void setId(int id) {
		this.id = id;
	}

	public boolean isStart() {
		return id == GESTURE_STARTED;
	}

	public boolean isUpdate() {
		return id == GESTURE_UPDATED;
	}

	public boolean isEnd() {
		return id == GESTURE_ENDED;
	}

	public  boolean isCancel() {
		return id == GESTURE_CANCELED;
	}

	public boolean isResume() {
		return id == GESTURE_RESUMED;
	}

	public String getIdName()
	{
		switch (id)
		{
			case GESTURE_STARTED:
				return "GESTURE_STARTED";
			case GESTURE_UPDATED:
				return "GESTURE_UPDATED";
			case GESTURE_ENDED:
				return "GESTURE_ENDED";
			case GESTURE_CANCELED:
				return "GESTURE_CANCELED";
			case GESTURE_RESUMED:
				return "GESTURE_RESUMED";
			default:
				return "UNKNOWN";
		}
	}
}
