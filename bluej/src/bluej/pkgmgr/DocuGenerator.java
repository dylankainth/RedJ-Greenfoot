package bluej.pkgmgr;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.pkgmgr.Package;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * This class handles documentation generation from inside BlueJ.
 * Documentation can be generated for a whole project or for a single class.
 * For each Project instance there should be one instance of DocuGenerator
 * that takes care of project documentation. Project documentation is written
 * into a directory in the project directory.
 * As the documentation for a single class serves merely as a preview option,
 * it is generated in a temporary directory.
 *
 * Information in this class belongs to one of three categories: <BR>
 * <BR>
 * Static information - valid for all runs of a generator (e.g. the name
 * (not the path!) of the directory where project documentation is written
 * to).<BR>
 * <BR>
 * Instance information - valid for all generator runs for one project (e.g.
 * the path of the project directory). <BR>
 * <BR>
 * Run-specific information - generated on each run (e.g. the names of the
 * targets, as these might change between several runs). <BR>
 * <BR>
 * Each of these categories can again be divided into tool-dependent (e.g.
 * the name of the documentation generating tool) and tool-independent.
 *
 * @author  Axel Schmolitzky
 * @version $ $
 */
public class DocuGenerator
{
    // static fields - tool-independent
    /** The name of the directory where project documentation is written to. */
    private static String docDirName =
                                Config.getPropString("doctool.outputdir");
    /** The directory where temporary documentation for a single class is
     *  written to. This name is unique for every instantiation of BlueJ.*/
    private static File docTempDir;

    // static fields - tool-dependent
    /** The name (including path) of the documentation tool used. */
    private static String docCommand = Config.getPropString("doctool.command");

    /** javadoc parameters for all runs: include author and version 
     * information, do not generate information about deprecated features,
     * consider only package, protected, and public classes and members,
     * include bottom line.
     */
    private static String fixedJavadocParams = " -author -version"
          + " -nodeprecated -package"
        + " -bottom <small><em>Generated&nbsp;by&nbsp;BlueJ</em></small>";
    /** javadoc parameters for preview runs: do not generate an index,
     * a tree, a help.
     */
    private static String tmpJavadocParams = " -noindex -notree -nohelp -nonavbar";

    /** The project this generator belongs to. */
    private static Project project;
    /** The project directory. */
    private static File projectDir;
    /** the path of the project directory, the root for all sources. */
    private static String projectDirPath;
    /** the directory where documentation is written to. */
    private static File docDir;
    /** the path of the directory where documentation is written to. */
    private static String docDirPath;

    /* -------------- end of static field declarations ----------------- */

    /**
     * Generate documentation for the class in file 'filename'. The
     * documentation is generated in a temporary directory. If the
     * generation was successful the result will be displayed in a web browser.
     * @param filename the fully qualified filename of the class to be
     * documented.
     * @return the path of the HTML file that will be generated
     */
    public static void generateClassDocu(String filename)
    {
        //File docDir = getDocTempDir();  use project docDir instead
        if (docDir == null)
                BlueJEvent.raiseEvent(BlueJEvent.DOCU_ABORTED, null);

        // test whether the documentation directory is accessible.
        String docDirStatus = testDocDir();
        if (docDirStatus != "")
                BlueJEvent.raiseEvent(BlueJEvent.DOCU_ABORTED, null);

        // build the call string
        String javadocCall = docCommand + fixedJavadocParams + tmpJavadocParams
            + " -d " + docDir.getPath() + " " + filename;

        // build the path for the result to be shown
        File htmlFile = new File(getDocuPath(filename));
        File logFile = new File(docDir, "logfile");

        generateDoc(javadocCall, htmlFile, logFile, false);
    }

    /**
     * For a given filename, return the path where the html documentation
     * file for that file would be generated.
     */
    public static String getDocuPath(String filename)
    {
        if(filename.startsWith(projectDirPath))
            filename = filename.substring(projectDirPath.length());
        if (filename.endsWith(".java"))
            filename = filename.substring(0, filename.indexOf(".java"));
        return docDirPath + filename + ".html";
    }

    /**
     * Create a temporary directory. The name of the directory is unique for
     * every BlueJ instantiation.
     * @return the file instance if successful, null otherwise.
     */
    private static File getDocTempDir()
    {
        if (docTempDir == null) {  // first time called, create File instance
            try {
                docTempDir = File.createTempFile("bluej","tmp"); 
            }
            catch (IOException e) {
                return null;
            }

            docTempDir.delete(); // it's a file, remove it first to allow mkdir
            docTempDir.mkdir();
        }
        else {  // not the first call, remove previous content
            FileUtility.deleteDir(docTempDir);
            docTempDir.mkdir();
        }
            
        return docTempDir;
    }

    /**
     * Creates a separate thread that starts the external call for faster
     * return to the GUI. If the call was successful the URL given in 'url'
     * will be shown in a web browser.
     * @param call the call to the documentation generating tool.
     * @param url the URL to be shown after successful completion.
     */
    private static void generateDoc(String call, File result, 
                                    File log, boolean openBrowser)
    {
        // start the call in a separate thread to allow fast return to GUI.
        Thread starterThread = new Thread(
                        new docuRunStarter(call, result, log, openBrowser));
        starterThread.setPriority(Thread.MIN_PRIORITY);
        starterThread.start();
        BlueJEvent.raiseEvent(BlueJEvent.GENERATING_DOCU, null);
    }


    /**
     * This class enables to run the external call for a documentation
     * generation in a different thread. An instance of this class gets
     * the string that constitutes the external call as a constructor
     * parameter. The second constructor parameter is the name of the
     * HTML file that should be opened by a web browser if the documentation
     * generation was successful.
     */
    private static class docuRunStarter implements Runnable
    {
        private String docuCall;
        private File showFile;
        private File logFile;
        private boolean openBrowser;

        public docuRunStarter(String call, File result, File log, 
                              boolean browse)
        {
            docuCall = call;
            showFile = result;
            logFile = log;
            openBrowser = browse;
        }

        /**
         * Perform the call that was passed in as a constructor parameter.
         * If this call was successful let the result be shown in a browser.
         */
        public void run()
        {
            Process docuRun;
            try {
//                 Debug.message(docuCall);
                OutputStream logStream = new FileOutputStream(logFile);
//                 Writer logWriter = new OutputStreamWriter(logStream);
                PrintWriter logWriter = new PrintWriter(logStream,true);
                logWriter.println(docuCall);
                logWriter.flush();
                docuRun = Runtime.getRuntime().exec(docuCall);

                // because we don't know what comes first we have to start
                // two threads that consume both the standard and the error
                // output of the external process. The output is appended to
                // the log file.
                EchoThread outEcho = new EchoThread(docuRun.getInputStream(),
                                                    logStream);
                EchoThread errEcho = new EchoThread(docuRun.getErrorStream(),
                                                    logStream);
                outEcho.start();
                errEcho.start();
                try {
                    docuRun.waitFor();
                    outEcho.join();
                    errEcho.join();
                }
                catch(InterruptedException e) {
                    System.err.println("Interrupted waiting for process");
                }

                if (docuRun.exitValue() == 0) {
                    BlueJEvent.raiseEvent(BlueJEvent.DOCU_GENERATED, null);
                    if (!showFile.exists()) {
                        Debug.message("showfile does not exist - searching");
                        showFile=FileUtility.findFile(showFile.getParentFile(),
                                                      showFile.getName());
                    }
                    if(openBrowser)
                        Utility.openWebBrowser(showFile.getPath());
                }
                else {
                    BlueJEvent.raiseEvent(BlueJEvent.DOCU_ABORTED, null);
                    DialogManager.showMessageWithText(null,
                                                      "doctool-error",
                                                      logFile.getPath());
                }
            }
            catch (IOException exc) {
                DialogManager.showMessage(null,"severe-doc-trouble");
            }
        }

        private static class EchoThread extends Thread {
            private InputStream   readStream;
            private OutputStream outStream;
            private byte[] lastBuf;
            public EchoThread(InputStream r,OutputStream out) {
                readStream = r;
                outStream = out;
            }
            public void run() {
                try {
                    byte[] buf = new byte[1000];
                    int n;
                    while((n = readStream.read(buf)) != -1) {
                        outStream.write(buf, 0, n);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /* ------------------- end of static part ------------------- */
    /* ---------------------------------------------------------- */


    //    instance fields

    // tool-dependent instance fields for javadoc
    /** javadoc param for the destination directory. */
    private String destinationParam;
    /** javadoc param for the root directory of the sources. */
    private String sourceParam;
    /** javadoc params for setting window and project title */
    private String titleParams;

    /* -------------- end of instance field declarations ---------------- */

    /**
     * Construct a documentation generator instance for a project.
     * @param project the project this generator belongs to.
     */
    public DocuGenerator(Project project)
    {
        // setup tool-independent instance information
        this.project = project;
        projectDir = project.getProjectDir();
        projectDirPath = projectDir.getPath();
        docDir = new File(projectDir, docDirName);
        docDirPath = docDir.getPath();

        // tool-dependent instance information for javadoc
        destinationParam = " -d " + docDirPath;
        sourceParam = " -sourcepath " + projectDirPath;
        titleParams = " -doctitle " + project.getProjectName()
                    + " -windowtitle " + project.getProjectName();
    }

    /**
     * Generate documentation for the whole project. As this is done in
     * a different process this method just returns whether the preconditions
     * for the generation that are immediately testable are fulfilled.
     * @return "" if the external process was started, an error message
     * otherwise.
     */

    public String generateProjectDocu()
    {
        // test whether the documentation directory is accessible.
        String docDirStatus = testDocDir();
        if (docDirStatus != "")
            return docDirStatus;

        // get the names of all the targets for the documentation tool.
        // first: get the names of all packages that contain java sources.
        List packageNames = project.getPackageNames();
        StringBuffer tmp = new StringBuffer();
        for (Iterator names=packageNames.iterator(); names.hasNext(); ) {
            String packageName = (String)names.next();
            // as javadoc doesn't like packages with no java-files, we have to
            // pass only names of packages that really contain java files.
            Package pack = project.getPackage(packageName);
            if (FileUtility.containsFile(pack.getPath(),".java")) {
                tmp.append(" ");
                tmp.append(packageName);
            }
        }

        // second: get class names of classes in unnamed package, if any
        List classNames = project.getPackage(project.getInitialPackageName())
                                                          .getAllClassnames();
        for (Iterator names = classNames.iterator();names.hasNext(); ) {
            tmp.append(" ");
            tmp.append((String)names.next());
        }
        String targets = new String(tmp);


        // tool-specific infos for javadoc
        // get the parameter that enables javadoc to link the generated
        // documentation to the API documentation
        String linkParam = getLinkParam();

        // stick it all together
        String javadocCall = docCommand + sourceParam + destinationParam
                          + titleParams + linkParam + fixedJavadocParams
                          + targets;

        File startPage = new File(docDir,"index.html");
        File logFile = new File(docDir,"logfile");

        generateDoc(javadocCall, startPage, logFile, true);
        return "";
    }


    /**
     * Test whether documentation directory exists in project dir and
     * create it, if necessary.
     * @return "" if directory exists and is accessible, an error message
     * otherwise.
     */
    private static String testDocDir()
    {
        if (docDir.exists()) {
            if (!docDir.isDirectory())
                return DialogManager.getMessage("docdir-blocked-by-file");
        }
        else {
            try {
                if (!docDir.mkdir())
                    return DialogManager.getMessage("docdir-not-created");
            }
            catch (SecurityException exc) {
                return DialogManager.getMessage("no-permission-for-docdir");
            }
        }
        return "";
    }


    /**
     * javadoc can link the generated documentation to existing documentation.
     * This method constructs the javadoc parameter to set the link to the
     * Java API. To make sure that javadoc is happy we test whether the file
     * that javadoc needs (a list of all package names of the API) is
     * accessible via the link provided in the BlueJ properties file.
     * @return the link parameter if the link is working, "" otherwise.
     */
    private String getLinkParam()
    {
        String linkToLib = Config.getPropString("doctool.linkToStandardLib");
        if(linkToLib.equals("true")) {

            String docURL = Config.getPropString("bluej.url.javaStdLib");

            if (docURL.endsWith("index.html"))
                docURL = docURL.substring(0, docURL.indexOf("index.html"));

            return " -link " + docURL;
        }
        else
            return "";
    }
}

