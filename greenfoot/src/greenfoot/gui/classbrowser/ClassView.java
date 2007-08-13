package greenfoot.gui.classbrowser;

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.LocationTracker;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.gui.classbrowser.role.ActorClassRole;
import greenfoot.gui.classbrowser.role.ClassRole;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.gui.classbrowser.role.WorldClassRole;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JToggleButton;

import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Utility;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassView.java 5155 2007-08-13 02:11:28Z davmac $
 */
public class ClassView extends JToggleButton
    implements Selectable, MouseListener
{
    private final Color classColour = new Color(245, 204, 155);
    private static final Color stripeColor = new Color(152,152,152);

    public static final Color[] shadowColours = { new Color(242, 242, 242), 
                                                  new Color(211, 211, 211),
                                                  new Color(189, 189, 189),
                                                  new Color(83, 83, 83)
                                                };

    private static final int SHADOW = 4;    // thickness of shadow
    private static final int GAP = 2;       // spacing between classes
    private static final int SELECTED_BORDER = 3;

    private GClass gClass;
    private ClassRole role;
    private ClassBrowser classBrowser;
    private JPopupMenu popupMenu;
    private String superclass; //Holds the current superclass. Used to determine wether the superclass has changed.
    
    /** Whether this class is a "core" class (can't be removed or have image set) */
    private boolean coreClass;
    
    /**
     * Creates a new ClassView with the role determined from gClass.
     */
    public ClassView(GClass gClass)
    {
        coreClass = false;
        init(gClass);
    }
    
    /**
     * Creates a new ClassView with the role determined from gClass. The
     * ClassView optionally represents a "core" class which can't be removed
     * from the project.
     */
    public ClassView(GClass gClass, boolean coreClass)
    {
        this.coreClass = coreClass;
        init(gClass);
    }
    
    /**
     * Check whether this class is a core class (can't be removed or have
     * image set).
     */
    public boolean isCoreClass()
    {
        return coreClass;
    }
    
    /**
     * Updates this ClassView to reflect a change in super class.
     * 
     * <p>
     * Will also update the UI, but can be called from any thread.
     * 
     */
    public void updateSuperClass()
    {        
    	EventQueue.invokeLater(new Runnable() {
    		public void run() {
    	        if (gClass.getSuperclassGuess() == superclass ||  (gClass != null && gClass.getSuperclassGuess().equals(superclass) )) {
    	            // If super class has not changed, we do not want to update
    	            // anything.
    	            return;
    	        }
    	        else {
    	            superclass = gClass.getSuperclassGuess();
    	        }

    	        try {
    	        	ClassRole newRole = determineRole(gClass.getPackage().getProject());
    	        	setRole(newRole);
    	        }
    	        catch (ProjectNotOpenException pnoe) {}
    	        catch (RemoteException re) { re.printStackTrace(); }

    	        if (classBrowser != null) {
    	            // If we are in a classBrowser, tell it to update this
    	            // classview.
    	            classBrowser.consolidateLayout(ClassView.this);
    	        }
    	        update();
    	        if (classBrowser != null) {
    	        	classBrowser.updateLayout();
    	        }
    		}
    	});
    }

    /**
     * Determines the role of this class based on the backing GClass.
     * 
     * @param gClass
     * @return
     */
    private ClassRole determineRole(GProject project)
    {
        ClassRole classRole = null;
        if (gClass.isActorClass()) {
            classRole = new ActorClassRole(project);
        }
        else if (gClass.isWorldClass()) {
            classRole = new WorldClassRole(project);
        }
        else if (gClass.isActorSubclass()) {
            classRole = new ActorClassRole(project);
        }
        else if (gClass.isWorldSubclass()) {
            classRole = new WorldClassRole(project);
        }
        else {
            // everything else
            classRole = NormalClassRole.getInstance();
        }
        return classRole;
    }

    private void init(GClass gClass)
    {
        this.gClass = gClass;
        gClass.setClassView(this);

        superclass = gClass.getSuperclassGuess();
        
        this.addMouseListener(this);
        this.setBorder(BorderFactory.createEmptyBorder(7, 8, 10, 11)); // top,left,bottom,right
        Font font = getFont();
        font = font.deriveFont(13.0f);
        this.setFont(font);
        // this.setFont(PrefMgr.getTargetFont());

        setContentAreaFilled(false);
        setFocusPainted(false);
        
        try {
        	setRole(determineRole(gClass.getPackage().getProject()));
        	update();
        }
        catch (ProjectNotOpenException pnoe) {}
        catch (RemoteException re) { re.printStackTrace(); }
    }

        
    /**
     * Return the real Java class that this class view represents.
     */
    public Class getRealClass()
    {
        return gClass.getJavaClass();
    }

    void setClassBrowser(ClassBrowser classBrowser)
    {
        this.classBrowser = classBrowser;
        createPopupMenu();
    }

    public GClass getGClass()
    {
        return gClass;
    }

    private void createPopupMenu()
    {
        popupMenu = role.createPopupMenu(classBrowser, this);
        popupMenu.setInvoker(this);
    }

    /**
     * Sets the role of this ClassLabel. Does not update UI.
     * 
     * @param role
     */
    private void setRole(ClassRole role)
    {
        this.role = role;
    }

    /**
     * Rebuild the UI of this ClassView from scratch.
     * <p>
     * Must be called from event thread, unless it is the first time it is
     * called.
     */
    private void update()
    {
        clearUI();
        role.buildUI(this, gClass);
        JRootPane rootPane = getRootPane();
        if(rootPane != null) {
            getRootPane().revalidate();
        }
    }

    /**
     *  Clears the UI for this ClassView
     */
    private void clearUI()
    {
        this.removeAll();
    }

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics g)
    {
        // Sometimes there are still paint events pending when the gclass
        // has been removed. We can check for that here.
        if (gClass != null) {
            drawBackground(g);
            super.paintComponent(g);
            
            drawShadow((Graphics2D) g);
            drawBorders((Graphics2D) g);
        }
    }

    
    private void drawBackground(Graphics g)
    {
        int height = getHeight() - SHADOW - GAP;
        int width = getWidth() - 4;

        g.setColor(classColour);
        g.fillRect(0, GAP, width, height);
        
        if(!gClass.isCompiled()) {
            g.setColor(stripeColor);
            Utility.stripeRect(g, 0, GAP, width, height, 8, 3);

            g.setColor(classColour);
            g.fillRect(7, GAP+7, width-14, height-14);
        }
    }
    
    
    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    protected void drawShadow(Graphics2D g)
    {
        int height = getHeight() - SHADOW;
        int width = getWidth() - 4;
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width + 4, GAP);   // blank for gap above class
        g.fillRect(0, height, 6, height + SHADOW);
        g.fillRect(width, 0, width + 3, 10);
        
        // colorchange is expensive on mac, so draworder is by color, not position
        g.setColor(shadowColours[3]);
        g.drawLine(3, height, width, height);//bottom

        g.setColor(shadowColours[2]);
        g.drawLine(4, height + 1, width, height + 1);//bottom
        g.drawLine(width + 1, height + 2, width + 1, 3 + GAP);//right

        g.setColor(shadowColours[1]);
        g.drawLine(5, height + 2, width + 1, height + 2);//bottom
        g.drawLine(width + 2, height + 3, width + 2, 4 + GAP);//right

        g.setColor(shadowColours[0]);
        g.drawLine(6, height + 3, width + 2, height + 3); //bottom
        g.drawLine(width + 3, height + 3, width + 3, 5 + GAP); // right
    }

    /**
     * Draw the borders of this target.
     */
    protected void drawBorders(Graphics2D g)
    {
        g.setColor(Color.BLACK);
        int thickness = isSelected() ? SELECTED_BORDER : 1;
        Utility.drawThickRect(g, 0, GAP, getWidth() - 4, getHeight() - SHADOW - GAP - 1, thickness);
    }

    /**
     *  
     */
    public ClassRole getRole()
    {
        return role;
    }

    public String getQualifiedClassName()
    {
        return gClass.getQualifiedName();
    }

    public String getClassName()
    {
        return gClass.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.gui.classbrowser.Selectable#select()
     */
    public void select()
    {
        this.setSelected(true);
        fireSelectionChangeEvent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.gui.classbrowser.Selectable#deselect()
     */
    public boolean deselect()
    {
        if (isSelected()) {
        	setSelected(false);
            fireSelectionChangeEvent();
            return true;
        }
        return false;
    }

    protected void fireSelectionChangeEvent()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).selectionChange(this);
            }
        }
    }

    /**
     * Add a changeListener to listen for changes.
     * 
     * @param l
     *            Listener to add
     */
    public void addSelectionChangeListener(SelectionListener l)
    {
        listenerList.add(SelectionListener.class, l);
    }
    
    /**
     * Remove a changeListener.
     * 
     * @param l
     *            Listener to remove
     */
    public void removeSelectionChangeListener(SelectionListener l)
    {
        listenerList.remove(SelectionListener.class, l);
    }

    /**
     * Creates an instance of this class. The default constructor is used. This
     * method is used for creating instances when clicking on the world.
     * 
     * @return The Object that has been created
     */
    public Object createInstance()
    {
        Class<?> realClass = getRealClass();
        try {
            if (realClass == null) {
                return null;
            }
            Constructor constructor = realClass.getConstructor(new Class[]{});

            Object newObject = constructor.newInstance(new Object[]{});
            ActorInstantiationListener invocationListener = GreenfootMain.getInstance().getInvocationListener();
            if(invocationListener != null) {
                invocationListener.localObjectCreated(newObject, LocationTracker.instance().getMouseButtonEvent());
            }
            return newObject;
        }
        catch (LinkageError le) {
            // This could be NoClassDefFound or similar. It really means the
            // class needs to be recompiled.
        }
        catch (NoSuchMethodException e) {
            // This might happen if there is no default constructor
            // e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                // Filter the stack trace. Take it from the first point
                // at which code from this class was being executed.
                StackTraceElement [] strace = cause.getStackTrace();
                for (int i = strace.length; i > 0; i--) {
                    if (strace[i-1].getClassName().equals(realClass.getName())) {
                        StackTraceElement [] newStrace = new StackTraceElement[i];
                        System.arraycopy(strace, 0, newStrace, 0, i);
                        cause.setStackTrace(newStrace);
                        break;
                    }
                }
                cause.printStackTrace();
            }
            else
                e.printStackTrace();
        }

        return null;

    }

    public GClass createSubclass(String className)
    {
        try {
            //get the default package which is the one containing the user
            // code.
            GPackage pkg = gClass.getPackage().getProject().getDefaultPackage();
            //write the java file as this is required to exist
            File dir = pkg.getProject().getDir();
            File newJavaFile = new File(dir, className + ".java");
            String superClassName = getClassName();            
            GreenfootUtil.createSkeleton(className, superClassName, newJavaFile, role.getTemplateFileName());
            
            GClass newClass = pkg.newClass(className);
            //We know what the superclass should be, so we set it.
            newClass.setSuperclassGuess(this.getQualifiedClassName());
            return newClass;
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (IOException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        return null;

    }
    
    /**
     * Updates this class view by rereading the underlying class' properties.
     */
    public void updateView()
    {
        createPopupMenu();
        update();
    }
    
    /**
     * Notify the class view that the underlying class has changed name.
     * @param oldName  The original name of the class
     */
    public void nameChanged(String oldName)
    {
        updateView();
        classBrowser.renameClass(this, oldName);
    }

    // ----- MouseListener interface -----
    
    /**
     * Mouse-click on this class view. Chek for double-click and handle.
     */
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() > 1 && ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            try {
                gClass.edit();
            }
            catch (ProjectNotOpenException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (PackageNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    /*
     */
    public void mouseEntered(MouseEvent e) { }

    /*
     */
    public void mouseExited(MouseEvent e) { }
    
    /**
     * The mouse was pressed on the component. Do what you have to do.
     */
    public void mousePressed(MouseEvent e)
    {
        select();
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Removes this class.
     * <br>
     * Must be called from the event thread.
     */
    public void remove()
    {
        classBrowser.removeClass(this);
        try {
            gClass.remove();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        gClass = null;
    }

    /**
     * Get the best guess at the super class for this class.
     * 
     */
    public String getSuperclassGuess()
    {
        return gClass.getSuperclassGuess();
    }
}
