/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2006-2009  Poul Henriksen and Michael K�lling 
 
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
package greenfoot.gui.input.mouse;

import greenfoot.core.WorldHandler;
import greenfoot.gui.WorldCanvas;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.SwingUtilities;

/**
 * This singleton keeps track of mouse events. It listens for both mouse motion
 * and mouse clicks.
 * 
 * A location will always be returned.
 * 
 * @author Poul Henriksen
 * 
 */
public class LocationTracker
{
    private static LocationTracker instance;
    private MouseEvent mouseButtonEvent;
    private Component sourceComponent;
    private MouseEvent mouseMovedEvent;

    static {
        instance();
    }


    /**
     * Needed for running in applets.
     *
     */
    public static void initialize()
    {
        MouseMotionAdapter mma = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                LocationTracker.instance().move(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e)
            {
            }
        };
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                LocationTracker.instance().click(e);
            }
            
        };

        WorldCanvas canvas = WorldHandler.getInstance().getWorldCanvas();
        canvas.addMouseListener(ma);
        canvas.addMouseMotionListener(mma);
    }
    
    private LocationTracker()
    {
        try {
            AWTEventListener listener = new AWTEventListener() {
                public void eventDispatched(AWTEvent event)
                {
                    MouseEvent me = (MouseEvent) event;
                    if ((event.getID() & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0) {
                        LocationTracker.instance().move(me);
                    }
                    if ((event.getID() & AWTEvent.MOUSE_EVENT_MASK) != 0) {
                        LocationTracker.instance().click(me);
                    }
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(listener,
                    AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
        catch (SecurityException e) {
            // Probably running in an applet, so it should already be
            // initialised to listen for mouse events.
        }

    }

    public synchronized static LocationTracker instance()
    {
        if (instance == null) {
            instance = new LocationTracker();
        }
        return instance;
    }

    /**
     * Gets the last MouseEvent where a mouse button was used.
     */
    public MouseEvent getMouseButtonEvent()
    {
        return mouseButtonEvent;
    }

    /**
     * Gets the last MouseEvent generated by a mouse motion.
     */
    public MouseEvent getMouseMotionEvent()
    {
        return mouseMovedEvent;

    }

    /**
     * Updates last motion event.
     */
    private void move(MouseEvent e)
    {
        mouseMovedEvent = translateEvent(e);
    }

    /**
     * Updates last click position.
     */
    private void click(MouseEvent e)
    {
        mouseButtonEvent = translateEvent(e);
    }

    /**
     * Translate the event coordinates into those of the source sourceComponent
     * of the LocationTracker.
     * 
     */
    private MouseEvent translateEvent(MouseEvent e)
    {
        Component source = e.getComponent();
        if (source != sourceComponent) {
            e = SwingUtilities.convertMouseEvent(source, e, sourceComponent);
        }
        return e;
    }

    /**
     * Sets the sourceComponent that all locations will be relative to.
     */
    public void setSourceComponent(Component source)
    {
        this.sourceComponent = source;
    }
}
