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
package org.mt4j.input.inputProcessors.componentProcessors.tapProcessor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

import org.cactoos.Input;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IInputProcessor;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.AbstractComponentProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor;
import org.mt4j.util.math.Vector3D;

import javax.swing.*;

/**
 * The Class TapProcessor. Tap multitouch gesture. Triggered on a component
 * that is tapped with a finger. Also allows to recognized double-tapps if the "enableDoubleTap" is set to true.
 * Fires TapEvent gesture events.
 * @author Christopher Ruff
 */
public class TapProcessor extends AbstractCursorProcessor {

	class TapContext {
		/** The button down screen pos. */
		private Vector3D buttonDownScreenPos;

		/** The time last tap. */
		private long timeLastTap;
		private long tapStartTime;
		private Component target;
		private InputCursor cursor;
		private boolean holdComplete;
		private Timer timer;
		private TapProcessor processor;

		public TapContext(InputCursor c)
		{
			buttonDownScreenPos = c.getPosition();
			tapStartTime = System.currentTimeMillis();
			target = c.getCurrentEvent().getCurrentTarget();
			cursor = c;
			holdComplete = false;
			timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					timerElapsed();
				}
			}, holdTime);
		}

		public Vector3D getButtonDownScreenPos() {
			return buttonDownScreenPos;
		}

		public synchronized void cancelHold()
		{
			if(timer == null)
				return;
			timer.cancel();
			timer = null;
		}

		public boolean isHoldComplete() {
			return holdComplete;
		}


		public InputCursor getCursor() {
			return cursor;
		}

		public long getElapsedTime()
		{
			long nowTime = System.currentTimeMillis();
			return nowTime - this.tapStartTime;
		}

		private void timerElapsed(){
			Vector3D screenPos = cursor.getPosition();

			if ( hasIntersection(target, cursor)&& Vector3D.distance2D(buttonDownScreenPos, screenPos) <= maxFingerUpDist) {
				fireTapAndHoldEvent(this, cursor, MTGestureEvent.GESTURE_UPDATED, target);
				fireTapEvent(cursor, MTGestureEvent.GESTURE_CANCELED, TapEvent.TAP_UP);
			} else {
				logger.debug("DISTANCE TOO FAR OR NO INTERSECTION");
				fireTapAndHoldEvent(this, cursor, MTGestureEvent.GESTURE_CANCELED, target);
			}
			holdComplete = true;
		}
	}

	/** The applet. */
	private JFrame applet;
	
	/** The max finger up dist. */
	private float maxFingerUpDist;
	
	/** The enable double tap. */
	private boolean enableDoubleTap;
	
	/** The double tap time. */
	private int doubleTapTime = 300;

	/** The tap time. */
	private int holdTime;


	private Map<InputCursor, TapContext> cursorContextMap;
	
	/**
	 * Instantiates a new tap processor.
	 * 
	 * @param pa the pa
	 */
	public TapProcessor(JFrame pa) {
		this(pa, 18.0f);
	}
	
	/**
	 * Instantiates a new tap processor.
	 * 
	 * @param pa the pa
	 * @param maxFingerUpDistance the max finger up distance
	 */
	public TapProcessor(JFrame pa, float maxFingerUpDistance) {
		this(pa, maxFingerUpDistance, true, 300);
	}
	
	/**
	 * Instantiates a new tap processor.
	 * 
	 * @param pa the pa
	 * @param maxFingerUpDistance the max finger up distance
	 * @param enableDoubleTap the enable double tap
	 */
	public TapProcessor(JFrame pa, float maxFingerUpDistance, boolean enableDoubleTap){
		this(pa, maxFingerUpDistance, enableDoubleTap, 300);
	}
	
	/**
	 * Instantiates a new tap processor.
	 *
	 * @param pa the pa
	 * @param maxFingerUpDistance the max finger up distance
	 * @param enableDoubleTap the enable double tap
	 * @param doubleTapTime the double tap time
	 */
	public TapProcessor(JFrame pa, float maxFingerUpDistance, boolean enableDoubleTap, int doubleTapTime){
		this(pa, maxFingerUpDistance, enableDoubleTap, doubleTapTime, 1000, false);
	}

	/**
	 * Instantiates a new tap processor.
	 *
	 * @param pa the pa
	 * @param maxFingerUpDistance the max finger up distance
	 * @param enableDoubleTap the enable double tap
	 * @param doubleTapTime the double tap time
	 * @param stopEventPropagation the stop event propagation
	 */

	public TapProcessor(JFrame pa, float maxFingerUpDistance, boolean enableDoubleTap, int doubleTapTime, int holdTime, boolean stopEventPropagation){
		super(stopEventPropagation);
		this.applet = pa;
		this.maxFingerUpDist = maxFingerUpDistance;
		this.setLockPriority(3);
		this.setDebug(true);

		this.holdTime = holdTime;
		this.enableDoubleTap = enableDoubleTap;
		this.doubleTapTime = doubleTapTime;
		this.cursorContextMap = new HashMap<InputCursor, TapContext>()
;
	}

	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor#cursorStarted(org.mt4j.input.inputData.InputCursor, org.mt4j.input.inputData.MTFingerInputEvt)
	 */
	@Override
	public void cursorStarted(InputCursor m, AbstractCursorInputEvt positionEvent) {

		if (getLock(m)){//See if we can obtain a lock on this cursor (depends on the priority)
			logger.debug(this.getName() + " successfully locked cursor (id:" + m.getId() + ")");
			TapContext ctx = new TapContext(m);
			cursorContextMap.put(m, ctx);
			fireTapEvent(m, MTGestureEvent.GESTURE_STARTED, TapEvent.TAP_DOWN);
			fireTapAndHoldEvent(ctx, m, MTGestureEvent.GESTURE_STARTED);
		}
	}


	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor#cursorUpdated(org.mt4j.input.inputData.InputCursor, org.mt4j.input.inputData.MTFingerInputEvt)
	 */
	@Override
	public void cursorUpdated(InputCursor c, AbstractCursorInputEvt positionEvent) {
		if (!getLockedCursors().contains(c))
			return;

		TapContext ctx = cursorContextMap.get(c);

		Vector3D screenPos = c.getPosition();
		if (Vector3D.distance2D(ctx.getButtonDownScreenPos(), screenPos) > this.maxFingerUpDist){
			logger.debug(this.getName() + " DISTANCE TOO FAR");
			cancelGesture(ctx, c, positionEvent);
		}
	}

	private void cancelGesture(TapContext ctx, InputCursor c, AbstractCursorInputEvt positionEvent)
	{
		fireTapAndHoldEvent(ctx, c, MTGestureEvent.GESTURE_CANCELED);
		endTabGesture(ctx, c, positionEvent);
		cleanup(c);
	}
	
	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor#cursorEnded(org.mt4j.input.inputData.InputCursor, org.mt4j.input.inputData.MTFingerInputEvt)
	 */
	@Override
	public void cursorEnded(InputCursor m, AbstractCursorInputEvt positionEvent) {
		logger.debug(this.getName() + " INPUT_ENDED RECIEVED - CURSOR: " + m.getId());
		List<InputCursor> locked = this.getLockedCursors();
		if (!locked.contains(m))
			return;

		TapContext ctx = cursorContextMap.get(m);
		ctx.cancelHold();
		if(ctx.holdComplete) {
			// Hold was completed. The cursor was already unlocked. Send GESTURE_END in case app wants it.
			fireTapAndHoldEvent(ctx, m, MTGestureEvent.GESTURE_ENDED);
			return;
		}

		fireTapAndHoldEvent(ctx, m, MTGestureEvent.GESTURE_CANCELED);
		this.endTabGesture(ctx, m, positionEvent);
		cleanup(m);
	}


	private void cleanup(InputCursor c)
	{
		cursorContextMap.remove(c);
		unLock(c);
	}

	/**
	 * End gesture.
	 *
	 * @param m the m
	 * @param positionEvent the position event
	 */
	private void endTabGesture(TapContext ctx, InputCursor m, AbstractCursorInputEvt positionEvent){
		//Default where for the event if no intersections are found
		Vector3D buttonUpScreenPos = m.getPosition();

		Component target = positionEvent.getCurrentTarget();

		if (!hasIntersection(target, m)
				|| Vector3D.distance2D(ctx.getButtonDownScreenPos(), buttonUpScreenPos) > this.maxFingerUpDist) {

			fireTapEvent(m, MTGestureEvent.GESTURE_CANCELED, TapEvent.TAP_UP);
			return;
		}

		//We have a valid TAP!
		fireTapEvent(m, MTGestureEvent.GESTURE_ENDED, TapEvent.TAPPED);
	}


	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor#cursorLocked(org.mt4j.input.inputData.InputCursor, org.mt4j.input.inputProcessors.IInputProcessor)
	 */
	@Override
	public void cursorLocked(InputCursor m, IInputProcessor lockingProcessor) {
		if (lockingProcessor instanceof AbstractComponentProcessor){
			logger.debug(this.getName() + " Recieved CURSOR LOCKED by (" + ((AbstractComponentProcessor)lockingProcessor).getName()  + ") - cursor ID: " + m.getId());
		}else{
			logger.debug(this.getName() + " Recieved CURSOR LOCKED by higher priority signal - cursor ID: " + m.getId());
		}

		logger.debug(this.getName() + " cursor:" + m.getId() + " CURSOR LOCKED. Was an active cursor in this gesture!");
		fireTapEvent(m, MTGestureEvent.GESTURE_CANCELED, TapEvent.TAP_UP);

		TapContext ctx = cursorContextMap.get(m);
		ctx.cancelHold();
		if(!ctx.holdComplete)
		{
			fireTapAndHoldEvent(ctx, m, MTGestureEvent.GESTURE_CANCELED);
		}
		cursorContextMap.remove(m);
	}

	private void fireTapEvent(InputCursor m, int gestureId, int tapId)
	{
		fireGestureEvent(new TapEvent(this,gestureId, m.getCurrentEvent().getCurrentTarget(), m, m.getPosition(), tapId));
	}

	private void fireTapAndHoldEvent(TapContext ctx, InputCursor m, int id)
	{
		fireTapAndHoldEvent(ctx, m, id, m.getCurrentEvent().getCurrentTarget());
	}

	private void fireTapAndHoldEvent(TapContext ctx, InputCursor m, int id, Component target)
	{
		fireGestureEvent(new TapAndHoldEvent(this, id, target, m, m.getPosition(), this.holdTime, ctx.getElapsedTime()));
	}

	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.componentProcessors.AbstractCursorProcessor#cursorUnlocked(org.mt4j.input.inputData.InputCursor)
	 */
	@Override
	public void cursorUnlocked(InputCursor m) {
		// do nothing
	}

	
	
	/**
	 * Gets the max finger up dist.
	 * 
	 * @return the max finger up dist
	 */
	public float getMaxFingerUpDist() {
		return maxFingerUpDist;
	}


	/**
	 * Sets the maximum allowed distance of the position
	 * of the finger_down event and the finger_up event
	 * that fires a click event (in screen pixels).
	 * <br>This ensures that a click event is only raised
	 * if the finger didnt move too far during the click.
	 * 
	 * @param maxFingerUpDist the max finger up dist
	 */
	public void setMaxFingerUpDist(float maxFingerUpDist) {
		this.maxFingerUpDist = maxFingerUpDist;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.componentProcessors.AbstractComponentProcessor#getName()
	 */
	@Override
	public String getName() {
		return "Tap Processor";
	}
}
