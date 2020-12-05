package net.rptools.maptool.client;

import net.rptools.lib.swing.SwingUtil;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class MtMouseEvent {
    private int x;
    private int y;
    private int clickCount;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getClickCount() {
        return clickCount;
    }

    public boolean isLeftMouseButton() {
        return mouseButton == 1;
    }

    public boolean isRightMouseButton() {
        return mouseButton == 2;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    private int mouseButton;
    private boolean shiftDown;

    public MtMouseEvent(int x, int y, int activecursors)
    {
        this.x = x;
        this.y = y;
        mouseButton = activecursors;
    }

    public MtMouseEvent(MouseEvent e)
    {
        x = e.getX();
        y = e.getY();
        clickCount = e.getClickCount();
        if(SwingUtilities.isRightMouseButton(e))
            mouseButton = 2;
        else if (SwingUtilities.isLeftMouseButton(e))
            mouseButton = 1;
        else if(SwingUtilities.isMiddleMouseButton(e))
            mouseButton = 3;

        shiftDown = SwingUtil.isShiftDown(e);
    }


}
