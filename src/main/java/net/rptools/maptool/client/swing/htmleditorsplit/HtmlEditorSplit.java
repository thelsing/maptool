package net.rptools.maptool.client.swing.htmleditorsplit;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.beans.JavaBean;

@JavaBean(
        defaultProperty = "UI",
        description = "HTML-Editor that also displays sourcecode"
)
public class HtmlEditorSplit extends JPanel {
    private HtmlEditorSplitGui gui = new HtmlEditorSplitGui();

    public HtmlEditorSplit()
    {
        setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        add(gui.$$$getRootComponent$$$(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    public String getText() {
        return gui.getText();
    }

    public void setText(String text) {
        gui.setText(text);
    }

    public String getSelectedText() {
        return gui.getSelectedText();
    }
}
