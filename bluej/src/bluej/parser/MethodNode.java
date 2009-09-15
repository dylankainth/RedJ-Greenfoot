/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

import bluej.parser.nodes.ParentParsedNode;
import bluej.parser.nodes.ParsedNode;

/**
 * A node representing a parsed method or constructor.
 * 
 * @author Davin McCall
 */
public class MethodNode extends ParentParsedNode
{
    private String name;
    
    public MethodNode(ParsedNode parent, String name)
    {
        super(parent);
        this.name = name;
    }
    
    @Override
    public boolean isContainer()
    {
        return true;
    }
    
    @Override
    public int getNodeType()
    {
        return ParsedNode.NODETYPE_METHODDEF;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
}
