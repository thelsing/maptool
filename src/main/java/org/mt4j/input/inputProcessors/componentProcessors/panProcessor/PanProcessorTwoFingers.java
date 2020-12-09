/***********************************************************************
 * mt4j Copyright (c) 2008 - 2009 C.Ruff, Fraunhofer-Gesellschaft All rights reserved.
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
package org.mt4j.input.inputProcessors.componentProcessors.panProcessor;

import java.awt.*;
import java.util.List;

import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IInputProcessor;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.AbstractComponentProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor;
import org.mt4j.util.math.Vector3D;

import javax.swing.*;


/**
 * The Class PanProcessorTwoFingers. Multi-touch gesture processor for panning the
 * canvas by moving the scene's camera. Should only be registered with MTCanvas components.
 * Fires PanEvent gesture events.
 * <br><strong>NOTE:</strong> Should be only used in combination with a MTCanvas component. 
 * @author Christopher Ruff
 */
public class PanProcessorTwoFingers extends AbstractCursorProcessor {
	
	/** The detect radius. */
	private float detectRadius;
	
	/** The applet. */
	private JFrame applet;
	
	/**
	 * Instantiates a new pan processor two fingers.
	 * 
	 * @param app the app
	 */
	public PanProcessorTwoFingers(JFrame app) {
		this(app, 100f);
	}
	
	/**
	 * Instantiates a new pan processor two fingers.
	 * 
	 * @param applet the applet
	 * @param panDetectRadius the pan detect radius
	 */
	public PanProcessorTwoFingers(JFrame applet, float panDetectRadius){
		this.applet = applet;
		this.detectRadius = panDetectRadius;
		this.setLockPriority(2);
	}
	

	@Override
	public void cursorStarted(InputCursor c, AbstractCursorInputEvt positionEvent) {
		InputCursor[] locked = getLockedCursorsArray();
		if (locked.length >= 2){ //gesture with 2 fingers already in progress
			logger.debug(this.getName() + " has already enough cursors for this gesture - adding to unused ID:" + c.getId());
			return;
		}

		List<InputCursor> availableCursors = getFreeComponentCursors();
		logger.debug(this.getName() + " Available cursors: " + availableCursors.size());
			
		if (availableCursors.size() < 2)
			return;

		InputCursor otherCursor = (availableCursors.get(0).equals(c))? availableCursors.get(1) : availableCursors.get(0);

		float newDistance = Vector3D.distance(otherCursor.getPosition(), c.getPosition());
		if (newDistance > detectRadius) {
			logger.debug(this.getName() + " cursors not close enough to start gesture. Distance: " + newDistance);
			return;
		}

		//See if we can obtain a lock on both cursors
		if (!this.getLock(otherCursor, c)) {
			logger.debug(this.getName() + " we could NOT lock both cursors!");
			return;
		}

		logger.debug(this.getName() + " we could lock both cursors! And fingers in zoom distance!");
		firePanEvent(MTGestureEvent.GESTURE_STARTED, positionEvent, otherCursor, c);
	}

	@Override
	public void cursorUpdated(InputCursor c, AbstractCursorInputEvt positionEvent) {
		List<InputCursor> locked = getLockedCursors();
		if (!locked.contains(c) || locked.size() < 2)
			return;

		//in progress with this cursors
		InputCursor firstCursor = locked.get(0);
		InputCursor secondCursor = locked.get(1);

		float newDistance = Vector3D.distance(firstCursor.getPosition(), secondCursor.getPosition());
		if (newDistance > detectRadius) {
			logger.debug(this.getName() + " cursors not close enough to uptate gesture. Distance: " + newDistance);
			firePanEvent(MTGestureEvent.GESTURE_CANCELED, positionEvent, firstCursor, secondCursor);
			unLock(firstCursor, secondCursor);
			return;
		}

		Vector3D translation = null;
		if(c.equals(firstCursor))
			translation =  getNewTranslation(positionEvent.getTarget(), firstCursor, secondCursor);
		else
			translation= getNewTranslation(positionEvent.getTarget(), secondCursor, firstCursor);

		if (c.equals(firstCursor)){
			firePanEvent(MTGestureEvent.GESTURE_UPDATED, positionEvent, firstCursor, secondCursor, translation);
		}else{
			firePanEvent(MTGestureEvent.GESTURE_UPDATED, positionEvent, secondCursor, firstCursor, translation);
		}

	}

	private void firePanEvent(int id, AbstractCursorInputEvt positionEvent, InputCursor firstCursor,
							  InputCursor secondCursor) {
		firePanEvent(id, positionEvent, firstCursor, secondCursor, new Vector3D(0, 0, 0));
	}

	private void firePanEvent(int id, AbstractCursorInputEvt positionEvent, InputCursor firstCursor,
							  InputCursor secondCursor, Vector3D translation) {
		fireGestureEvent(new PanTwoFingerEvent(this, id, positionEvent.getCurrentTarget(),
				firstCursor, secondCursor, new Vector3D(translation.getX(),translation.getY(),0)));
	}

	
	@Override
	public void cursorEnded(InputCursor c, AbstractCursorInputEvt positionEvent) {
		logger.debug(this.getName() + " INPUT_ENDED RECIEVED - cursor: " + c.getId());
		List<InputCursor> locked = getLockedCursors();
		if (!locked.contains(c) || locked.size() < 2)
			return;

		InputCursor leftOverCursor = (locked.get(0).equals(c))? locked.get(1) : locked.get(0);
		InputCursor futureCursor = getFarthestFreeCursorToLimited(leftOverCursor, detectRadius);
		if (futureCursor == null) {
			endGesture(c, leftOverCursor, positionEvent);
			return;
		}

		float newDistance = Vector3D.distance(leftOverCursor.getPosition(), futureCursor.getPosition());
		if (newDistance > detectRadius) {//Check if other cursor is in distance
			this.endGesture(c, leftOverCursor, positionEvent);
			return;
		}

		if(!this.getLock(futureCursor)) {
			this.endGesture(c, leftOverCursor, positionEvent);
			return;
		}

		logger.debug(this.getName() + " we could lock another cursor! (ID:" + futureCursor.getId() +")");
		logger.debug(this.getName() + " continue with different cursors (ID: " + futureCursor.getId()
				+ ")" + " " + "(ID: " + leftOverCursor.getId() + ")");
	}
	
	
	/**
	 * End gesture.
	 * 
	 * @param inputEndedCursor the input ended cursor
	 * @param leftOverCursor the left over cursor
	 */
	private void endGesture(InputCursor inputEndedCursor, InputCursor leftOverCursor, AbstractCursorInputEvt positionEvent){
		this.unLock(leftOverCursor);
		firePanEvent(MTGestureEvent.GESTURE_ENDED, positionEvent, inputEndedCursor, leftOverCursor);
	}
	
	

	@Override
	public void cursorLocked(InputCursor c, IInputProcessor lockingProcessor) {
		if (lockingProcessor instanceof AbstractComponentProcessor){
			logger.debug(this.getName() + " Recieved cursor LOCKED by (" + ((AbstractComponentProcessor)lockingProcessor).getName()  + ") - cursor ID: " + c.getId());
		}else{
			logger.debug(this.getName() + " Recieved cursor LOCKED by higher priority signal - cursor ID: " + c.getId());
		}
		
		List<InputCursor> locked = getLockedCursors();
		if (!locked.contains(c))
			return;

		InputCursor leftOverCursor = (locked.get(0).equals(c))? locked.get(1) : locked.get(0);

		firePanEvent(MTGestureEvent.GESTURE_CANCELED, c.getCurrentEvent(), c, leftOverCursor);
		unLockAllCursors();
		logger.debug(this.getName() + " cursor:" + c.getId() +
				" cursor LOCKED. Was an active cursor in this gesture - we therefore have to stop this gesture!");
	}

	
	
	@Override
	public void cursorUnlocked(InputCursor c) {
		logger.debug(this.getName() + " Recieved UNLOCKED signal for cursor ID: " + c.getId());
		
		if (getLockedCursors().size() >= 2){ //we dont need the unlocked cursor, gesture still in progress
			return;
		}
		
		this.unLockAllCursors();
		
		List<InputCursor> availableCursors = getFreeComponentCursors();
		if (availableCursors.size() < 2)
			return;

		//we can try to resume the gesture
		InputCursor firstCursor = availableCursors.get(0);
		InputCursor secondCursor = getFarthestFreeCursorToLimited(firstCursor, detectRadius);

		if(secondCursor == null)
			return;

		//See if we can obtain a lock on both cursors
		if (!getLock(firstCursor, secondCursor)) {
			logger.debug(this.getName() + " we could NOT lock cursors: " + firstCursor.getId() +", " + secondCursor.getId());
			return;
		}

		//Check if other cursor is in distance
		float newDistance = Vector3D.distance(firstCursor.getPosition(), secondCursor.getPosition());
		if (newDistance > detectRadius)
		{
			logger.debug(this.getName() + " distance was too great between cursors: " +
					firstCursor.getId() +", " + secondCursor.getId() + " distance: " + newDistance);
			return;
		}

		logger.debug(this.getName() + " we could lock cursors: " + firstCursor.getId() +", " + secondCursor.getId());
		logger.debug(this.getName() + " continue with different cursors (ID: " + firstCursor.getId() + ")" + " " + "(ID: " + secondCursor.getId() + ")");

		firePanEvent(MTGestureEvent.GESTURE_RESUMED, c.getCurrentEvent(), firstCursor, secondCursor);
	}

	/**
	 * Gets the new translation.
	 * 
	 * @param comp the comp
	 * @param movingCursor the moving cursor
	 * @param otherCursor the other cursor
	 * 
	 * @return the new translation
	 */
	private Vector3D getNewTranslation(Component comp, InputCursor movingCursor, InputCursor otherCursor){
		if(movingCursor.getEventCount() < 2 || otherCursor.getEventCount() < 2)
			return new Vector3D(0, 0);

		return movingCursor.getPosition().getSubtracted(movingCursor.getPreviousEvent().getPosition());
	}
	
	
	/**
	 * Gets the middle point between fingers.
	 * 
	 * @param firstFinger the first finger
	 * @param secondFinger the second finger
	 * 
	 * @return the middle point between fingers
	 */
	private Vector3D getMiddlePointBetweenFingers(Vector3D firstFinger, Vector3D secondFinger){
		Vector3D bla = secondFinger.getSubtracted(firstFinger); //= direction vector of 1. to 2. finger
		bla.scaleLocal(0.5f); //take the half
		return (new Vector3D(firstFinger.getX() + bla.getX(), firstFinger.getY() + bla.getY(), firstFinger.getZ() + bla.getZ()));
	}

	@Override
	public String getName() {
		return "two finger pan detector";
	}
}
