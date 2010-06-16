/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

import java.util.List;

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.texteval.DeclaredVar;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.nodes.ParsedCUNode;

/**
 * Test that void results are handled correctly by the textpad parser.
 * 
 * @author Davin McCall
 * @version $Id: TextParserTest.java 7768 2010-06-16 04:55:19Z davmac $
 */
public class TextParserTest extends TestCase
{
    {
        InitConfig.init();
    }

    private TestEntityResolver resolver;
    private ObjectBench objectBench;
    
    @Override
    protected void setUp() throws Exception
    {
        objectBench = new ObjectBench(null);
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        MoeSyntaxDocument document = new MoeSyntaxDocument(resolver);
        document.enableParser(true);
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document.getParser();
    }
    
    public void testVoidResult()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("System.out.println(\"no comment\");");
        assertNull(r);
    }
    
    public void testNull()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("null");
        assertEquals("null", r);
    }
    
    public void testArithmeticPromotion()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("2+3");
        assertEquals("int", r);
        r = tp.parseCommand("2.0+3");
        assertEquals("double", r);
        r = tp.parseCommand("2.2+3.0f");
        assertEquals("double", r);
        r = tp.parseCommand("'a'+'b'");
        assertEquals("int", r);
        r = tp.parseCommand("4+5l");
        assertEquals("long", r);
    }
    
    public void testParenthesizedExpression()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(int)(2+5l) * 3");
        assertEquals("int", r);
    }
    
    public void testCasting()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(String)null");
        assertEquals("java.lang.String", r);
    }
    
    public void testCasting2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(String[])null");
        assertEquals("java.lang.String[]", r);
    }
    
    public void testCasting3()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(java.util.LinkedList<? extends javax.swing.JComponent>[])null");
        assertEquals("java.util.LinkedList<? extends javax.swing.JComponent>[]", r);
    }
    
    public void testStaticMethodCall()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("javax.swing.BorderFactory.createEmptyBorder()");
        assertEquals("javax.swing.border.Border", r);
        
        r = tp.parseCommand("Class.forName(\"java.lang.Object\")");
        assertEquals("java.lang.Class<?>", r);
    }

    public void testStaticVariable()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("System.out");
        assertEquals("java.io.PrintStream", r);
        r = tp.parseCommand("java.lang.System.out");
        assertEquals("java.io.PrintStream", r);
    }

    public void testNewExpression()
    {
        // Classes in java.lang can be unqualified
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new String()");
        assertEquals("java.lang.String", r);
        
        // Fully qualified classes
        r = tp.parseCommand("new java.util.LinkedList()");
        assertEquals("java.util.LinkedList", r);

        r = tp.parseCommand("new java.util.ArrayList(5)");
        assertEquals("java.util.ArrayList", r);
    }
    
    /** Used by the next test */
    public static class Inner<T>
    {
        public class Further<U>
        {
            U u;
        }
    }
    
    public void testNewExpression2()
    {
        // New inner class
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new " + Inner.class.getCanonicalName() + "()");
        assertEquals(getClass().getName() + ".Inner", r);
    }
    
    public void testNewExpression3()
    {
        // Type arguments
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new " + getClass().getName() + ".Inner<String>()");
        assertEquals(getClass().getName() + ".Inner<java.lang.String>", r);
        
        // Array
        r = tp.parseCommand("new int[10]");
        assertEquals("int[]", r);
        
        r = tp.parseCommand("new java.util.HashMap<String, String>[10]");
        assertEquals("java.util.HashMap<java.lang.String,java.lang.String>[]", r);
    }
    
    public void testNewInnerClass()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new javax.swing.Box.Filler(null, null, null)");
        assertEquals("javax.swing.Box.Filler", r);
    } 
    
    public void testCastToWildcard()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(java.util.LinkedList<?>) new java.util.LinkedList<Thread>()");
        assertEquals("java.util.LinkedList<?>", r);
    }
    
    public void testArrayDeclaration()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        tp.parseCommand("int [] ia = new int [] {1,2,3};");
        List<DeclaredVar> declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        DeclaredVar var = declaredVars.get(0);
        assertEquals("ia", var.getName());
        assertEquals("int[]", var.getDeclaredType().toString());
        
        // Test two-dimensional array
        tp.parseCommand("int [][] iaa = new int [5][6];");
        declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        var = declaredVars.get(0);
        assertEquals("iaa", var.getName());
        assertEquals("int[][]", var.getDeclaredType().toString());
    }
    
    public void testAnonymousInnerClass()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new Runnable() {" +
        		"public void run() {}" +
        		"}");
        assertEquals("java.lang.Runnable", r);
    }
    
    public void testClassLiteral()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("Object.class");
        assertEquals("java.lang.Class<java.lang.Object>", r);
        
        r = tp.parseCommand("int.class");
        assertEquals("java.lang.Class<java.lang.Integer>", r);
    }
    
    public void testClassLiteral2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("Object[].class");
        assertEquals("java.lang.Class<java.lang.Object[]>", r);
        
        r = tp.parseCommand("int[][].class");
        assertEquals("java.lang.Class<int[][]>", r);
    }
    
    public void testImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import java.util.LinkedList;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }
    
    public void testWcImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import java.util.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }

    public void testStaticImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import static java.awt.Color.BLACK;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("BLACK");
        assertEquals("java.awt.Color", r);
        
        // Try one that's not the same type as its enclosing class:
        r = tp.parseCommand("import static Math.PI;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("PI");
        assertEquals("double", r);
    }
    
    public void testStaticWildcardImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import static java.awt.Color.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("BLACK");
        assertEquals("java.awt.Color", r);
        
        // Try one that's not the same type as its enclosing class:
        r = tp.parseCommand("import static Math.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("PI");
        assertEquals("double", r);
    }
    
    public void testStringConcat()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("\"string\"");
        assertEquals("java.lang.String", r);
        
        r = tp.parseCommand("\"string\" + 4");
        assertEquals("java.lang.String", r);

        r = tp.parseCommand("\"string\" + 4.7 + 4");
        assertEquals("java.lang.String", r);

        r = tp.parseCommand("\"string\" + new Object()");
        assertEquals("java.lang.String", r);
    }
    
    public void testUnboxing()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new Integer(4) * 5");
        assertEquals("int", r);
        
        // + is especially confusing because it's overloaded for Strings:
        r = tp.parseCommand("new Integer(4) + 5");
        assertEquals("int", r);
    }
    
    public void testOperators()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("3 << 2");
        assertEquals("int", r);
        r = tp.parseCommand("3 << 2l");
        assertEquals("int", r);
        
        r = tp.parseCommand("3 >> 2l");
        assertEquals("int", r);
        
        r = tp.parseCommand("3 >>> 2");
        assertEquals("int", r);
        
        r = tp.parseCommand("3 == 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 != 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 < 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 <= 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 > 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 >= 4");
        assertEquals("boolean", r);
    }
    
    public void testUnboxingNumericComparison()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("3 == new Integer(6)");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(6) != 3");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) != 0.0f");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) < 0.0f");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) < new Double(8)");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) < null");
        assertNull(r);
        
        r = tp.parseCommand("3 < null");
        assertNull(r);
        
        r = tp.parseCommand("3.0f < (Integer)null");
        assertEquals("boolean", r);
    }
        
    public void testEqualityReferenceOperators()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("null == null");
        assertEquals("boolean", r);
        r = tp.parseCommand("new Integer(4) == null");
        assertEquals("boolean", r);
        // Perform an object reference check:
        r = tp.parseCommand("new Integer(4) != new Integer(5)");
        assertEquals("boolean", r);
        // This shouldn't convert to numeric types, at least one must be numeric,
        // and they don't inherit from each other so it's invalid:
        //r = tp.parseCommand("new Integer(4) != new Double(6)");
        //assertNull(r);

        // These should work because the numeric types inherit from Object:
        r = tp.parseCommand("new Object() != new Double(6)");
        assertEquals("boolean", r);
        r = tp.parseCommand("new Integer(5) == new Object()");
        assertEquals("boolean", r);
    }
    
    public void testLiterals()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("0xFFFF");
        assertEquals("int", r);
        
        r = tp.parseCommand("0xFFFFFFFFFl");
        assertEquals("long", r);
        
        r = tp.parseCommand("1234");
        assertEquals("int", r);
        
        r = tp.parseCommand("12345L");
        assertEquals("long", r);
    }
    
    //test behaviour of parsing of statements and expressions
    //please refer to #Bug 213
    public void testObjectBench()
    {
        String lalaSrc = "package xyz; public class Lala { " +
                "public String toString() { return \"haha\"; }" +
                "public String foo() { return \"mama\"; } }";
        
        String nanaSrc="package xyz;\n"
            + "public class Nana extends Lala\n"
            + "{\n"
            + "  public int bar() {\n"
            + "    return 99;\n"
            + "  }\n"
            + "}\n";

        ParsedCUNode lalaNode = cuForSource(lalaSrc, "xyz");
        resolver.addCompilationUnit("xyz", lalaNode);
       
        ParsedCUNode nanaNode = cuForSource(nanaSrc, "xyz");
        resolver.addCompilationUnit("xyz", nanaNode);

        EntityResolver res = new PackageResolver(this.resolver, "xyz");
                
        TextAnalyzer tp = new TextAnalyzer(res, "xyz", objectBench);
        
        String r = tp.parseCommand("46");        
        assertEquals("int", r);
        tp.confirmCommand();
        
        r = tp.parseCommand("new Lala()");        
        assertEquals("xyz.Lala", r);
        tp.confirmCommand();
            
        r = tp.parseCommand("(new Lala()).toString()");
        assertEquals("java.lang.String", r);
        tp.confirmCommand();
       
        r = tp.parseCommand("(new Lala()).foo()");
        assertEquals("java.lang.String", r);
        tp.confirmCommand();
        
        r = tp.parseCommand("(new Nana()).foo()");
        assertEquals("java.lang.String", r);
        tp.confirmCommand();

        r = tp.parseCommand("(new Nana()).bar()");
        assertEquals("int", r);
        tp.confirmCommand();     
    }
    
    public void testMethodResolution()
    {
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public int method(Runnable r) { return 0; }\n"
            + "  public float method(Thread r) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((Runnable) null)");
        assertEquals("int", r);
        r = tp.parseCommand("new Lala().method(new Thread())");        
        assertEquals("float", r);
    }
    
    public void testMethodResolution2()
    {
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public int method(Runnable r) { return 0; }\n"
            + "  private float method(Thread r) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((Runnable) null)");
        assertEquals("int", r);
        r = tp.parseCommand("new Lala().method(new Thread())");        
        assertEquals("int", r);
    }
    
    public void testMethodResolution3()
    {
        // widening primitive conversion
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public float method(int b) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((byte)3)");
        assertEquals("float", r);
    }

    public void testMethodResolution4()
    {
        // boxing conversion followed by widening reference conversion
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public String method(Integer i) { return null; }\n"
            + "  public float method(Object o) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((byte)3)");
        assertEquals("float", r);
    }
    
    public void testMethodResolution5()
    {
        // method with type parameters, return type is dependent on parameter type
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public <T> T method(T i) { return null; }\n"
            // + "  public float method(Object o) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method(\"a string\")");
        assertEquals("java.lang.String", r);
    }
    
    //TODO test instanceof

}
