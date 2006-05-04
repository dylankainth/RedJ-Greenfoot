package rmiextension;

import greenfoot.core.GreenfootMain;
import greenfoot.core.GreenfootLauncher;
import greenfoot.core.ProjectProperties;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import bluej.extensions.BPackage;
import bluej.extensions.BlueJ;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.event.PackageListener;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * The ProjectManager is on the BlueJ-VM. It monitors pacakage events from BlueJ
 * and launches the greenfoot project in the greenfoot-VM.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ProjectManager.java 4088 2006-05-04 20:36:05Z mik $
 */
public class ProjectManager
    implements PackageListener
{
    /** Singleton instance */
    private static ProjectManager instance;

    /** List to keep track of which projects has been opened */
    private List<BPackage> openedPackages = new ArrayList<BPackage>();

    /** List to keep track of which projects are int the process of being created */
    private List<File> projectsInCreation = new ArrayList<File>();

    /** The class that will be instantiated in the greenfoot VM to launch the project */
    private String launchClass = GreenfootLauncher.class.getName();

    private static BlueJ bluej;

    private ProjectManager()
    {}

    /**
     * Get the singleton instance. Make sure it is initialised first.
     * 
     * @see #init(BlueJ)
     */
    public static ProjectManager instance()
    {
        if (bluej == null) {
            throw new IllegalStateException("Projectmanager has not been initialised.");
        }
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }

    /**
     * Initialise. Must be called before the instance is accessed.
     */
    public static void init(BlueJ bluej)
    {
        ProjectManager.bluej = bluej;
    }

    /**
     * Launch the project in the greenfoot-VM if it is a proper greenfoot
     * project.
     */
    private void launchProject(final Project project)
    {
        if (!ProjectManager.instance().isProjectOpen(project)) {
            File projectDir = new File(project.getDir());
            boolean versionOK = checkVersion(projectDir);
            if (versionOK) {
                ObjectBench.createObject(project, launchClass, "launcher");
            }
            else {
                //If this was the only open project, open the startup project instead.
                if (bluej.getOpenProjects().length == 1) {
                    ((PkgMgrFrame) bluej.getCurrentFrame()).doClose(true);
                    File startupProject = new File(bluej.getSystemLibDir(), "startupProject");
                    bluej.openProject(startupProject);
                }
            }
        }
    }

    /**
     * Launches the RMI client in the greenfoot-VM.
     * 
     */
    private void launchRmiClient(Project project)
    {
        ObjectBench.createObject(project, BlueJRMIClient.class.getName(), "blueJRMIClient", new String[]{
                project.getDir(), project.getName()});
    }

    /**
     * Handles the check of the project version. It will notify the user if the
     * project has to be updated.
     * 
     * @param projectDir Directory of the project.
     * @return true if the project can be opened.
     */
    private boolean checkVersion(File projectDir)
    {
        if(isNewProject(projectDir)) {
            ProjectProperties newProperties = new ProjectProperties(projectDir);
            newProperties.setApiVersion();
            newProperties.save();
        }
        
        boolean doOpen = false;
        try {
            JFrame frame = new JFrame("NONE");
            frame.setUndecorated(true);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation(screenSize.width / 2, screenSize.height / 2);
            frame.setVisible(true);
            doOpen = GreenfootMain.updateApi(projectDir, bluej.getSystemLibDir(), frame);
            frame.dispose();
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return doOpen;
    }

    /**
     * Checks if this is a project that is being created for the first time
     * @param projectDir
     * @return
     */
    private boolean isNewProject(File projectDir)
    {
        return projectsInCreation.contains(projectDir);        
    }
    
    /**
     * Flags that this project is in the process of being created.
     */
    public void addNewProject(File projectDir)
    {
        projectsInCreation.add(projectDir);
    }

    /**
     * Flags that this project is no longer in the process of being created.
     */
    public void removeNewProject(File projectDir)
    {
        projectsInCreation.remove(projectDir);
    }

    /**
     * Whether this project is currently open or not.
     */
    private boolean isProjectOpen(Project prj)
    {
        boolean projectIsOpen = false;
        File prjFile = null;
        try {
            prjFile = prj.getPackage().getProject().getDir();
        }
        catch (ProjectNotOpenException e1) {
            e1.printStackTrace();
        }
        for (int i = 0; i < openedPackages.size(); i++) {
            BPackage openPkg = openedPackages.get(i);

            File openPrj = null;
            try {
                //  TODO package could be null if it is removed inbetween. should
                // synchronize the access to the list.
                //can throw ProjectNotOpenException
                openPrj = openPkg.getProject().getDir();
            }
            catch (ProjectNotOpenException e2) {
                //e2.printStackTrace();
            }

            if (openPrj != null && prjFile != null && openPrj.equals(prjFile)) {
                projectIsOpen = true;
            }
        }
        return projectIsOpen;
    }

    //=================================================================
    //bluej.extensions.event.PackageListener implementation
    //=================================================================

    /**
     * 
     * @see bluej.extensions.event.PackageListener#packageOpened(bluej.extensions.event.PackageEvent)
     */
    public void packageOpened(PackageEvent event)
    {
        try {
            BPackage pkg = event.getPackage();
            if (pkg.getName().equals("") || pkg.getProject().getName().equals("startupProject")) {
                Project project = new Project(pkg);
                launchRmiClient(project);
                launchProject(project);
            }
        }
        catch (PackageNotFoundException pnfe) {}
        catch (ProjectNotOpenException pnoe) {}
        openedPackages.add(event.getPackage());
    }

    /**
     * 
     * @see bluej.extensions.event.PackageListener#packageClosing(bluej.extensions.event.PackageEvent)
     */
    public void packageClosing(PackageEvent event)
    {
        openedPackages.remove(event.getPackage());
    }
}