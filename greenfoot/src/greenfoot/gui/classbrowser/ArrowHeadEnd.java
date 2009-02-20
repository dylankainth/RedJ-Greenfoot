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
package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;

/**
 * Graphics for the head of an arrow. With a connector to the right.
 *  ^ | |__
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowHeadEnd.java 6167 2009-02-20 10:42:49Z polle $
 */
public class ArrowHeadEnd extends ArrowElement
{
    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        Polygon arrow = new Polygon();
        arrow.addPoint(size.width / 2, 0);
        arrow.addPoint((size.width / 2) - ARROW_WIDTH / 2, ARROW_HEIGHT);
        arrow.addPoint((size.width / 2) + ARROW_WIDTH / 2, ARROW_HEIGHT);

        g.drawLine(size.width / 2, 0 + ARROW_HEIGHT, size.width / 2, size.height / 2);
        g.drawLine(size.width / 2, size.height / 2, size.width, size.height / 2);
        g.drawPolygon(arrow);
    }
}