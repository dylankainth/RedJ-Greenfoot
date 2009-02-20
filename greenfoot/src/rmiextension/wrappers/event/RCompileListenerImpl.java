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

import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileListenerImpl.java 6167 2009-02-20 10:42:49Z polle $
 */
public abstract class RCompileListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RCompileListener
{

    public RCompileListenerImpl()
        throws RemoteException
    {
        super();
    }

    /**
     * This method will be called when a compilation starts. If a long operation
     * must be performed you should start a Thread.
     */
    public abstract void compileStarted(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile error. If
     * a long operation must be performed you should start a Thread.
     */
    public abstract void compileError(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile warning.
     * If a long operation must be performed you should start a Thread.
     */
    public abstract void compileWarning(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile ends successfully. If a long
     * operation must be performed you should start a Thread.
     */
    public abstract void compileSucceeded(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile fails. If a long operation
     * must be performed you should start a Thread.
     */
    public abstract void compileFailed(RCompileEvent event)
        throws RemoteException;
}