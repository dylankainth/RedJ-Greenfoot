package greenfoot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;

import javax.swing.ImageIcon;

/**
 * This class represents the object world, which is a 2 dimensional grid of
 * cells. The world can be populated with
 * GreenfootObjects.
 * 
 * @see greenfoot.GreenfootObject
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootWorld.java 3158 2004-11-24 15:29:21Z polle $
 */
public class GreenfootWorld extends Observable
{
    private Map[][] world;
    private List objects = new ArrayList();

    /**
     * Map from classes to the size of the largest object of that class - used
     * for collision checks. <br>
     * 
     * TODO find a better way to do this. It is not fail safe if the object size
     * is changed after obejcts has been added to the world Map from classes to
     * sizes. Used for collision
     */
    private Map objectMaxSizes = new HashMap();

    private static Collection emptyCollection = new Vector();

    private Color backgroundColor = Color.WHITE;

    /** Image painted in the background. */
    private Image backgroundImage;
    private boolean tiledBackground;

    /** A canvas that can be used for additional drawing. */
    private Image canvasImage;


    private int delay = 500;

    /** for timing the animation */
    private long lastDelay;

    /**
     * Create a new world with the given size.
     * 
     * @param worldWidth
     *            The width of the world. 
     * @param worldHeight
     *            The height of the world.
     */
    public GreenfootWorld(int worldWidth, int worldHeight)
    {
        setSize(worldWidth, worldHeight);        
    }

    /**
     * Sets a new background color.
     * 
     * @param color
     *            The new background color
     */
    final public void setBackgroundColor(Color color)
    {
        this.backgroundColor = color;
        setChanged();
        notifyObservers(color);
    }

    /**
     * Sets the backgroundimage of the world
     * 
     * @param image
     *            The image
     */
    final public void setBackgroundImage(Image image)
    {
        backgroundImage = image;
        setChanged();
        notifyObservers(image);
    }

    /**
     * Tiles the backgroundimage to fill up the background.
     * 
     * @see #setBackgroundImage(Image)
     * @see #setBackgroundImage(String)
     * @param tiled
     *            Whether it should tile the background or not.
     */
    public void setTiledBackground(boolean tiled)
    {
        tiledBackground = tiled;
        update();
    }

    /**
     * Returns true if the background image is tiled. Otrherwise false is
     * returned.
     * 
     * @return Wherher the background image is tilled.
     */
    public boolean isTiledBackground()
    {
        return tiledBackground;
    }

    /**
     * Gets the background image
     * 
     * @return The background image
     */
    public Image getBackgroundImage()
    {
        return backgroundImage;
    }

    /**
     * Sets the backgroundimage of the world.
     * 
     * @see #setTiledBackground(boolean)
     * @see #setBackgroundImage(Image)
     * @param filename
     *            The file containing the image
     */
    final public void setBackgroundImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        ImageIcon imageIcon = new ImageIcon(imageURL);
        setBackgroundImage(imageIcon.getImage());
        update();
    }

    /**
     * Gets the width of the world.
     * 
     * @return Number of pixels in the x-direction
     */
    public int getWidth()
    {
        return world.length;
    }

    /**
     * Gets the height of the world.
     * 
     * @return Number of pixels in the y-direction
     */
    public int getHeight()
    {
        return world[0].length;
    }

    /**
     * Sets the size of the world. <br>
     * This will remove all objects from the world. TODO Maybe it shouldn't!
     */
    public void setSize(int width, int height)
    {
        world = new Map[width][height];
        canvasImage = null;

        update();
    }

    /**
     * Adds a GreenfootObject to the world.
     * 
     * If the coordinates of the objects is outside the worlds bounds, an
     * exception is thrown.
     * 
     * @param thing
     *            The new object to add.
     */
    public synchronized void addObject(GreenfootObject thing)
        throws ArrayIndexOutOfBoundsException
    {
        if (thing.getX() >= getWidth()) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() >= getHeight()) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }
        if (thing.getX() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }

        if (!objects.contains(thing)) {

            HashMap map = (HashMap) world[thing.getX()][thing.getY()];
            if (map == null) {
                map = new HashMap();
                world[thing.getX()][thing.getY()] = map;
            }
            Class clazz = thing.getClass();
            List list = (List) map.get(clazz);
            if (list == null) {
                list = new ArrayList();
                map.put(clazz, list);
            }
            list.add(thing);
            thing.setWorld(this);
            objects.add(thing);

            updateMaxSize(thing);

            update();
        }
    }

    /**
     * Updates the map of maximum object sizes with the given object (if
     * necessary).
     *  
     */
    private void updateMaxSize(GreenfootObject thing)
    {
        Class clazz = thing.getClass();
        Integer maxSize = (Integer) objectMaxSizes.get(clazz);
        int height = thing.getImage().getIconHeight();
        int width = thing.getImage().getIconWidth();
        int diag = (int) Math.sqrt(width * width + height * height);
        if (maxSize == null || maxSize.intValue() < diag) {
            objectMaxSizes.put(clazz, new Integer(diag));
        }
    }

    
    /**
     * @param x
     * @param y
     * @return
     */
    private Collection getObjectsWithLocation(int x, int y)
    {
        Map map = (Map) world[x][y];
        if (map != null) {
            Collection values = map.values();
            Collection list = new ArrayList();
            for (Iterator iter = values.iterator(); iter.hasNext();) {
                List element = (List) iter.next();
                list.addAll(element);
            }
            return list;
        }
        else {
            return emptyCollection;
        }
    }
    
    /**
     * Gets all the objects of class cls (and subclasses) at the given pixel location
     */
    public Collection getObjectsAt(int x, int y, Class cls)
    {
        List objectsThere = new ArrayList();
        Collection objectsAtCell = getObjectsAt(x, y);
        for (Iterator iter = objectsAtCell.iterator(); iter.hasNext();) {
            GreenfootObject go = (GreenfootObject) iter.next();
            if(cls.isInstance(go)) {
                 objectsThere.add(go);
            }
        }
        return objectsThere;
    }

    /**
     * Returns all objects at the given location.
     */
    public Collection getObjectsAt(int x, int y)
    {      
            Collection maxSizes = objectMaxSizes.values();
            int maxSize = 0;
            for (Iterator iter = maxSizes.iterator(); iter.hasNext();) {
                Integer element = (Integer) iter.next();
                if (element.intValue() > maxSize) {
                    maxSize = element.intValue();
                }
            }

            List objectsThere = new ArrayList();
            int xStart = (x - maxSize) + 1;
            int yStart = (y - maxSize) + 1;
            if (xStart < 0) {
                xStart = 0;
            }
            if (yStart < 0) {
                yStart = 0;
            }
            if (x >= getWidth()) {
                x = getWidth() - 1;
            }
            if (y >= getHeight()) {
                y = getHeight() - 1;
            }

            for (int xi = xStart; xi <= x; xi++) {
                for (int yi = yStart; yi <= y; yi++) {
                    Map map = world[xi][yi];
                    if (map != null) {
                        Collection list = getObjectsWithLocation(xi, yi);
                        for (Iterator iter = Collections.unmodifiableCollection(list).iterator(); iter.hasNext();) {
                            GreenfootObject go = (GreenfootObject) iter.next();
                            if (go.contains(x - xi, y - yi)) {
                                objectsThere.add(go);
                            }
                        }
                    }
                }
            }
            return objectsThere;
    }

   
    /**
     * Removes the object from the world.
     * 
     * @param object
     *            the object to remove
     */
    public synchronized void removeObject(GreenfootObject object)
    {
        Map map = world[object.getX()][object.getY()];
        if (map != null) {
            List list = (List) map.get(object.getClass());
            if (list != null) {
                list.remove(object);
                object.setWorld(null);
            }
        }
        objects.remove(object);
        update();
    }

    /**
     * Provides an Iterator to all the things in the world.
     *  
     */
    public synchronized Iterator getObjects()
    {
        //TODO: Make sure that the iterator returns things in the correct
        // paint-order (whatever that is)
        List c = new ArrayList();
        c.addAll(objects);
        return c.iterator();
    }

    /**
     * Updates the location of the object in the world.
     * 
     * 
     * @param object
     *            The object which should be updated
     * @param oldX
     *            The old X location of the object
     * @param oldY
     *            The old Y location of the object
     */
    protected void updateLocation(GreenfootObject object, int oldX, int oldY)
    {
        Map map = world[oldX][oldY];
        Class clazz = object.getClass();
        if (map != null) {
            List list = (List) map.get(object.getClass());
            if (list != null) {
                list.remove(object);
            }
        }

        map = world[object.getX()][object.getY()];
        if (map == null) {
            map = new HashMap();
            world[object.getX()][object.getY()] = map;
        }
        List list = (List) map.get(clazz);
        if (list == null) {
            list = new ArrayList();
            map.put(clazz, list);
        }
        list.add(object);
        update();
    }   

    /**
     * Sets the delay that is used in the animation loop
     * 
     * @param millis
     *            The delay in ms
     */
    public void setDelay(int millis)
    {
        this.delay = millis;
        update();
    }

    /**
     * Returns the delay.
     * 
     * @return The delay in ms
     */
    public int getDelay()
    {
        return delay;
    }

    /**
     * Pauses for a while
     */
    public void delay()
    {
        //TODO this functionality shouldn't really be here. Should it be
        // available to the user at all?
        try {
            long timeElapsed = System.currentTimeMillis() - this.lastDelay;
            long actualDelay = delay - timeElapsed;
            if (actualDelay > 0) {
                Thread.sleep(delay - timeElapsed);
            }
            this.lastDelay = System.currentTimeMillis();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the background color
     * 
     * @return The current background color
     */
    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    /**
     * Returns a canvas that can be used to paint custom stuff. <br>
     * This will be painted on top of the background and below the
     * GreenfootObjects. <br>
     * update() must be called for changes to take effect.
     * 
     * @see #update()
     * @return A graphics2D that can be used for painting.
     */
    public Graphics2D getCanvas()
    {
        if (canvasImage == null) {
            canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }
        return (Graphics2D) canvasImage.getGraphics();
    }

    /**
     * Used by the WorldCanvas.
     * 
     * @return
     */
    Image getCanvasImage()
    {
        return canvasImage;
    }

    /**
     * Refreshes the world. <br>
     * Should be called to see the changes after painting on the graphics
     * 
     * @see #getCanvas()
     * @see #getCanvas(int, int)
     */
    public void update()
    {
        setChanged();
        notifyObservers();
    }

    /**
     * Gets a canvas that can be used to draw on the world. The origo of the
     * canvas will be the given coordinates.
     * 
     * @see #update()
     * @return A graphics2D that can be used for painting.
     */
    public Graphics2D getCanvas(int x, int y)
    {       
        Graphics2D g = getCanvas();
        g.translate(x, y);
        return g;
    }
}