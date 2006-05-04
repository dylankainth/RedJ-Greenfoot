package greenfoot.core;

import greenfoot.Actor;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.EventListenerList;

/**
 * The main class of the simulation. It drives the simulation and calls act()
 * obejcts in the world and then paints them.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class Simulation extends Thread
{
    private WorldHandler worldHandler;
    private boolean paused;

    private EventListenerList listenerList = new EventListenerList();

    private SimulationEvent startedEvent;
    private SimulationEvent stoppedEvent;
    private SimulationEvent speedChangeEvent;
    private static Simulation instance;

    /** for timing the animation */
    public static final int MAX_SIMULATION_SPEED = 100;
    private int speed;      // the simulation speed in range (1..100)
    
    private long lastDelayTime;
    private int delay;      // the speed translated into delay (ms) per step

    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation()
    {}

    
    public static void initialize(WorldHandler worldHandler)
    {
        if (instance == null) {
            instance = new Simulation();

            instance.worldHandler = worldHandler;
            
            instance.startedEvent = new SimulationEvent(instance, SimulationEvent.STARTED);
            instance.stoppedEvent = new SimulationEvent(instance, SimulationEvent.STOPPED);
            instance.speedChangeEvent = new SimulationEvent(instance, SimulationEvent.CHANGED_SPEED);
            instance.setPriority(Thread.MIN_PRIORITY);
//            instance.setSpeed(50);
            instance.paused = true;
            instance.start();
        }
    }

    
    /**
     * Returns the simulation if it is initialised. If not, it will return null.
     */
    public static Simulation getInstance()
    {
        return instance;
    }

    public WorldHandler getWorldHandler()
    {
        return worldHandler;
    }

    /**
     * Runs the simulation from the current state.
     * 
     */
    public void run()
    {
        System.gc();
        while (true) {
            maybePause();
            worldHandler.startSequence();
            runOnce();
            delay();
        }
    }

    private synchronized void maybePause()
    {
        if (paused) {
            fireSimulationEvent(stoppedEvent);
            System.gc();
        }
        while (paused) {
            try {
                this.wait();
            }
            catch (InterruptedException e1) {}
            if (!paused) {
                System.gc();
                fireSimulationEvent(startedEvent);
            }

        }
    }

    /**
     * Performs one step in the simulation. Calls act() on all actors.
     * 
     */
    public void runOnce()
    {
        try {
            List objects = null;

            // We need to sync, so that the collection is not changed while
            // copying it ( to avoid ConcurrentModificationException)
            synchronized (worldHandler.getWorldLock()) {
                // We need to copy it, to avoid ConcurrentModificationException
                objects = new ArrayList(worldHandler.getActors());

                for (Iterator i = objects.iterator(); i.hasNext();) {
                    Actor actor = (Actor) i.next();
                    actor.act();
                }
            }
        }
        catch (Throwable t) {
            // If an exception occurs, halt the simulation
            paused = true;
            t.printStackTrace();
        }
        worldHandler.repaint();
    }

    /**
     * Pauses and unpauses the simulation.
     * 
     * @param b
     */
    public synchronized void setPaused(boolean b)
    {
        paused = b;
        notifyAll();
    }

    protected void fireSimulationEvent(SimulationEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SimulationListener.class) {
                ((SimulationListener) listeners[i + 1]).simulationChanged(event);
            }
        }
    }

    /**
     * Add a simulationListener to listen for changes.
     * 
     * @param l
     *            Listener to add
     */
    public void addSimulationListener(SimulationListener l)
    {
        listenerList.add(SimulationListener.class, l);
    }
    
    
    /**
     * Set the speed of the simulation.
     *
     * @param speed  The speed in the range (0..100)
     */
    public void setSpeed(int speed)
    {
        if (speed < 0) {
            speed = 0;
        }
        else if (speed > MAX_SIMULATION_SPEED) {
            speed = MAX_SIMULATION_SPEED;
        }

        if(this.speed != speed) {
            this.speed = speed;
            this.delay = (MAX_SIMULATION_SPEED - speed) * 4;
            fireSimulationEvent(speedChangeEvent);
        }
    }
    
    
    /**
     * Get the current simulation speed.
     * @return  The speed in the range (1..100)
     */
    public int getSpeed()
    {
        return speed;
    }

    
    /**
     * Cause a delay (wait) according to the current speed setting for this
     * simulation.
     */
    private void delay()
    {
        try {
            long timeElapsed = System.currentTimeMillis() - this.lastDelayTime;
            long actualDelay = delay - timeElapsed;
            if (actualDelay > 0) {
                Thread.sleep(delay - timeElapsed);
            } else {
               // Thread.yield();
            }
            this.lastDelayTime = System.currentTimeMillis();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}