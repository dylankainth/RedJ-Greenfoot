import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.Image;

import java.awt.Color;
import java.util.Random;

public class AntWorld extends GreenfootWorld
{
    public static final int RESOLUTION = 1;
    public static final int SIZE = 640;
    
    private static Random randomizer = new Random();

    public static Random getRandomizer()
    {
        return randomizer;
    }
    

    public AntWorld() {
        super(SIZE,SIZE);       
        Image background = new Image("sand.jpg");
        background.setTiled(true);
        setBackground(background);
    }
    
    public void scenario1()
    {
        newObject(new AntHill(40), SIZE - 17, SIZE - 20);
        newObject(new Food(), 73, 21);
        newObject(new Food(), 20, 19);
        newObject(new Food(), 36, 86);
    }

    public void scenario2()
    {
        newObject(new AntHill(20), 56, 85);
        newObject(new AntHill(20), 7, 6);
        newObject(new AntHill(20), 82, 67);
        
        newObject(new Food(), 41, 4);
        newObject(new Food(), 40, 22);
        newObject(new Food(), 33, 36);
        newObject(new Food(), 20, 42);
        newObject(new Food(), 5, 43);
    }

    private void newObject(GreenfootObject obj, int x, int y)
    {
        obj.setLocation(x, y);
        addObject(obj);
    }
}