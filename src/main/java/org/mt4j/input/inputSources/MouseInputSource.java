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
package org.mt4j.input.inputSources;

import net.rptools.maptool.client.MapTool;
import org.mt4j.input.inputData.ActiveCursorPool;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputData.MTFingerInputEvt;
import org.mt4j.input.inputData.MTMouseInputEvt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * The Class MouseInputSource.
 *
 * @author Christopher Ruff
 */
public class MouseInputSource extends AbstractInputSource implements AWTEventListener, MouseListener {

    /**
     * The Constant OPENGL_MODE.
     */
    public final static int OPENGL_MODE = 0;

    /**
     * The Constant JAVA_MODE.
     */
    public final static int JAVA_MODE = 1;
    // private int mode;
    /**
     * The last used mouse id.
     */
    private long lastUsedMouseID;

    /**
     * The mouse busy.
     */
    private boolean mouseBusy;

    // private Stack lastUsedMouseIDs;
    /**
     * The mouse pressed button.
     */
    private int mousePressedButton;

    // make singleton
    private JFrame frame;
    /**
     * Instantiates a new mouse input source.
     *
     * @param pa the pa
     */
    public MouseInputSource(JFrame pa) {
        super(pa);
        frame = pa;
        // this.mode = mode;

        //pa.getContentPane().addMouseListener(this);
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        // if (this.mode == MouseInputSource.OPENGL_MODE){
        // pa.registerMouseEvent(this);
        // }

        // if (ConstantsAndSettings.getInstance().isOpenGlMode()){
        //pa.registerMethod("mouseEvent", this);
        // }

        mouseBusy = false;
    }


    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {

    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
    }

    private Point pointFromEvent(MouseEvent e)
    {
        Point p = new Point();
        p.x = e.getXOnScreen() - frame.getX();;
        p.y = e.getYOnScreen() - frame.getY();
        return p;

        //Point glassPanePoint = e.getPoint();

        //Point containerPoint = SwingUtilities.convertPoint(frame.getGlassPane(),glassPanePoint,frame.getContentPane());
        //return containerPoint;
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        if (!mouseBusy) {
            mousePressedButton = e.getButton();
            mouseBusy = true;

            Point p = pointFromEvent(e);

            InputCursor m = new InputCursor();
            MTMouseInputEvt touchEvt =
                    new MTMouseInputEvt(this, e.getModifiers(), p.x, p.y, MTFingerInputEvt.INPUT_STARTED, m,
                            e.getButton());

            lastUsedMouseID = m.getId();
            ActiveCursorPool.getInstance().putActiveCursor(lastUsedMouseID, m);
            //
            // System.out.println("MouseSource Finger DOWN, Motion ID: " + m.getId());
            // FIRE
            this.enqueueInputEvent(touchEvt);

        }
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
        // System.err.println("mouse dragged");
        try {
            InputCursor m = ActiveCursorPool.getInstance().getActiveCursorByID(lastUsedMouseID);
            if (m != null) {
                Point p = pointFromEvent(e);
                MTMouseInputEvt te =
                        new MTMouseInputEvt(this, e.getModifiers(), p.x, p.y, MTFingerInputEvt.INPUT_UPDATED,
                                m, e.getButton());
                // System.out.println("MouseSource Finger UPDATE, Motion ID: " + m.getId());
                this.enqueueInputEvent(te);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
        // System.err.println("mouse released");
        if (e.getButton() == mousePressedButton) {
            Point p = pointFromEvent(e);
            InputCursor m = ActiveCursorPool.getInstance().getActiveCursorByID(lastUsedMouseID);
            MTMouseInputEvt te =
                    new MTMouseInputEvt(this, e.getModifiers(), p.x, p.y, MTFingerInputEvt.INPUT_ENDED, m,
                            e.getButton());

            // System.out.println("MouseSource Finger UP, Motion ID: " + m.getId());
            this.enqueueInputEvent(te);

            ActiveCursorPool.getInstance().removeCursor((lastUsedMouseID));
            mouseBusy = false;
        }
    }

    public void mouseWheeled(MouseEvent event) {

    }

    @Override
    public void eventDispatched(AWTEvent event) {

        if (!(event instanceof MouseEvent))
            return;

        MouseEvent evt = (MouseEvent) event;
        Point p = pointFromEvent(evt);


        switch (evt.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                this.mousePressed(evt);
                break;
            case MouseEvent.MOUSE_RELEASED:
                this.mouseReleased(evt);
                break;
            case MouseEvent.MOUSE_CLICKED:
                this.mouseClicked(evt);
                break;
            case MouseEvent.MOUSE_DRAGGED:
                this.mouseDragged(evt);
                break;
            case MouseEvent.MOUSE_MOVED:
                this.mouseMoved(evt);
                break;
        }
    }

    // @Override
    // public boolean firesEventType(Class<? extends MTInputEvent> evtClass){
    // return (evtClass == MTFingerInputEvt.class);
    // }

}
