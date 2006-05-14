import greenfoot.World;
import greenfoot.Actor;

import java.awt.Color;

public class TurtleWorld extends World
{
    /**
     * Creates a new world with 800x600 cells and
     * with a cell size of 1x1 pixels
     */
    public TurtleWorld() {
        super(800,600, 1);
        getBackground().setColor(Color.WHITE);
        getBackground().fill();
    }
}