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
/*
 * StandalonePropStringManager.java
 *
 * Created on 3/10/2007, 15:09:39
 *
 */

package greenfoot.util;

import bluej.BlueJPropStringSource;
import java.util.Properties;


/**
 * A standalone reader of property strings for use in exported scenarios
 * as standalone jar files
 * 
 * @author Bruce Quig
 */
public class StandalonePropStringManager implements BlueJPropStringSource
{
    private Properties values;
    
    public StandalonePropStringManager(Properties props)
    {
        values = props;
    }

    public String getBlueJPropertyString(String property, String def)
    {
       return values.getProperty(property, def);
    }

    public String getLabel(String key)
    {
        return values.getProperty(key, key);
    }

    public void setUserProperty(String property, String value)
    {
        values.setProperty(property, value);
    }
    
}