/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of f
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */

package greenfoot.guifx;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

/**
 * The content pane for GreenfootStage.  This needs to be a custom pane to get the layout
 * algorithm implemented as we want.
 */
class GreenfootStageContentPane extends Pane
{
    private static final int CLASS_DIAGRAM_PADDING = 12;
    private static final int IDEAL_WORLD_PADDING = 30;
    private final Pane worldViewScroll;
    private final ScrollPane classDiagramScroll;
    private final Pane controlPanel;

    /**
     * Construct a content pane for the three major components: the world view,
     * the class diagram, and the control panel.
     */
    public GreenfootStageContentPane(Pane worldViewScroll, ScrollPane classDiagramScroll, ControlPanel controlPanel)
    {
        this.worldViewScroll = worldViewScroll;
        this.classDiagramScroll = classDiagramScroll;
        this.controlPanel = controlPanel;
        getChildren().addAll(worldViewScroll, classDiagramScroll, controlPanel);
    }

    @Override
    protected void layoutChildren()
    {
        final double ourWidth = getWidth();
        final double ourHeight = getHeight();
        
        final double idealWorldWidth = worldViewScroll.prefWidth(-1);

        // Class diagram height is known: our height minus padding
        final double classDiagramHeight = ourHeight - 2 * CLASS_DIAGRAM_PADDING;
        final double idealClassDiagramWidth = classDiagramScroll.prefWidth(classDiagramHeight);
        
        final double classDiagramWidth;
        if (idealClassDiagramWidth + 2 * CLASS_DIAGRAM_PADDING + idealWorldWidth > ourWidth)
        {
            // Someone is going to have lose some width.  We start by taking it from class diagram:
            double minClassDiagramWidth = classDiagramScroll.minWidth(classDiagramHeight);
            classDiagramWidth = Math.max(minClassDiagramWidth, 
                ourWidth - idealWorldWidth - 2 * CLASS_DIAGRAM_PADDING);
        }
        else
        {
            // Everyone can have what they want, width-wise:
            classDiagramWidth = idealClassDiagramWidth;
        }
        
        // The control panel is always its preferred height:
        final double worldWidth = ourWidth - (classDiagramWidth + 2 * CLASS_DIAGRAM_PADDING);
        final double controlPanelHeight = controlPanel.prefHeight(worldWidth);
        
        worldViewScroll.resizeRelocate(0, 0, worldWidth, ourHeight - controlPanelHeight);
        classDiagramScroll.resizeRelocate(worldWidth + CLASS_DIAGRAM_PADDING, CLASS_DIAGRAM_PADDING,
                classDiagramWidth, classDiagramHeight);
        controlPanel.resizeRelocate(0, ourHeight - controlPanelHeight, worldWidth, controlPanelHeight);
    }

    @Override
    protected double computePrefWidth(double height)
    {
        // Not quite accurate, but shouldn't matter when we have no real parent.
        // This is really just for calculating the initial window size:
        return worldViewScroll.prefWidth(height) + 2 * IDEAL_WORLD_PADDING /* Some world spacing */ 
                + classDiagramScroll.prefWidth(height) + 2 * CLASS_DIAGRAM_PADDING;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        // Again, not quite accurate, but should be close enough when we are topmost container:
        return Math.max(classDiagramScroll.prefHeight(-1) + 2 * CLASS_DIAGRAM_PADDING, 
            worldViewScroll.prefHeight(-1) + 2 * IDEAL_WORLD_PADDING + controlPanel.prefHeight(-1));
    }
}
