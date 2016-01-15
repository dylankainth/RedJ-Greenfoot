/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.ast;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Stream;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.stride.framedjava.ast.JavaFragment.Destination;
import bluej.stride.framedjava.ast.JavaFragment.ErrorRelation;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame;
import bluej.utility.Debug;

public class JavaSource
{
    private final List<SourceLine> lines = new ArrayList<>();
    private static class SourceLine
    {
        private String indent; // some string of spaces
        private final List<JavaFragment> content; // no preceding spaces, no trailing newlines
        private final JavaSingleLineDebugHandler debugHandler; // ditto
        private final boolean breakpoint;
        public SourceLine(String indent, List<JavaFragment> content,
                JavaSingleLineDebugHandler debugHandler,
                boolean breakpoint) {
            if (indent == null || content == null) {
                throw new IllegalArgumentException("Null argument to SourceLine");
            }
            this.indent = indent;
            this.content = content;
            this.debugHandler = debugHandler;
            this.breakpoint = breakpoint;
            
            for (JavaFragment f : content)
            {
                if (f == null)
                    throw new IllegalArgumentException("Cannot have null Java fragment in sourceLine");
            }
        }
    }

    // Copy constructor (shallow copy)
    public JavaSource(JavaSource copyFrom)
    {
        lines.addAll(copyFrom.lines);
    }
    
    public JavaSource(JavaSingleLineDebugHandler debugHandler, JavaFragment... line)
    {
        // Add them via splitter, in case they have newlines:
        //for (String line : lines)
        {
            appendLine(Arrays.asList(line), debugHandler);
        }
    }
    public JavaSource(JavaSingleLineDebugHandler debugHandler, List<JavaFragment> line)
    {
        appendLine(line, debugHandler);
    }

    public void appendLine(List<JavaFragment> line, JavaSingleLineDebugHandler debugHandler)
    {
        addLine(lines.size(), line, debugHandler);
    }
    
    public void prependLine(List<JavaFragment> line, JavaSingleLineDebugHandler debugHandler)
    {
        addLine(0, line, debugHandler);
    }
    
    public void prepend(JavaSource src)
    {
        lines.addAll(0, src.lines);
    }

    public void appened(JavaSource javaCode)
    {
        lines.addAll(javaCode.lines);
    }

    private void addLine(int position, List<JavaFragment> line, JavaSingleLineDebugHandler debugHandler)
    {
        lines.add(position, new SourceLine("", line, debugHandler, false));
    }

    public void addIndented(JavaSource javaCode)
    {
        for (SourceLine line : javaCode.lines) {
            line.indent += "    ";
        }
        appened(javaCode);
    }
    
    private static interface Recorder
    {
        /**
         * To be able to match back the elements to their position
         * in each line of the Java code.
         * @param positionInFile Position across whole file, 0 being first char
         * @param lineNumber Line in the file
         * @param columnNumber Column in the file
         */
        public void recordPosition(JavaFragment fragment, int posInSource, int lineNumber, int columnNumber, int len);
    }
    
    @OnThread(Tag.FX)
    public String toDiskJavaCodeString()
    {
        return toJavaCodeString(Destination.JAVA_FILE_TO_COMPILE, null, (frag, pos, lineNumber, columnNumber, len) -> frag.recordDiskPosition(lineNumber, columnNumber, len));
    }

    public String toMemoryJavaCodeString(IdentityHashMap<JavaFragment, Integer> positions, ExpressionSlot<?> completing)
    {
        return toJavaCodeString(Destination.SOURCE_DOC_TO_ANALYSE, completing, (frag, pos, a, b, c) -> positions.put(frag, pos));
    }

    @OnThread(Tag.Any)
    public String toTemporaryJavaCodeString()
    {
        return toJavaCodeString(Destination.TEMPORARY, null, (frag, pos, a, b, c) -> {});
    }

    @OnThread(Tag.Any)
    private String toJavaCodeString(Destination dest, ExpressionSlot<?> completing, Recorder recorder)
    {
        StringBuilder sourceString = new StringBuilder();
        int lineNumber = 1;
        for (SourceLine line : lines) {
            int sourceLength = sourceString.length();
            StringBuilder oneLineString = new StringBuilder(100);
            oneLineString.append(line.indent);
            for (JavaFragment fragment : line.content) {
                int lineLength = oneLineString.length();
                String codeLine = fragment.getJavaCode(dest, completing);
                recorder.recordPosition(fragment, sourceLength + lineLength, lineNumber, lineLength + 1, codeLine.length());
                if (codeLine.contains("\n") || codeLine.contains("\r")) {
                    throw new IllegalStateException("Source line contains \\n or \\r! Line: " + codeLine);
                }
                oneLineString.append(codeLine);
            }
            sourceString.append(oneLineString.toString()).append("\n");
            lineNumber += 1;
        }
        return sourceString.toString();
    }
    
    @OnThread(Tag.FX)
    public boolean handleError(int startLine, int startColumn,
            int endLine, int endColumn, String message, boolean force, int identifier)
    {
        JavaFragment fragment = findError(startLine, startColumn, endLine, endColumn, message, force);
        if (fragment != null)
        {
            fragment.showCompileError(startLine, startColumn, endLine, endColumn, message, identifier);
            return true;
        }
        else
            return false;
    }

    @OnThread(Tag.Any)
    public JavaFragment findError(int startLine, int startColumn, int endLine, int endColumn, String message, boolean force)
    {
        // If it's on the last empty line, use handler from last line:
        if (startLine == lines.size() + 1) {
            startLine -= 1;
        }

        if (startLine >= lines.size() || startLine == -1)
        {
            // We'll retry in a minute anyway:
            if (!force)
                return null;

            // Just show on the very last fragment we can find:
            for (int i = lines.size() - 1; i >= 0; i--)
            {
                List<JavaFragment> frags = lines.get(i).content;
                for (int j = frags.size() - 1; j >= 0; j--)
                {
                    JavaFragment f = frags.get(j);
                    if (f.checkCompileError(startLine, startColumn, endLine, endColumn) != ErrorRelation.CANNOT_SHOW)
                    {
                        return f;
                    }
                }
            }
            // Nothing at all?!  Give up!
            Debug.message("No fragments found capable of showing error (shouldn't happen): " + message);
            return null;
        }

        JavaFragment last = null;
        for (JavaFragment f : lines.get(startLine - 1).content) // Lines start at 1
        {
            ErrorRelation r = f.checkCompileError(startLine, startColumn, endLine, endColumn);
            if (r == ErrorRelation.CANNOT_SHOW)
                continue;

            if (r == ErrorRelation.BEFORE_FRAGMENT && last != null)
            {
                return last;
            }
            else if (r != ErrorRelation.AFTER_FRAGMENT)
            {
                return f;
            }
            last = f;
        }
        if (last != null)
        {
            return last;
        }
        else
        {
            Debug.reportError("No slots found to show compile error: (" + startLine + "," + startColumn + ")->(" + endLine + "," + endColumn + "): " + message);
            return null;
        }
    }

    @OnThread(Tag.FX)
    public HighlightedBreakpoint handleStop(int line, DebugInfo debug)
    {
        JavaSingleLineDebugHandler handler = lines.get(line - 1).debugHandler; // Lines start at 1
        if (handler != null) {
            return handler.showDebugBefore(debug);
        }
        else {
            Debug.message("Cannot debug line: " + lines.get(line - 1).content);
            return null;
        }
    }
    
    @OnThread(Tag.FX)
    public void handleException(int lineNumber)
    {
        Debug.message("Handling " + lineNumber);
        JavaSingleLineDebugHandler handler = lines.get(lineNumber - 1).debugHandler; // Lines start at 1
        if (handler != null)
        {
            handler.showException();
        }
        else
        {
            Debug.message("Cannot show exception for line: " + lines.get(lineNumber - 1).content);
        }
    }

    @OnThread(Tag.Swing)
    public void registerBreakpoints(Editor editor, EditorWatcher watcher)
    {
        for (int i = 0;i < lines.size(); i++) {
            if (lines.get(i).breakpoint) {
                watcher.breakpointToggleEvent(editor, i + 1, true);
            }
        }
    }

    // Header line should have no curly brackets
    public static JavaSource createMethod(Frame frame, JavaSingleLineDebugHandler debugHandler, 
            JavadocUnit documentation, List<JavaFragment> header, List<JavaSource> contents)
    {
        JavaSource parent = new JavaSource(debugHandler, header);
        parent.prependJavadoc(documentation.getJavaCode());
        parent.appendLine(Arrays.asList(new FrameFragment(frame, "{")), null);
        for (JavaSource src : contents) {
            parent.addIndented(src);
        }
        // Methods can have breakpoint on last line so no need for extra code:
        parent.appendLine(Arrays.asList(new FrameFragment(frame, "}")), debugHandler);
        return parent;
        
    }
    
    public static JavaSource createCompoundStatement(Frame frame, JavaSingleLineDebugHandler headerDebugHandler,
            JavaContainerDebugHandler endDebugHandler, List<JavaFragment> header, List<JavaSource> contents)
    {
        return createCompoundStatement(frame, headerDebugHandler, endDebugHandler, header, contents, null);
    }
    
    // Header line should have no curly brackets
    public static JavaSource createCompoundStatement(Frame frame, JavaSingleLineDebugHandler headerDebugHandler, 
            final JavaContainerDebugHandler endDebugHandler, List<JavaFragment> header, List<JavaSource> contents, JavaFragment footer)
    {
        ArrayList<JavaFragment> headerAndBrace = new ArrayList<>(header);
        headerAndBrace.add(new FrameFragment(frame, " {"));
        JavaSource parent = new JavaSource(headerDebugHandler, headerAndBrace);
        for (JavaSource src : contents) {
            parent.addIndented(src);
        }
        
        /*
         * Adding the extra dummy statement causes the breakboint to appear twice. In addition, it seems to be
         * unneeded after the change that been made in the 'removeSpecialsAfter' method in the 'JavaCanvas' class 
       
        // If, loops, etc cannot have breakpoint on last line so need extra code to break on,
        // except if the last statement is a return one.
        boolean hasReturn = parent.lines.get(parent.lines.size() -1).content.get(0).getJavaCode().contains("return");
        if (endDebugHandler != null && !hasReturn) {
            parent.appendLine(Arrays.asList(b("if (Object.class != null);")), errorHandler, new JavaSingleLineDebugHandler() {
                @Override public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
                {
                    return endDebugHandler.showDebugAtEnd(debug);
                }
            });
        }
        */
       
        parent.appendLine(Arrays.asList(new FrameFragment(frame, "}")), null);
        
        if (footer != null) {
            parent.addIndented(new JavaSource(headerDebugHandler, footer));
        }
        return parent;
    }

    public static JavaSource createBreakpoint(Frame frame, JavaSingleLineDebugHandler handler)
    {
        // We need a valid line of Java code for the breakpoint, but no method calls
        // (so step-over/-into work the same).  This may trigger a warning in future javac:
        JavaSource r = new JavaSource(handler);
        r.lines.add(new SourceLine("", Arrays.asList(new FrameFragment(frame, "{ int org_greenfoot_debug_frame = 7; } /* dummy code for breakpoint */")), handler, true));
         
        return r;
    }

    //For debugging (of Greenfoot) purposes:
    public JavaSingleLineDebugHandler internalGetDebugHandler(int i)
    {
        return lines.get(i).debugHandler;
    }
    
    /*private static JavaFragment b(String s)
    {
        return new Boilerplate(s);
    }*/

    public void prependJavadoc(List<String> javadocLines)
    {
        for (int i = javadocLines.size() - 1; i >= 0; i--) {
            prependLine(Arrays.asList(new FrameFragment(null, javadocLines.get(i))), null);
        }
    }
    
    public Stream<JavaFragment> getAllFragments()
    {
        return lines.stream().flatMap(l -> l.content.stream());
    }
}
