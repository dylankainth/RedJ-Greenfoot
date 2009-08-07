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

import java.io.Reader;

/**
 * Parse class to get info.
 * 
 * To work "properly" this is a more complicated process which potentially requires parsing
 * multiple source files. However, at the moment we parse a single file at a time. We only
 * create dependencies to existing classes in the same package (as supplied).
 * 
 * @author Davin McCall
 * @version $Id: ClassParser.java 6501 2009-08-07 05:16:02Z davmac $
 */
public class ClassParser extends InfoParser
{
    public ClassParser(Reader r)
    {
    	super(r);
    }
}
