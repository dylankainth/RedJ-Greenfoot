import greenfoot.GreenfootObject;
import greenfoot.GreenfootWorld;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

public class Building extends GreenfootWorld
{
    public static final int RESOLUTION = 1;
    public static final int DEFAULT_LIFTS = 3;
    public static final int DEFAULT_STORIES = 6;
    
    private static Random random = new Random();

    public static Random getRandomizer()
    {
        return random;
    }
    
    public static boolean chance(int percent)
    {
        return random.nextInt(100) < percent;
    }
    
    // all the floors in the building
    private Floor[] floors;
    
    /**
     * Create a building with default number of lifts and stories
     */
    public Building() 
    {
        this(DEFAULT_STORIES, DEFAULT_LIFTS);
    }

    /**
     * Create a building with specified number of lifts and stories
     */
    public Building(int stories, int lifts)
    {
        //super(120 + lifts * 28, stories * 36 + 20);
        super(240 + lifts * 56, stories * 72 + 40, 1);
        
        //setBackgroundImage("brick.jpg");
        GreenfootImage background = new GreenfootImage("sandstone.jpg");
        background.setTiled(true);
        setBackground(background);
        
        createFloors(stories);
        createLifts(lifts, stories);
    }

    /**
     * Create all the floors in the building.
     */
    private void createFloors(int numberOfFloors)
    {
        floors = new Floor[numberOfFloors];
        for(int i=0; i<numberOfFloors; i++) {
            Button button = new Button();
            addObject(button);
            floors[i] = new Floor(i, button);
            newObject(floors[i], 100, (numberOfFloors-1-i) * 72 + 40);
        }
    }
    
    /**
     * Create all the lifts in the building.
     */
    private void createLifts(int numberOfLifts, int numberOfFloors)
    {
        GreenfootImage background = getBackground();
        background.setColor(new Color(255, 255, 255, 100));

        for(int i=0; i<numberOfLifts; i++) {
            background.fillRect(218 + i * 56, 18, 54, (numberOfFloors)*72 + 2);
            newObject(new Lift(), 240 + i * 56, (numberOfFloors-1)*72 + 40);
        }
    }
    
    private void newObject(GreenfootObject obj, int x, int y)
    {
        addObject(obj);
        obj.setLocation(x, y);
    }
    
    /**
     * Return the floor number at a given screen cell y-coordinate.
     * If this cell is not the exact height of an existing floor, return -1.
     */
    public int getFloorAt(int y)
    {
        for(int i=0; i<floors.length; i++) {
            if(floors[i].getY() == y) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the top floor number.
     */
    public int getTopFloor()
    {
        return floors.length - 1;
    }

    /**
     * Return a random floor number.
     */
    public int getRandomFloor()
    {
        return random.nextInt(floors.length);
    }

}