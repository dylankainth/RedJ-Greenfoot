/*
 This file is part of the BlueJ program.
 Copyright (C) 2015  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.IdentityHashMap;

import javax.swing.*;

import bluej.Config;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.editor.moe.MoeEditor;
import bluej.pkgmgr.Project;

/**
 * A window which contains all the (Swing) editor tabs for Java
 */
@OnThread(Tag.Swing)
public class SwingTabbedEditor
{
    /** The actual GUI window */
    private final JFrame window;
    /** The tabbed pane inside the window, containing all the tabs */
    private final JTabbedPane tabPane;
    /** The project which this window is associated with */
    private final Project project;
    // We need to maintain a bidirectional mapping from tab/tab components/menubar to editor.
    /** Maps tab content to editor */
    private final IdentityHashMap<Component, MoeEditor> panelToEditor = new IdentityHashMap<>();
    /** Maps editor to tab content */
    private final IdentityHashMap<MoeEditor, Component> editorToPanel = new IdentityHashMap<>();
    /** Maps editor to tab header panel */
    private final IdentityHashMap<MoeEditor, HeaderPanel> editorToHeader = new IdentityHashMap<>();
    /** Maps editor to menu bar */
    private final IdentityHashMap<MoeEditor, JMenuBar> menuBars = new IdentityHashMap<>();

    /**
     * Constructs a SwingTabbedEditor for the given project
     */
    public SwingTabbedEditor(Project project)
    {
        window = new JFrame(project.getProjectName() + " - Java");
        tabPane = new JTabbedPane();
        // Create some actions for selecting tabs, which will later be bound to Ctrl+1, etc
        for (int i = 1; i <= 9; i++)
        {
            tabPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(Character.forDigit(i, 10), Config.isMacOS() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK), "selectTab" + i);
            final int tabIndex = i - 1;
            tabPane.getActionMap().put("selectTab" + i, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (tabPane.getTabCount() > tabIndex)
                        tabPane.setSelectedIndex(tabIndex);
                }
            });
        }
        
        window.add(tabPane);

        // When the selected tab changes, we must set the menubar, and notify the editors
        // as to whether they are visible or not:
        tabPane.addChangeListener(evt -> {
            MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
            window.setJMenuBar(menuBars.get(editor));
            updateTitle();
            for (MoeEditor ed : editorToPanel.keySet())
            {
                ed.notifyVisibleTab(ed == editor && window.isFocused());
            }
        });
        
        WindowFocusListener windowFocusListener = new WindowFocusListener() {
            
            @Override
            public void windowLostFocus(WindowEvent e)
            {
                for (MoeEditor ed : editorToPanel.keySet())
                    ed.notifyVisibleTab(false);
            }
            
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
                for (MoeEditor ed : editorToPanel.keySet())
                    ed.notifyVisibleTab(ed == editor);
            }
        };
        
        window.addWindowFocusListener(windowFocusListener);
        
        this.project = project;
    }

    private void updateTitle()
    {
        final MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
        if (editor != null)
            window.setTitle(editor.getTitle() + " - " + project.getProjectName());
        else
            window.setTitle(project.getProjectName());
    }

    /**
     * Sets the given editor visible or not.
     * @param visible If true, makes tab for MoeEditor if needed, and shows the tab and the window.
     *                If false, closes the tab.
     * @param editor The editor in question
     */
    public void setEditorVisible(boolean visible, MoeEditor editor)
    {
        if (editor == null)
            throw new IllegalArgumentException("Cannot show null editor");

        if (visible)
        {
            // Must ask for editorTab before calling pack(), otherwise pack makes window zero-size:
            
            Component editorTab = getMoeEditorTab(editor);
            
            if (!window.isShowing())
            {
                window.pack();
                window.setVisible(true);
                // On Windows7, there was a problem with the window rendering white on re-show,
                // until it was resized.  As a work-around, we do the resize ourselves:
                EventQueue.invokeLater(() -> {
                    // We need a resize but don't really want to resize, just need one pixel change.
                    // If window size is odd, -1, if it's even then +1.  That way, repeated re-shows
                    // will not continually grow or shrink window:
                    if (window.getWidth() % 2 == 0)
                        window.setSize(window.getWidth() + 1, window.getHeight());
                    else
                        window.setSize(window.getWidth() - 1, window.getHeight());
                });
            }
            
            tabPane.setSelectedComponent(editorTab);
        }
        else
        {
            Component tab = editorToPanel.get(editor);
            tabPane.remove(tab);
            panelToEditor.remove(tab);
            editorToPanel.remove(editor);
            editorToHeader.remove(editor);
            if (tabPane.getTabCount() == 0)
            {
                window.setVisible(false);
            }
        }
    }

    /**
     * Fetches the editor tab component corresponding to the given editor.  If it doesn't
     * currently exist, creates it and adds it to the tab pane.
     */
    private Component getMoeEditorTab(final MoeEditor editor)
    {
        // If you try to put MoeEditor directly into the JTabbedPane as a tab, it will not work.
        // I am not 100% sure why, but presumably we are doing something complex with the JPanel
        // which JTabbedPane does not support (e.g. JTabbedPane says don't call setVisible on a tab;
        // we override it so this shouldn't be an issue, but there is presumably some other similar
        // problem).  Wrapping the MoeEditor in a further JPanel seems to solve the issue, even if
        // it does feel a bit hacky.
        
        if (!editorToPanel.containsKey(editor))
        {       
            JPanel tmp = new JPanel();
            tmp.setLayout(new BorderLayout());
            tmp.add(editor,BorderLayout.CENTER);
            panelToEditor.put(tmp, editor);
            editorToPanel.put(editor, tmp);
            HeaderPanel header = new HeaderPanel(editor);

            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem close = new JMenuItem(Config.getString("editor.closeLabel"));
            close.addActionListener(e -> {
                setEditorVisible(false, editor);
            });
            contextMenu.add(close);
            header.setComponentPopupMenu(contextMenu);


            editorToHeader.put(editor, header);
            tabPane.addTab(editor.getTitle(), tmp);
            tabPane.setTabComponentAt(tabPane.indexOfComponent(tmp), header);
        }
        return editorToPanel.get(editor);
    }

    /**
     * Brings the window to the front.
     */
    public void bringToFront()
    {
        window.toFront();
        Utility.bringToFront(window);
        MoeEditor editor = panelToEditor.get(tabPane.getSelectedComponent());
        editor.getSourcePane().requestFocusInWindow();
        editor.getSourcePane().setVisible(true);
    }

    /**
     * Delete for Project.scheduleCompilation
     */
    public void scheduleCompilation(boolean immediate)
    {
        project.scheduleCompilation(immediate);
    }

    /**
     * Gets the menu bar for the given editor.
     */
    public JMenuBar getJMenuBar(MoeEditor e)
    {
        return menuBars.get(e);
    }

    /**
     * Sets the menu bar for the given editor.
     */
    public void setJMenuBar(MoeEditor e, JMenuBar menuBar)
    {
        menuBars.put(e, menuBar);
    }

    /**
     * Sets the tab title for the given editor.
     */
    public void setTitle(MoeEditor moeEditor, String title)
    {
        editorToHeader.get(moeEditor).setTitle(title);
        updateTitle();
    }

    /**
     * A component for the tab header; contains a title bale, and a close button
     */
    private class HeaderPanel extends JPanel
    {
        private final JLabel label;

        public HeaderPanel(MoeEditor editor)
        {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);
            label = new JLabel(editor.getTitle());
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            label.setOpaque(false);
            label.setInheritsPopupMenu(true);
            add(label);
            
            CloseIcon closeIcon = new CloseIcon();
            
            JButton close = new JButton(closeIcon);
            close.setBorder(null);
            close.setBorderPainted(false);
            close.setOpaque(false);
            close.setContentAreaFilled(false);
            close.addActionListener(e -> setEditorVisible(false, editor));
            close.setInheritsPopupMenu(true);
            add(close);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (SwingUtilities.isMiddleMouseButton(e))
                    {
                        setEditorVisible(false, editor);
                    }
                    else
                    {
                        setEditorVisible(true, editor);
                    }
                }
            });
            // For close, all buttons (including middle) should close tab:
            close.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    setEditorVisible(false, editor);
                }
            });
        }
        
        public void setTitle(String title)
        {
            label.setText(title);
        }
    }

    /**
     * A hand-drawn 16x16 black cross icon for the close button.
     */
    private class CloseIcon implements Icon
    {
        private int width = 16;
        private int height = 16;
        

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2d = (Graphics2D) g;
            height = g2d.getFontMetrics(g2d.getFont()).getHeight();
            width = height;
            int offset = 5;
            
            g2d.setColor(Color.BLACK);
            
            g2d.drawLine(x + offset , y + offset, width - offset , height - offset ); // draw "\"
            g2d.drawLine(width - offset, y + offset, x + offset , height - offset); // draw "/"

            g2d.dispose();
        }

        /**
         * @return the width
         */
        @Override
        public int getIconWidth()
        {
            return width;
        }

        /**
         * @return the height
         */
        @Override
        public int getIconHeight()
        {
            return height;
        }
    }
}
