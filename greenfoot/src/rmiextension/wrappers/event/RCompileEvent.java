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
package rmiextension.wrappers.event;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileEvent.java 6167 2009-02-20 10:42:49Z polle $
 *  
 */
public interface RCompileEvent
    extends Remote
{
    /**
     * @return
     */
    public int getErrorLineNumber()
        throws RemoteException;

    /**
     * @return
     */
    public String getErrorMessage()
        throws RemoteException;

    /**
     * @return
     */
    public int getEvent()
        throws RemoteException;

    /**
     * @return
     */
    public File[] getFiles()
        throws RemoteException;

    /**
     * @param aLineNumber
     */
    public void setErrorLineNumber(int aLineNumber)
        throws RemoteException;

    /**
     * @param anErrorMessage
     */
    public void setErrorMessage(String anErrorMessage)
        throws RemoteException;
}