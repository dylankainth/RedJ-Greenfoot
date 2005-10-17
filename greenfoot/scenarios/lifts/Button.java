import greenfoot.GreenfootObject;
import greenfoot.GreenfootImage;

import javax.swing.ImageIcon;

public class Button extends GreenfootObject
{
    public static final int UP = 0;
    public static final int DOWN = 1;
    
    private GreenfootImage imageNone;
    private GreenfootImage imageUp;
    private GreenfootImage imageDown;
    private GreenfootImage imageUpDown;
    
    private boolean up;
    private boolean down;
    
    public Button()
    {
        imageUpDown = new GreenfootImage("button-up-down.jpg");
        imageUp = new GreenfootImage("button-up.jpg");
        imageDown =new GreenfootImage("button-down.jpg");
        imageNone = new GreenfootImage("button.jpg");
        
        setImage(imageNone);
        
        up = false;
        down = false;
    }

    public void act()
    {
        //here you can create the behaviour of your object
    }

    public void press(int direction)
    {
        change(direction, true);
    }
    
    public void clear(int direction)
    {
        change(direction, false);
    }
    
    public void change(int direction, boolean onOff)
    {
        if(direction == UP) {
            up = onOff;
        }
        else if(direction == DOWN) {
            down = onOff;
        }
        updateImage();
    }
    
    private void updateImage()
    {
        if(up && down) 
            setImage(imageUpDown);
        else if(up)
            setImage(imageUp);
        else if(down)
            setImage(imageDown);
        else
            setImage(imageNone);
    }
}