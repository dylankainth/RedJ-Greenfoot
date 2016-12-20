/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2013,2014,2015,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.terminal;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import bluej.BlueJTheme;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.SwingNodeFixed;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerTerminal;
import bluej.debugmgr.ExecutionEvent;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The Frame part of the Terminal window used for I/O when running programs
 * under BlueJ.
 *
 * @author  Michael Kolling
 * @author  Philip Stevens
 */
@SuppressWarnings("serial")
public final class Terminal
    implements KeyListener, BlueJEventListener, DebuggerTerminal
{
    private static final String WINDOWTITLE = Config.getApplicationName() + ": " + Config.getString("terminal.title");
    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //private static final int ALT_SHORTCUT_MASK =
    //        SHORTCUT_MASK == Event.CTRL_MASK ? Event.CTRL_MASK : Event.META_MASK;

    private static final String TERMINALFONTPROPNAME = "bluej.terminal.font";
    private static final String TERMINALFONTSIZEPROPNAME = "bluej.editor.fontsize";
    
    private static final String RECORDMETHODCALLSPROPNAME = "bluej.terminal.recordcalls";
    private static final String CLEARONMETHODCALLSPROPNAME = "bluej.terminal.clearscreen";
    private static final String UNLIMITEDBUFFERINGCALLPROPNAME = "bluej.terminal.buffering";
        
    // initialise to config value or zero.
    private static int terminalFontSize = Config.getPropInteger(
            TERMINALFONTSIZEPROPNAME, PrefMgr.getEditorFontSize());
    
    private static boolean isMacOs = Config.isMacOS();
    private final String title;

    // -- instance --

    private final Project project;
    
    private TermTextArea text;
    private TermTextArea errorText;
    private JScrollPane errorScrollPane;
    private JScrollPane scrollPane;
    private JSplitPane splitPane;
    private boolean isActive = false;
    private static boolean recordMethodCalls =
            Config.getPropBoolean(RECORDMETHODCALLSPROPNAME);
    private static boolean clearOnMethodCall =
            Config.getPropBoolean(CLEARONMETHODCALLSPROPNAME);
    private static boolean unlimitedBufferingCall =
            Config.getPropBoolean(UNLIMITEDBUFFERINGCALLPROPNAME);
    private boolean newMethodCall = true;
    private boolean errorShown = false;
    private InputBuffer buffer;

    private JCheckBoxMenuItem autoClear;
    private JCheckBoxMenuItem recordCalls;
    private JCheckBoxMenuItem unlimitedBuffering;

    @OnThread(Tag.Any) private final Reader in = new TerminalReader();
    @OnThread(Tag.Any) private final Writer out = new TerminalWriter(false);
    @OnThread(Tag.Any) private final Writer err = new TerminalWriter(true);

    /** Used for lazy initialisation  */
    private boolean initialised = false;
    @OnThread(Tag.FX)
    private Stage window;
    private JPanel mainPanel;
    /**
     * Since all the decisions to show or hide the window pass through the Swing
     * thread, we can actually keep track reliably on the Swing thread of whether
     * the FX window is currently showing:
     */
    private boolean isShowing;

    /**
     * Create a new terminal window with default specifications.
     */
    @OnThread(Tag.Swing)
    public Terminal(Project project)
    {
        this.title = WINDOWTITLE + " - " + project.getProjectName();
        this.project = project;
        initialise();
        BlueJEvent.addListener(this);
    }

    /**
     * Get the terminal font
     */
    private static Font getTerminalFont() {
        //reload terminal fontsize from configurations.

        terminalFontSize = Config.getPropInteger(
                TERMINALFONTSIZEPROPNAME, PrefMgr.getEditorFontSize());
        return Config.getFont(
                TERMINALFONTPROPNAME, "Monospaced", terminalFontSize);
    }

    /*
     * Set the terminal font size to equal either the passed parameter, or the
     * editor font size if the passed parameter is too low. Place the updated
     * value into the configuration.
     */
    public static void setTerminalFontSize(int size)
    {
        if (size <= 6) {
            return;
        } else {
            terminalFontSize = size;
        }
        Config.putPropInteger(TERMINALFONTSIZEPROPNAME, terminalFontSize);
    }

    
    /**
     * Initialise the terminal; create the UI.
     */
    private synchronized void initialise()
    {
        if(! initialised) {            
            buffer = new InputBuffer(256);
            int width = Config.isGreenfoot() ? 80 : Config.getPropInteger("bluej.terminal.width", 80);
            int height = Config.isGreenfoot() ? 10 : Config.getPropInteger("bluej.terminal.height", 22);
            makeWindow(width, height);
            initialised = true;
            text.setUnlimitedBuffering(unlimitedBufferingCall);
        }
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        DataCollector.showHideTerminal(project, show);

        isShowing = show;
        Platform.runLater(() -> {
            if (show)
                window.show();
            else
                window.hide();
            if(show) {
                SwingUtilities.invokeLater(() -> text.requestFocus());
            }
        });
    }
    
    public void dispose()
    {
        showHide(false);
        Platform.runLater(() -> {
            window = null;
        });
    }

    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return isShowing;
    }

    /**
     * Make the window active.
     */
    public void activate(boolean active)
    {
        if(active != isActive) {
            text.setEditable(active);
            if (!active) {
                text.getCaret().setVisible(false);
            }
            isActive = active;
        }
    }

    /**
     * Check whether the terminal is active (accepting input).
     */
    public boolean checkActive()
    {
        return isActive;
    }
    
    /**
     * Reset the font according to preferences.
     */
    public void resetFont()
    {
        Font terminalFont = getTerminalFont();
        text.setFont(terminalFont);
        if (errorText != null) {
            errorText.setFont(terminalFont);
        }
    }

    /**
     * Clear the terminal.
     */
    public void clear()
    {
        text.setText("");
        if(errorText!=null) {
            errorText.setText("");
        }
        hideErrorPane();
    }


    /**
     * Save the terminal text to file.
     */
    public void save()
    {
        Platform.runLater(() -> {
            File fileName = FileUtility.getSaveFileFX(window,
                    Config.getString("terminal.save.title"),
                    null, false);
            if(fileName != null) {
                if (fileName.exists()){
                    if (DialogManager.askQuestionFX(window, "error-file-exists") != 0)
                        return;
                }
                SwingUtilities.invokeLater(() -> {
                    try
                    {
                        FileWriter writer = new FileWriter(fileName);
                        text.write(writer);
                        writer.close();
                    } catch (IOException ex)
                    {
                        Platform.runLater(() -> DialogManager.showErrorFX(window, "error-save-file"));
                    }
                });
            }
        });
    }
    
    public void print()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        int printFontSize = Config.getPropInteger("bluej.fontsize.printText", 10);
        Font font = new Font("Monospaced", Font.PLAIN, printFontSize);
        if (job.printDialog()) {
            TerminalPrinter.printTerminal(job, text, job.defaultPage(), font);
        }
    }

    /**
     * Write some text to the terminal.
     */
    private void writeToPane(boolean stdout, String s)
    {
        prepare();
        if (!stdout)
            showErrorPane();
        
        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1) {
            clear();
            s = s.substring(n + 1);
        }
        
        TermTextArea tta = stdout ? text : errorText;
        
        tta.append(s);
        tta.setCaretPosition(tta.getDocument().getLength());       
    }
    
    public void writeToTerminal(String s)
    {
        writeToPane(true, s);
    }
    
    /**
     * Prepare the terminal for I/O.
     */
    private void prepare()
    {
        if (newMethodCall) {   // prepare only once per method call
            showHide(true);
            newMethodCall = false;
        }
        else if (Config.isGreenfoot()) {
            // In greenfoot new output should always show the terminal
            if (!isShowing) {
                showHide(true);
            }
        }
    }

    /**
     * An interactive method call has been made by a user.
     */
    private void methodCall(String callString)
    {
        newMethodCall = false;
        if(clearOnMethodCall) {
            clear();
        }
        if(recordMethodCalls) {
            text.appendMethodCall(callString + "\n");
        }
        newMethodCall = true;
    }
    
    private void constructorCall(InvokerRecord ir)
    {
        newMethodCall = false;
        if(clearOnMethodCall) {
            clear();
        }
        if(recordMethodCalls) {
            String callString = ir.getResultTypeString() + " " + ir.getResultName() + " = " + ir.toExpression() + ";";
            text.appendMethodCall(callString + "\n");
        }
        newMethodCall = true;
    }
    
    private void methodResult(ExecutionEvent event)
    {
        if (recordMethodCalls) {
            String result = null;
            String resultType = event.getResult();
            
            if (resultType == ExecutionEvent.NORMAL_EXIT) {
                DebuggerObject object = event.getResultObject();
                if (object != null) {
                    if (event.getClassName() != null && event.getMethodName() == null) {
                        // Constructor call - the result object is the created object.
                        // Don't display the result separately:
                        return;
                    }
                    else {
                        // if the method returns a void, we must handle it differently
                        if (object.isNullObject()) {
                            return; // Don't show result of void calls
                        }
                        else {
                            // other - the result object is a wrapper with a single result field
                            DebuggerField resultField = object.getField(0);
                            result = "    returned " + resultField.getType().toString(true) + " ";
                            result += resultField.getValueString();
                        }
                    }
                }
            }
            else if (resultType == ExecutionEvent.EXCEPTION_EXIT) {
                result = "    Exception occurred.";
            }
            else if (resultType == ExecutionEvent.TERMINATED_EXIT) {
                result = "    VM terminated.";
            }
            
            if (result != null) {
                text.appendMethodCall(result + "\n");
            }
        }
    }


    /**
     * Return the input stream that can be used to read from this terminal.
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Reader getReader()
    {
        return in;
    }


    /**
     * Return the output stream that can be used to write to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getWriter()
    {
        return out;
    }


    /**
     * Return the output stream that can be used to write error output to this terminal
     */
    @OnThread(value = Tag.Any, ignoreParent = true)
    public Writer getErrorWriter()
    {
        return err;
    }

    // ---- KeyListener interface ----

    @Override
    public void keyPressed(KeyEvent event)
    {
        handleFontsizeKeys(event, event.getKeyCode());
    }
    
    @Override
    public void keyReleased(KeyEvent event) { }
    
    /**
     * Handle the keys which change the terminal font size.
     * 
     * @param event   The key event (key pressed/released/typed)
     * @param ch      The key code (for pressed/release events) or character (for key typed events)
     */
    private boolean handleFontsizeKeys(KeyEvent event, int ch)
    {
        boolean handled = false;
        
        // Note the following works because VK_EQUALS, VK_PLUS and VK_MINUS
        // are actually defined as their ASCII (and thus unicode) equivalent.
        // Since they are final constants this cannot become untrue in the
        // future.
        
        switch (ch) {
        case KeyEvent.VK_EQUALS: // increase the font size
        case KeyEvent.VK_PLUS: // increase the font size (non-uk keyboards)
            if (event.getModifiers() == SHORTCUT_MASK) {
                PrefMgr.setEditorFontSize(terminalFontSize + 1);
                event.consume();
                handled = true;
                break;
            }

        case KeyEvent.VK_MINUS: // decrease the font size
            if (event.getModifiers() == SHORTCUT_MASK) {
                PrefMgr.setEditorFontSize(terminalFontSize - 1);
                event.consume();
                handled = true;
                break;
            }
        }

        return handled;
    }

    @Override
    public void keyTyped(KeyEvent event)
    {
        // We handle most things we are interested in here. The InputMap filters out
        // most other unwanted actions (but allows copy/paste).

        char ch = event.getKeyChar();

        if ((event.getModifiers() & Event.META_MASK) != 0) {
            return; // return without consuming the event
        }
        if (isActive) {
            switch (ch) {

            case 4:   // CTRL-D (unix/Mac EOF)
            case 26:  // CTRL-Z (DOS/Windows EOF)
                buffer.signalEOF();
                writeToTerminal("\n");
                event.consume();
                break;

            case '\b':  // backspace
                if (buffer.backSpace()) {
                    try {
                        int length = text.getDocument().getLength();
                        text.replaceRange("", length - 1, length);
                    } catch (Exception exc) {
                        Debug.reportError("bad location " + exc);
                    }
                }
                event.consume();
                break;

            case '\r':  // carriage return
            case '\n':  // newline
                if (buffer.putChar('\n')) {
                    // SwingNode gives us '\r' as the character for pressing Enter,
                    // but we want a newline in that case, so pass '\n' to the terminal:
                    writeToTerminal(String.valueOf('\n'));
                    buffer.notifyReaders();
                }
                event.consume();
                break;

            default:
                if (ch >= 32) {
                    if (buffer.putChar(ch)) {
                        writeToTerminal(String.valueOf(ch));
                    }
                    event.consume();
                }
                break;
            }
        }
    }


    // ---- BlueJEventListener interface ----

    /**
     * Called when a BlueJ event is raised. The event can be any BlueJEvent
     * type. The implementation of this method should check first whether
     * the event type is of interest an return immediately if it isn't.
     *
     * @param eventId  A constant identifying the event. One of the event id
     *                 constants defined in BlueJEvent.
     * @param arg      An event specific parameter. See BlueJEvent for
     *                 definition.
     */
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        if(eventId == BlueJEvent.METHOD_CALL) {
            InvokerRecord ir = (InvokerRecord) arg;
            if (ir.getResultName() != null) {
                constructorCall(ir);
            }
            else {
                boolean isVoid = ir.hasVoidResult();
                if (isVoid) {
                    methodCall(ir.toStatement());
                }
                else {
                    methodCall(ir.toExpression());
                }
            }
        }
        else if (eventId == BlueJEvent.EXECUTION_RESULT) {
            methodResult((ExecutionEvent) arg);
        }
    }

    // ---- make window frame ----

    /**
     * Create the Swing window.
     */
    private void makeWindow(int columns, int rows)
    {
        text = new TermTextArea(rows, columns, buffer, project, this, false);
        final InputMap origInputMap = text.getInputMap();
        text.setInputMap(JComponent.WHEN_FOCUSED, new InputMap() {
            {
                setParent(origInputMap);
            }
            
            @Override
            public Object get(KeyStroke keyStroke)
            {
                Object actionName = super.get(keyStroke);
                
                if (actionName == null) {
                    return null;
                }
                
                char keyChar = keyStroke.getKeyChar();
                if (keyChar == KeyEvent.CHAR_UNDEFINED || keyChar < 32) {
                    // We might want to filter the action
                    if ("copy-to-clipboard".equals(actionName)) {
                        return actionName;
                    }
                    if ("paste-from-clipboard".equals(actionName)) {
                        // Handled via paste() in TermTextArea
                        return actionName;
                    }
                    return PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT) ? actionName : null;
                }
                
                return actionName;
            }
        });
        
        scrollPane = new JScrollPane(text);
        text.setFont(getTerminalFont());
        text.setEditable(false);
        text.setMargin(new Insets(6, 6, 6, 6));
        text.addKeyListener(this);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        SwingNode swingNode = new SwingNodeFixed();
        swingNode.setContent(mainPanel);
        FXPlatformSupplier<MenuBar> makeFXMenuBar = JavaFXUtil.swingMenuBarToFX(makeMenuBar(), mainPanel);
        Platform.runLater(() -> {
            window = new Stage();
            window.setWidth(500);
            window.setHeight(500);
            BlueJTheme.setWindowIconFX(window);
            window.setTitle(title);
            MenuBar fxMenuBar = makeFXMenuBar.get();
            fxMenuBar.setUseSystemMenuBar(true);
            VBox.setVgrow(swingNode, Priority.ALWAYS);
            window.setScene(new Scene(new VBox(fxMenuBar, swingNode)));

            // Close Action when close button is pressed
            window.setOnCloseRequest(e -> {
                // We consume the event on the FX thread, then hop to the Swing
                // thread to decide if we can close:
                e.consume();
    
                SwingUtilities.invokeLater(() -> {
                    // don't allow them to close the window if the debug machine
                    // is running.. tries to stop them from closing down the
                    // input window before finishing off input in the terminal
                    if (project != null) {
                        if (project.getDebugger().getStatus() == Debugger.RUNNING)
                            return;
                    }
                    showHide(false);
                });
            });

            Config.rememberPosition(window, "bluej.terminal");
        });
    }

    /**
     * Create a second scrolled text area to the window, for error output.
     */
    private void createErrorPane()
    {
        errorText = new TermTextArea(Config.isGreenfoot() ? 15 : 5, 80, null, project, this, true);
        errorScrollPane = new JScrollPane(errorText);
        errorText.setFont(getTerminalFont());
        errorText.setEditable(false);
        errorText.setMargin(new Insets(6, 6, 6, 6));
        errorText.setUnlimitedBuffering(true);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   scrollPane, errorScrollPane); 
    }
    
    /**
     * Show the errorPane for error output
     */
    private void showErrorPane()
    {
        if(errorShown) {
            return;
        }
        
        //the first time the errortext is shown we need to pack() it
        //to make it have the right size.
        boolean isFirstShow = false; 
        if(errorText == null) {
            isFirstShow = true;
            createErrorPane();
        }
     
        mainPanel.remove(scrollPane);
  
        // We want to know if it is not the first time
        // This means a "clear" has been used to remove the splitpane
        // when this re-adds the scrollPane to the terminal area
        // it implicitly removes it from the splitpane as it can only have one
        // owner. The side-effect of this is the splitpane's
        // top component becomes null.
        if(!isFirstShow)
            splitPane.setTopComponent(scrollPane);
        mainPanel.add(splitPane, BorderLayout.CENTER);       
        splitPane.resetToPreferredSizes();
            
        mainPanel.validate();
        
        errorShown = true;
    }
    
    /**
     * Hide the pane with the error output.
     */
    private void hideErrorPane()
    {
        if(!errorShown) {
            return;
        }
        mainPanel.remove(splitPane);
        mainPanel.add(scrollPane, BorderLayout.CENTER);        
        errorShown = false; 
        mainPanel.validate();
    }
    
    /**
     * Create the terminal's menubar, all menus and items.
     */
    private JMenuBar makeMenuBar()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(Config.getString("terminal.options"));
        JMenuItem item;
        item = menu.add(new ClearAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                                                   SHORTCUT_MASK));
        item = menu.add(getCopyAction());
        item.setText(Config.getString("terminal.copy"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                   SHORTCUT_MASK));
        item = menu.add(new SaveAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                                   SHORTCUT_MASK));
        menu.add(new PrintAction());
        menu.add(new JSeparator());

        autoClear = new JCheckBoxMenuItem(new AutoClearAction());
        autoClear.setSelected(clearOnMethodCall);
        menu.add(autoClear);

        recordCalls = new JCheckBoxMenuItem(new RecordCallAction());
        recordCalls.setSelected(recordMethodCalls);
        menu.add(recordCalls);

        unlimitedBuffering = new JCheckBoxMenuItem(new BufferAction());
        unlimitedBuffering.setSelected(unlimitedBufferingCall);
        menu.add(unlimitedBuffering);

        menu.add(new JSeparator());
        item = menu.add(new CloseAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                                                   SHORTCUT_MASK));

        menubar.add(menu);
        return menubar;
    }

    /**
     * Cleanup any resources or listeners the terminal has created/registered.
     * Called when the project is closing.
     */
    public void cleanup()
    {
        BlueJEvent.removeListener(this);
    }


    private class ClearAction extends AbstractAction
    {
        public ClearAction()
        {
            super(Config.getString("terminal.clear"));
        }

        public void actionPerformed(ActionEvent e)
        {
            clear();
        }
    }

    private class SaveAction extends AbstractAction
    {
        public SaveAction()
        {
            super(Config.getString("terminal.save"));
        }

        public void actionPerformed(ActionEvent e)
        {
            save();
        }
    }
    
    private class PrintAction extends AbstractAction
    {
        public PrintAction()
        {
            super(Config.getString("terminal.print"));
        }

        public void actionPerformed(ActionEvent e)
        {
            print();
        }
    }

    private class CloseAction extends AbstractAction
    {
        public CloseAction()
        {
            super(Config.getString("terminal.close"));
        }

        public void actionPerformed(ActionEvent e)
        {
            showHide(false);
        }
    }

    private Action getCopyAction()
    {
        Action[] textActions = text.getActions();
        for (int i=0; i < textActions.length; i++) {
            if(textActions[i].getValue(Action.NAME).equals("copy-to-clipboard")) {
                return textActions[i];
            }
        }

        return null;
    }

    private class AutoClearAction extends AbstractAction
    {
        public AutoClearAction()
        {
            super(Config.getString("terminal.clearScreen"));
        }

        public void actionPerformed(ActionEvent e)
        {
            clearOnMethodCall = autoClear.isSelected();
            Config.putPropBoolean(CLEARONMETHODCALLSPROPNAME, clearOnMethodCall);
        }
    }

    private class RecordCallAction extends AbstractAction
    {
        public RecordCallAction()
        {
            super(Config.getString("terminal.recordCalls"));
        }

        public void actionPerformed(ActionEvent e)
        {
            recordMethodCalls = recordCalls.isSelected();
            Config.putPropBoolean(RECORDMETHODCALLSPROPNAME, recordMethodCalls);
        }
    }

    private class BufferAction extends AbstractAction
    {
        public BufferAction()
        {
            super(Config.getString("terminal.buffering"));
        }

        public void actionPerformed(ActionEvent e)
        {
            unlimitedBufferingCall = unlimitedBuffering.isSelected();
            text.setUnlimitedBuffering(unlimitedBufferingCall);
            Config.putPropBoolean(UNLIMITEDBUFFERINGCALLPROPNAME, unlimitedBufferingCall);
        }
    }
            
    /**
     * A Reader which reads from the terminal.
     */
    @OnThread(Tag.Any)
    private class TerminalReader extends Reader
    {
        public int read(char[] cbuf, int off, int len)
        {
            int charsRead = 0;

            while(charsRead < len) {
                cbuf[off + charsRead] = buffer.getChar();
                charsRead++;
                if(buffer.isEmpty())
                    break;
            }
            return charsRead;
        }

        @Override
        public boolean ready()
        {
            return ! buffer.isEmpty();
        }
        
        public void close() { }
    }

    /**
     * A writer which writes to the terminal. It can be flagged for error output.
     * The idea is that error output could be presented differently from standard
     * output.
     */
    @OnThread(Tag.Any)
    private class TerminalWriter extends Writer
    {
        private boolean isErrorOut;
        
        TerminalWriter(boolean isError)
        {
            super();
            isErrorOut = isError;
        }

        public void write(final char[] cbuf, final int off, final int len)
        {
            try {
                // We use invokeAndWait so that terminal output is limited to
                // the processing speed of the event queue. This means the UI
                // will still respond to user input even if the output is really
                // gushing.
                EventQueue.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        writeToPane(!isErrorOut, new String(cbuf, off, len));
                    }
                });
            }
            catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
            catch (InterruptedException ie) {}
        }

        public void flush() { }

        public void close() { }
    }
}
