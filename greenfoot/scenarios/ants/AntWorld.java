import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.util.Random;
 
public class AntWorld extends World
{
    public static final int RESOLUTION = 1;
    public static final int SIZE = 640; 
  
    private static Random randomizer = new Random();

    public static Random getRandomizer()
    {
        return randomizer; 
    }
    

    public AntWorld() {
        super(SIZE/RESOLUTION,SIZE/RESOLUTION,RESOLUTION);       
        GreenfootImage background = new GreenfootImage("images/sand.jpg");
        setBackground(background);
        scenario3();
    }
    
    public void scenario1()
    {
        addObject(new AntHill(70), SIZE / 2, SIZE / 2);
        addObject(new Food(), SIZE/2, SIZE/2 - 260);
        addObject(new Food(), SIZE/2 + 215, SIZE/2 - 100);
        addObject(new Food(), SIZE/2 + 215, SIZE/2 + 100);
        addObject(new Food(), SIZE/2, SIZE/2 + 260);
        addObject(new Food(), SIZE/2 - 215, SIZE/2 + 100);
        addObject(new Food(), SIZE/2 - 215, SIZE/2 - 100);
    }

    public void scenario2()
    {
        addObject(new AntHill(40), 546, 356);
        addObject(new AntHill(40), 95,267);
        
        addObject(new Food(), 80, 71);
        addObject(new Food(), 291, 56);
        addObject(new Food(), 516, 212);
        addObject(new Food(), 311, 269);
        addObject(new Food(), 318, 299);
        addObject(new Food(), 315, 331);
        addObject(new Food(), 141, 425);
        addObject(new Food(), 378, 547);
        addObject(new Food(), 566, 529);
    }

    public void scenario3()
    {
        addObject(new AntHill(40), 576, 134);
        addObject(new AntHill(40), 59, 512);
        
        addObject(new Food(), 182, 84);
        addObject(new Food(), 39, 308);
        addObject(new Food(), 249, 251);
        addObject(new Food(), 270, 272);
        addObject(new Food(), 291, 253);
        addObject(new Food(), 339, 342);
        addObject(new Food(), 593, 340);
        addObject(new Food(), 487, 565);
    }

    public int getResolution() {
        return RESOLUTION;
    }
}