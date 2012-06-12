/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.collect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

import bluej.Config;
import bluej.compiler.Diagnostic;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.Package;

/**
 * DataCollector for sending off data.
 * 
 * You can call these methods under any setting. It is this class's responsibility to check:
 *  - That the user has actually opted in (TODO)
 *  - That we're not running Greenfoot
 *  - That we don't keep attempting to send when there's no net connection (TODO)
 */
public class DataCollector
{
    private static final String VERSION = "1.0";
    
    private static final String PROPERTY_UUID = "blackbox.uuid";
    
    private static final Charset utf8 = Charset.forName("UTF-8");

    //private List<BPackage> openPkgs = new ArrayList<BPackage>();
    
    /** Whether we've seen the first error in the current compilation yet */
    //private boolean seenFirstError;
    
    private static String uuid;
    private static int sequenceNum;
    
    /** map from package directory to information on the sources contained within */
    //private Map<File,Set<SourceInfo>> srcInfoMap = new HashMap<File,Set<SourceInfo>>();

    public static void bluejOpened()
    {
        if (dontSend()) return;
        initUUidSequence();
        submitEventNoProject("bluej_start");
    }
    
    public static void projectOpened(Project proj)
    {   
        MultipartEntity mpe = new MultipartEntity();
        
        for (File f : proj.getFilesInProject(false, true))
        {
            if (f.getPath().toLowerCase().endsWith(".java"))
            {
                String relative = toPath(proj, f);
                
                mpe.addPart("project[source_files][][name]", toBody(relative));
                
                String contents = readFile(proj, f);
                
                if (contents != null)
                {
                    // The order here matters; if you put the [text][text] one first,
                    // Rails groups the entries into arrays wrongly.  It seems that the
                    // nested one must come last.  (Might be worth un-nesting that hash in future...)
                    mpe.addPart("source_histories[][source_event_type]", toBody("complete"));
                    mpe.addPart("source_histories[][name]", toBody(relative));
                    mpe.addPart("source_histories[][text][text]", toBody(contents));
                }
            }
        }
        
        submitEvent(proj.getProjectName(), "project_opening", mpe);
    }
    
    private static String readFile(Project proj, File f)
    {
        try {
            StringBuilder sb = new StringBuilder();
            FileInputStream inputStream = new FileInputStream(f);
            InputStreamReader reader = new InputStreamReader(inputStream, proj.getProjectCharset());
            char[] buf = new char[4096];
            
            int read = reader.read(buf);
            while (read != -1)
            {
                sb.append(buf, 0, read);
                read = reader.read(buf);
            }
            
            reader.close();
            inputStream.close();
            return sb.toString();
        }
        catch (IOException ioe) {return null;}
    }

    private static String toPath(Project proj, File f)
    {
        return proj.getProjectDir().toURI().relativize(f.toURI()).getPath();
    }
    
    private static boolean dontSend()
    {
        return Config.isGreenfoot(); //TODO or opted out or send failed
    }

    private static void initUUidSequence()
    {
        sequenceNum = 1;
        uuid = Config.getPropString(PROPERTY_UUID, null);
        if (uuid == null)
        {
            uuid = UUID.randomUUID().toString();
            Config.putPropString(PROPERTY_UUID, uuid);
            
        }
        /*
        else {
            try {
                int numFiles = Integer.parseInt(bluej.getExtensionPropertyString(PROPERTY_NUMFILES, "0"));
                // Read known file change times
                for (int i = 0; i < numFiles; i++) {
                    String fname = bluej.getExtensionPropertyString(PROPERTY_FILENAME + i, null);
                    if (fname == null) break;
                    String lastModString = bluej.getExtensionPropertyString(PROPERTY_LASTMOD + i, "0");
                    long lastMod = Long.parseLong(lastModString);
                    File file = new File(fname);
                    File parentFile = file.getParentFile();

                    Set<SourceInfo> infoSet = srcInfoMap.get(parentFile);
                    if (infoSet == null) {
                        infoSet = new HashSet<SourceInfo>();
                        srcInfoMap.put(parentFile, infoSet);
                    }

                    infoSet.add(new SourceInfo(file.getName(), lastMod));
                }
            }
            catch (NumberFormatException nfe) {
                // Shouldn't happen; just ignore it.
            }
        }
        */
    }
    
    /**
     * Save information which should be persistent across sessions.
     */
    private void saveInfo()
    {
        /*
        bluej.setExtensionPropertyString(PROPERTY_UUID, uuid);
        Set<Map.Entry<File,Set<SourceInfo>>> entrySet = srcInfoMap.entrySet();
        int fileCount = 0;
        for (Map.Entry<File,Set<SourceInfo>> entry : entrySet) {
            File baseDir = entry.getKey();
            Set<SourceInfo> infoSet = entry.getValue();
            int i = 0;
            for (SourceInfo info : infoSet) {
                File fullFile = new File(baseDir, info.getFilename());
                bluej.setExtensionPropertyString(PROPERTY_FILENAME + i, fullFile.getPath());
                bluej.setExtensionPropertyString(PROPERTY_LASTMOD + i, Long.toString(info.getLastModified()));
                i++;
                fileCount++;
            }
            
            // Hmm, this doesn't work - causes an exception, BlueJ 3.0.4:
            // bluej.setExtensionPropertyString(PROPERTY_FILENAME + i, null);
            // bluej.setExtensionPropertyString(PROPERTY_LASTMOD + i, null);
        }
        
        bluej.setExtensionPropertyString(PROPERTY_NUMFILES, "" + fileCount);
        */
    }
    
    /**
     * Handle a compilation event - collection information and submit it to the
     * data collection server in another thread.
     * 
     * @param files  the files being compiled (or with the error)
     * @param lineNum  the line number of the error (ignored if errMsg is null)
     * @param errMsg  the error message, or null if the compilation succeeded
     */
    private void handleCompilationEvent(File[] files, int lineNum, String errMsg)
    {
        /*
        if (files == null || files.length == 0) {
            return;
        }
        
        File errFile = files[0];
        File errDir = errFile.getParentFile();
        
        // Find what package the error was in:
        BPackage errPkg = getPackageForDir(errDir);
        if (errPkg == null) {
            return;
        }
        
        // Determine which classes have changed, get their content
        try {
            BClass[] bclasses = errPkg.getClasses();
            List<SourceContent> changedFiles = new ArrayList<SourceContent>();
            
            classLoop:
            for (BClass bclass : bclasses) {
                File sourceFile = bclass.getJavaFile();
                long lastMod = sourceFile.lastModified();
                Set<SourceInfo> infoSet = srcInfoMap.get(sourceFile.getParentFile());
                
                checkExistingInfo:
                if (infoSet != null) {
                    for (SourceInfo info : infoSet) {
                        if (info.getFilename().equals(sourceFile.getName())) {
                            if (info.getLastModified() == lastMod) {
                                continue classLoop;
                            }
                            try {
                                changedFiles.add(new SourceContent(sourceFile));
                                info.setLastModified(lastMod);
                                break checkExistingInfo;
                            }
                            catch (IOException ioe) {
                                // Don't really want to cause issues, so just ignore it.
                                break checkExistingInfo;
                            }
                        }
                    }
                    // If we get here, there's no existing info for the file:
                    try {
                        infoSet.add(new SourceInfo(sourceFile.getName(), lastMod));
                        changedFiles.add(new SourceContent(sourceFile));
                    }
                    catch (IOException ioe) {}
                }
                else {
                    try {
                        infoSet = new HashSet<SourceInfo>();
                        infoSet.add(new SourceInfo(sourceFile.getName(), lastMod));
                        changedFiles.add(new SourceContent(sourceFile));
                        srcInfoMap.put(sourceFile.getParentFile(), infoSet);
                    }
                    catch (IOException ioe) {
                        // Just ignore.
                    }
                }
            }
            String errPath = (errMsg == null) ? null : errFile.getPath();
            //DataSubmitter.submit(uuid, lineNum, errMsg, errPath, changedFiles);
        }
        catch (PackageNotFoundException pnfe) {}
        catch (ProjectNotOpenException pnoe) {}
        */
    }
    
    /**
     * Find the open package corresponding to the given directory.
     */
    /*
    private BPackage getPackageForDir(File dir)
    {
        BPackage thePkg = null;
        for (BPackage pkg : openPkgs) {
            try {
                if (pkg.getDir().equals(dir)) {
                    thePkg = pkg;
                    break;
                }
            }
            catch (PackageNotFoundException pnfe) { }
            catch (ProjectNotOpenException pnoe) { }
        }
        return thePkg;
    }
    */
    /*
    public static void packageOpened(Package pkg)
    {
        if (dontSend()) return;
        
        //TODO should this do something?
    }
    */
    /*
    @Override
    public void packageClosing(PackageEvent event)
    {
        if (Config.isGreenfoot()) return;
        
        openPkgs.remove(event.getPackage());
        saveInfo();
    }
    */

    public static void projectClosed(Project proj)
    {
        submitEventNoData(proj.getProjectName(), "project_closing");
    }

    public static void bluejClosed()
    {
        submitEventNoProject("bluej_finish");        
    }
    
    private static void submitEventNoProject(String eventName)
    {
        submitEventNoData("", eventName);
    }
    
    private static void submitEventNoData(String projectName, String eventName)
    {
        submitEvent(projectName, eventName, new MultipartEntity());
    }
    
    private static synchronized void submitEvent(String projectName, String eventName, MultipartEntity mpe)
    {
        if (dontSend()) return;
        
        mpe.addPart("user[uuid]", toBody(uuid));        
        mpe.addPart("project[name]", toBody(projectName));
        mpe.addPart("event[source_time]", toBody(DateFormat.getDateTimeInstance().format(new Date())));
        mpe.addPart("event[event_type]", toBody(eventName));
        mpe.addPart("event[sequence_id]", toBody(Integer.toString(sequenceNum)));
        
        DataSubmitter.submitEvent(mpe);
        sequenceNum += 1;
    }

    public static void compiled(Project proj, File[] sources, List<Diagnostic> diagnostics)
    {
        MultipartEntity mpe = new MultipartEntity();
        for (Diagnostic d : diagnostics)
        {
            mpe.addPart("event[compile_output][][start_line]", toBody(d.getStartLine()));
            mpe.addPart("event[compile_output][][end_line]", toBody(d.getEndLine()));
            mpe.addPart("event[compile_output][][start_column]", toBody(d.getStartColumn()));
            mpe.addPart("event[compile_output][][end_column]", toBody(d.getEndColumn()));
            mpe.addPart("event[compile_output][][is_error]", toBody(d.getType() == Diagnostic.ERROR));
            mpe.addPart("event[compile_output][][message]", toBody(d.getMessage()));
            // Must make file name relative for anonymisation:
            String relative = toPath(proj, new File(d.getFileName()));
            mpe.addPart("event[compile_output][][source_file_name]", toBody(relative));
            //TODO have a flag indicated whether the error was shown to the user
        }
        submitEvent(proj.getProjectName(), "compile", mpe);
    }
    
    private static StringBody toBody(String s)
    {
        try {
            return new StringBody(s, utf8);
        }
        catch (UnsupportedEncodingException e) {
            // Shouldn't happen, because UTF-8 is required to be supported
            return null;
        }
    }
    
    private static StringBody toBody(int i)
    {
        return toBody(Integer.toString(i));
    }
    
    private static StringBody toBody(long l)
    {
        return toBody(Long.toString(l));
    }
    
    private static StringBody toBody(boolean b)
    {
        return toBody(Boolean.toString(b));
    }

    public static void restartVM(Project project)
    {
        submitEventNoData(project.getProjectName(), "resetting_vm");        
    }

    public static void edit(Project proj, File path, String diff)
    {
        MultipartEntity mpe = new MultipartEntity();
        
        mpe.addPart("source_histories[][diff][diff]", toBody(diff));
        mpe.addPart("source_histories[][source_event_type]", toBody("multi_line_edit"));
        mpe.addPart("source_histories[][name]", toBody(toPath(proj, path))); 
        
        submitEvent(proj.getProjectName(), "multi_line_edit", mpe);
    }
}