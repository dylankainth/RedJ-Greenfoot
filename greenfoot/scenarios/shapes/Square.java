import greenfoot.GreenfootObject;
import greenfoot.Image;
import greenfoot.Utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;


/**
 * A square
 */
public class Square extends GreenfootObject{
    private Color color;
    private int size;
    
    /**
     * Creates a black square
     */
    public Square()
    {
        size=32;
        color=Color.BLACK;
        draw();      
    }
    
    /**
     * Draws the square
     */
    public void draw()
    {
        Image im = new Image(size, size);
        im.setColor(color);
        im.fillRect(0, 0, size, size);
        setImage(im);
        Utilities.repaint();
    }
    
    /**
     * Does nothing.
     */    
    public void act()
    {
        //here you can create the behaviour of your object
    }
    
    
    /**
     * Move the square a few pixels to the right.
     */
    public void moveRight()
    {
        moveHorizontal(20);
    }
    
    /**
     * Move the square a few pixels to the left.
     */
    public void moveLeft()
    {
        moveHorizontal(-20);
    }
    
    /**
     * Move the square a few pixels up.
     */
    public void moveUp()
    {
        moveVertical(-20);
    }
    
    /**
     * Move the square a few pixels down.
     */
    public void moveDown()
    {
        moveVertical(20);
    }
    
    /**
     * Move the square horizontally by 'distance' pixels.
     */
    public void moveHorizontal(int distance)
    {
        setLocation(getX()+distance, getY());
        Utilities.repaint();
    }
    
    /**
     * Move the square vertically by 'distance' pixels.
     */
    public void moveVertical(int distance)
    {
        setLocation(getX(), getY()+distance);
        Utilities.repaint();
    }
    
    /**
     * Slowly move the square horizontally by 'distance' pixels.
     */
    public void slowMoveHorizontal(int distance)
    {
        int delta;
        
        if(distance < 0) 
        {
            delta = -1;
            distance = -distance;
        }
        else 
        {
            delta = 1;
        }
        
        for(int i = 0; i < distance; i++)
        {
            setLocation(getX()+delta, getY());
            Utilities.delay();
        }
    }
    
    /**
     * Slowly move the square vertically by 'distance' pixels.
     */
    public void slowMoveVertical(int distance)
    {
        int delta;
        
        if(distance < 0) 
        {
            delta = -1;
            distance = -distance;
        }
        else 
        {
            delta = 1;
        }
        
        for(int i = 0; i < distance; i++)
        {
            setLocation(getX(), getY()+delta);
            Utilities.delay();
        }
    }
    
    /**
     * Change the size to the new size (in pixels). Size must be >= 0.
     */
    public void changeSize(int newSize)
    {   
        size=newSize;
        draw();
    }
    
    /**
     * Change the color. 
     */
    public void changeColor(Color newColor)
    {
        color = newColor;
        draw();
    }
    
}