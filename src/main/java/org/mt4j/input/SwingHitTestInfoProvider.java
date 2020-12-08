package org.mt4j.input;

import org.mt4j.input.IHitTestInfoProvider;

import javax.swing.*;
import java.awt.*;

public class SwingHitTestInfoProvider implements IHitTestInfoProvider {
    JFrame top;

    public SwingHitTestInfoProvider(JFrame top)
    {

        this.top = top;
    }

    @Override
    public Component getComponentAt(float x, float y) {
        Point p = new Point();
        p.x = (int)x;
        p.y = (int)y;
        Component c = null;

        if (top.isShowing()) {
            if (top instanceof RootPaneContainer)
                c =
                        ((RootPaneContainer) top).getLayeredPane().findComponentAt(
                                SwingUtilities.convertPoint(top, p, ((RootPaneContainer) top).getLayeredPane()));
            else
                c = ((Container) top).findComponentAt(p);
        }
        Component c2 = SwingUtilities.getDeepestComponentAt(top, p.x, p.y);

        return c;
    }

    @Override
    public boolean isBackGroundAt(float x, float y) {
        return false;
    }
}
