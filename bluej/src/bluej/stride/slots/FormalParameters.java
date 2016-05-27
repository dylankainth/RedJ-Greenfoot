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
package bluej.stride.slots;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.TextSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.EmptyError;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.ConcatMapListBinding;
import bluej.utility.javafx.binding.DeepListBinding;

public class FormalParameters
{
    protected final InteractionManager editor;
    protected final ObservableList<FormalParameter> params;
    protected final Frame parentFrame;
    protected final CodeFrame<? extends CodeElement> codeParentFrame;
    private final FrameContentRow row;
    protected String stylePrefix;
    private final SlotLabel spacer;
    private final SlotLabel open = new SlotLabel("(");
    private final SlotLabel close = new SlotLabel(")");
    private boolean normalView = true;
    private FXRunnable updateSlots;

    public FormalParameters(InteractionManager editor, Frame parentFrame, CodeFrame<? extends CodeElement> codeParentFrame, FrameContentRow row, String stylePrefix)
    {
        this.editor = editor;
        this.parentFrame = parentFrame;
        this.codeParentFrame = codeParentFrame;
        if (parentFrame != codeParentFrame)
            throw new IllegalArgumentException("codeFrame and codeParentFrame should be identical");
        this.row = row;
        this.stylePrefix = stylePrefix;
        params = FXCollections.observableArrayList();

        open.getStyleClass().add("bracket-label");
        spacer = new SlotLabel(" ");
        JavaFXUtil.addStyleClass(spacer, "param-spacer");
        spacer.setOpacity(0.0);
        spacer.setCursor(Cursor.TEXT);
        spacer.setOnMouseClicked(e -> {
                if (normalView)
                {
                    addNewAfter(null).requestFocus(Focus.LEFT);
                    e.consume();
                }
        });
        close.getStyleClass().add("bracket-label");
    }

    public FormalParameter findFormal(HeaderItem slot)
    {
        return params.stream().filter(p -> p.getType() == slot || p .getName() == slot).findFirst().orElse(null);
    }
    
    private FormalParameter createFormal(TypeSlotFragment type, NameDefSlotFragment name)
    {
        BooleanProperty freshProperty = new SimpleBooleanProperty(true);
        Runnable checkStillFocused = () -> {
            // Once it turns false, it can never go back to true:
            freshProperty.set(freshProperty.get() && params.stream().anyMatch(p -> p.isFocused()));
        };
        final TypeSlot typeSlot = new TypeSlot(editor, parentFrame, codeParentFrame, row, TypeSlot.Role.DECLARATION, stylePrefix)
        {
            /*TODOTYPESLOT
            @Override
            protected BooleanExpression getFreshExtra(CodeError err)
            {
                if (err instanceof EmptyError)
                    return freshProperty;
                else
                    return super.getFreshExtra(err);
            }*/

            @Override
            public void lostFocus()
            {
                super.lostFocus();
                // Wait until it's all settled
                // then see if anyone is still focused, and if not, cancel fresh property:
                if (freshProperty.get())
                    Platform.runLater(checkStillFocused);
            }
        };
        final VariableNameDefTextSlot nameSlot = new VariableNameDefTextSlot(editor, parentFrame, codeParentFrame, row, stylePrefix)
        {
            @Override
            protected BooleanExpression getFreshExtra(CodeError err)
            {
                if (err instanceof EmptyError)
                    return freshProperty;
                else
                    return super.getFreshExtra(err);
            }

            @Override
            public void lostFocus()
            {
                super.lostFocus();
                // Wait until it's all settled
                // then see if anyone is still focused, and if not, cancel fresh property:
                if (freshProperty.get())
                    Platform.runLater(checkStillFocused);
            }
        };
        TypeSlot paramType = typeSlot; //TODOTYPESLOT there is more to add back from initialiseTextSlot
        paramType.setText(type);
        paramType.setSimplePromptText("paramType");
        TextSlot<NameDefSlotFragment> paramName = initialiseTextSlot("paramName", name, false, nameSlot);
        
        return new FormalParameter(paramType, paramName);
    }
    private <F extends TextSlotFragment> TextSlot<F> initialiseTextSlot(String promptText, F value, boolean isType, TextSlot<F> textSlot)
    {
        textSlot.setPromptText(promptText);
        textSlot.setText(value);

        textSlot.addValueListener(new SlotValueListener() {
            public boolean valueChanged(HeaderItem slot, String oldValue, String newValue, FocusParent<HeaderItem> parent)
            {
                if (newValue.contains(",")) {
                    if (isType) {
                        if (findFormal(slot).getName().isFocused()) {
                            //name is focused, add new after
                            Platform.runLater(() -> addNewAfter(findFormal(slot)).requestFocus(Focus.LEFT));
                        }
                        else if (findFormal(slot).getType().isFocused()) {
                            Platform.runLater(() -> addNewBefore(findFormal(slot)).requestFocus(Focus.LEFT));
                        }
                    }
                    else {
                        Platform.runLater(() -> addNewAfter(findFormal(slot)).requestFocus(Focus.LEFT));
                    }
                    return false;
                }

                if (newValue.contains(")")) {
                    if (!isType && newValue.endsWith(")")) {
                        parent.focusRight(textSlot);
                    }
                    return false;
                }

                return true;
            }

            @Override
            public void backSpacePressedAtStart(HeaderItem slot)
            {
                FormalParameters.this.backSpacePressedAtStart(slot);
            }

            @Override
            public void deletePressedAtEnd(HeaderItem slot)
            {
                FormalParameters.this.deletePressedAtEnd(slot);
            }
        });

        textSlot.addFocusListener(parentFrame);
        textSlot.addValueListener(SlotTraversalChars.IDENTIFIER);
        return textSlot;
    }
    
    public void addFormal(TypeSlotFragment type, NameDefSlotFragment name)
    {
        params.add(createFormal(type, name));
    }

    private FormalParameter addNewBefore(FormalParameter before)
    {
        return insertBefore(before, createFormal(new TypeSlotFragment("", ""), new NameDefSlotFragment("")));
    }
    
    private FormalParameter addNewAfter(FormalParameter after)
    {
        return insertAfter(after, createFormal(new TypeSlotFragment("", ""), new NameDefSlotFragment("")));
    }

    private FormalParameter insertBefore(FormalParameter before, FormalParameter slot)
    {
        params.add(before == null ? 0 : params.indexOf(before), slot);
        editor.modifiedFrame(parentFrame);
        return slot;
    }
    
    private FormalParameter insertAfter(FormalParameter after, FormalParameter slot)
    {
        params.add(after == null ? 0 : params.indexOf(after) + 1, slot);
        editor.modifiedFrame(parentFrame);
        return slot;
    }

    /**
     * Ensures that there is at least one parameter in the parameters list.
     * @return true if we had to add a new parameter, false if there already was at least one
     */
    public boolean ensureAtLeastOneParameter()
    {
        if (params.isEmpty())
        {
            addNewAfter(null);
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /*
    public FormalParameter addSlot(TypeSlotFragment type, NameDefSlotFragment name)
    {
        return insertSlot(null, prepareSlot(type, name));
    }
    
    public FormalParameter addEmptySlot()
    {
       return addSlot(new TypeSlotFragment(""), new NameDefSlotFragment(""));
    }

    public void setSlots(List<String> types, List<String> names)
    {
        if (types.size() != names.size()) {
            throw new IllegalArgumentException("Types and names must be same length");
        }
        
        while (slotCount() > 0) {
            deleteFirstSlot();
        }
        
        for (int i = 0; i < types.size(); i++) {
            insertSlot(null, prepareSlot(new TypeSlotFragment(types.get(i)), new NameDefSlotFragment(names.get(i))));
        }
    }
    
    public boolean showError(JavaCompileError err)
    {
        for (FormalParameter pairSlot : slots) {
            if (pairSlot.showError(err)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkForEmptySlot()
    {
        if ( isEmpty() ) {
            if ( !slots.get(0).isFocused() ) {
                cleanup();
                deleteFirstSlot();
            }
        }
    }

    @Override
    protected void mergeTwoSlotsContents(FormalParameter slot, FormalParameter prevSlot)
    {
        // TODO What we want in this case?
    }

    @Override
    protected FormalParameter getWrapperSlot(Slot slot)
    {
        return slots.stream().filter(p -> p.getName() == slot || p.getType() == slot).findFirst().orElse(null);
    }

    @Override
    protected boolean isLastInTheWrapper(Slot slot)
    {
        return getWrapperSlot(slot).getName().equals(slot);
    }

    @Override
    public void requestFocus(Focus on)
    {
        if (slots.size() == 0) {
            addEmptySlot();
        }
        
        if (on == Focus.LEFT) {
            slots.get(0).requestFocus(on);
        }
        else if (on == Focus.RIGHT) {
            slots.get(slots.size() - 1).requestFocus(on);
        }
    }
*/


    /**
     * Here's the outcome of a brief design decision discussion.  Given params:
     *
     * void foo(int a, String b)
     *
     * Here are some numbered cursor positions of interest:
     *
     * void foo|(|int| |a|, |String b|)
     *         0 1   2 3 4  5        6
     *
     * You can press backspace at 1, 3, 5 without character directly before.  For those:
     *
     * 1 [start of type of first param]:
     *   Move cursor left to end of method name, don't delete anything
     * 3 [start of name of any param]:
     *   Move cursor left to end of type name, don't delete anything
     * 5 [start of type of non-first param]:
     *   If parameter we are in is totally blank, delete our current one and move left.
     *   Otherwise, delete parameter beforehand.
     *   (Note: if both are blank, it doesn't matter which we delete)
     */
    private void backSpacePressedAtStart(HeaderItem slot)
    {
        FormalParameter formal = findFormal(slot);
        if (formal.getType() == slot)
        {
            // Backspace at beginning of type.
            int index = params.indexOf(formal);
            // Delete prev slot, if we are not the first:
            if (index == 0)
            {
                // We are first parameter, just move left from us into method name:
                row.focusLeft(formal.getType());
            }
            else
            {
                // We are not first.  Are we blank?
                if (formal.getName().isAlmostBlank() && formal.getType().isAlmostBlank())
                {
                    // We are; delete us and focus the right hand end of parameter before us:
                    deleteFormal(formal);
                    params.get(index - 1).getName().requestFocus(Focus.RIGHT);
                }
                else
                {
                    // We're not blank; delete the one before us:
                    deleteFormal(params.get(index - 1));
                }
            }
        }
        else
        {
            // Backspace at beginning of name.
            // Just move left, to end of type slot:
            formal.getType().requestFocus(Focus.RIGHT);
        }

    }

    /**
     * Here's the outcome of a brief design decision discussion.  Given params:
     *
     * void foo(int a, String b)
     *
     * Here are some numbered cursor positions of interest:
     *
     * void foo|(|int| |a|, |String b|)
     *         0 1   2 3 4  5        6
     *
     * You can press delete at 0, 2, 4, 6 without character directly after.  For those:
     *
     * 0 [end of method name]:
     *   Delete first parameter
     * 2 [end of type of any param]:
     *   Do nothing
     * 4 [end of name of non-last param]:
     *   Delete parameter to the right
     * 6 [end of name of last param]:
     *   Do nothing
     */
    private void deletePressedAtEnd(HeaderItem slot)
    {
        FormalParameter formal = findFormal(slot);
        if (formal.getType() == slot)
        {
            // End of type, do nothing
        }
        else
        {
            // End of name.  Are we last?
            int index = params.indexOf(formal);
            // If we're not the last parameter, delete the one after us:
            if (index < params.size() - 1)
            {
                deleteFormal(params.get(index+1));
            }
        }
    }
    
    private void deleteFormal(FormalParameter param)
    {
        //if (index > 0) {
            // TODO 
            //SLOT prevSlot = slots.get(index - 1);
            // Copy remaining content into previous slot:
            //mergeTwoSlotsContents(slot, prevSlot);
            //prevSlot.requestFocus(Focus.RIGHT);
        //}

        // Remove the formal:
        param.cleanup();
        params.remove(param);
        editor.modifiedFrame(parentFrame);
    }
    
    public void checkForEmptySlot()
    {
        if ( params.size() == 1 && params.get(0).isEmpty() && !params.get(0).isFocused() ) {
            deleteFormal(params.get(0));
        }
    }

    private ObservableList<HeaderItem> boundSlots;
    
    public ObservableList<HeaderItem> getSlots()
    {
        if (boundSlots == null)
        {
            boundSlots = FXCollections.observableArrayList();
            ConcatMapListBinding.bind(boundSlots, params, FormalParameter::getSlots);
            
            DeepListBinding<HeaderItem> binding = new DeepListBinding<HeaderItem>(boundSlots) {

                {
                    updateSlots = this::update;
                }

                @Override
                protected Stream<ObservableList<?>> getListenTargets()
                {
                    return Stream.concat(Stream.of(params), params.stream().map(FormalParameter::getSlots));
                }

                @Override
                protected Stream<HeaderItem> calculateValues()
                {
                    ArrayList<ObservableList<HeaderItem>> commas = new ArrayList<>();
                    for (int i = 0; i < params.size() - 1; i++)
                    {
                        SlotLabel comma = new SlotLabel(", ");
                        JavaFXUtil.addStyleClass(comma, "formal-comma");
                        commas.add(FXCollections.observableArrayList(comma));
                    }

                    Stream<HeaderItem> start = (params.size() == 0) ? Stream.of(open, spacer) : Stream.of(open);
                    
                    // Important that we filter the null operators out after interleaving, to preserve ordering:
                    return Utility.concat(start, Utility.interleave(params.stream().map(FormalParameter::getSlots), commas.stream()).flatMap(List::stream), Stream.of(close));
                }
            };

            binding.startListening();
        }
        return boundSlots;
    }

    public boolean isEmpty()
    {
        return params.isEmpty();
    }

    public void escape(HeaderItem src)
    {
        if (params.size() == 1 && params.get(0).isEmpty()) {
            row.focusDown(src);
        }
    }

    public void focusBeginning()
    {
        if (!params.isEmpty())
            params.get(0).requestFocus(Focus.LEFT);
    }

    private class FormalParameter
    {
        private final TypeSlot type;
        private final TextSlot<NameDefSlotFragment> name;

        protected FormalParameter(TypeSlot type, TextSlot<NameDefSlotFragment> name)
        {
            this.type = type;
            this.name = name;
        }
/*
        public String getText()
        {
            return type.getText() + " " + name.getText();
        }
        
        public void positionCaretName(int position)
        {
            name.positionCaret(position);
        }
        */
        
        public void cleanup()
        {
            type.cleanup();
            name.cleanup();
        }

        public TypeSlot getType()
        {
            return type;
        }
        
        public TextSlot<NameDefSlotFragment> getName()
        {
            return name;
        }
/*
        public void setText(String text)
        {
            // TODO TEST IT
            type.setText(text);
        }

        public void positionCaret(int position)
        {
            // TODO TEST IT
            type.positionCaret(position);
        }
        */

        public ParamFragment getSlotElement()
        {
            return new ParamFragment(type.getSlotElement(), name.getSlotElement());
        }

        /*
        public void both(Consumer<TextSlot<?>> f)
        {
            f.accept(type);
            f.accept(name);
        }
         */
        public boolean isEmpty()
        {
            return type.isEmpty() && name.isEmpty();
        }
        
        public boolean isFocused()
        {
            return getType().isFocused() || getName().isFocused();
        }
        
        public void requestFocus(Focus on)
        {
            if (on == Focus.LEFT) {
                type.requestFocus(on);
            }
            else if (on == Focus.RIGHT) {
                name.requestFocus(on);
            }
            else if (on == Focus.SELECT_ALL) {
                bluej.utility.Debug.message("Focus.SELECT_ALL in PairParameterSlot::requestFocus is not implemented");
            }
        }

        public ObservableList<HeaderItem> getSlots()
        {
            return FXCollections.observableArrayList(type, name);
        }
    }

    public List<ParamFragment> getSlotElement()
    {
        return Utility.mapList(params, FormalParameter::getSlotElement);
    }

    public void deleteFirstParam()
    {
        if (params.size() > 0)
            params.remove(0);
    }

    public <T> void setParams(List<T> src, Function<T, String> getType, Function<T, String> getName)
    {
        params.setAll(Utility.mapList(src, x -> createFormal(new TypeSlotFragment(getType.apply(x), getType.apply(x)), new NameDefSlotFragment(getName.apply(x)))));
    }
    
    public Stream<String> getVars()
    {
        return params.stream().map(FormalParameter::getName).map(TextSlot::getText);
    }


    public void setView(Frame.View view, SharedTransition animate)
    {
        normalView = view == Frame.View.NORMAL;
        if (updateSlots != null)
        {
            updateSlots.run();
        }
    }

}
