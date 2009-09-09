package bluej.editor.moe;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;

public class NaviView2 extends JEditorPane implements AdjustmentListener, DocumentListener
{
    private JScrollBar scrollBar;
    
    private int currentViewPos;
    private int currentViewPosBottom;
    
    private int dragOffset;
    
    public NaviView2(Document document, JScrollBar scrollBar)
    {
        this.scrollBar = scrollBar;
        Font smallFont = new Font(Font.MONOSPACED, Font.BOLD, 1);
        setFont(smallFont);
        setDocument(document);
        
        scrollBar.addAdjustmentListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        // The scrollbar position changed
        
        int newTop = sbPositionToLine(e.getValue());
        int newBottom = sbPositionToLine(e.getValue() + scrollBar.getVisibleAmount());
        
        Element map = getDocument().getDefaultRootElement();
        
        int topPos = map.getElement(newTop).getStartOffset();
        if (newBottom >= map.getElementCount()) {
            newBottom = map.getElementCount() - 1;
        }
        int bottomPos = map.getElement(newBottom).getEndOffset() - 1;
        
        // Now convert to view coordinates
        try {
            int topV = modelToView(topPos).y;
            Rectangle bottomL = modelToView(bottomPos);
            int bottomV = bottomL.y + bottomL.height;

            int repaintTop = Math.min(topV, currentViewPos);
            int repaintBottom = Math.max(bottomV, currentViewPosBottom);

            currentViewPos = topV;
            currentViewPosBottom = bottomV;

            repaint(0, repaintTop, getWidth(), repaintBottom - repaintTop + 1);
        }
        catch (BadLocationException ble) {}
    }

    public void removeUpdate(DocumentEvent e)
    {
        // TODO Auto-generated method stub
        
    }
    
    public void insertUpdate(DocumentEvent e)
    {
        // TODO Auto-generated method stub
        
    }
    
    public void changedUpdate(DocumentEvent e)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void processMouseEvent(MouseEvent e)
    {
        //super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            int y = e.getY();
            if (y > currentViewPos && y < currentViewPosBottom) {
                // clicked within the current view area
                dragOffset = y - currentViewPos;
            }
            else {
                dragOffset = (currentViewPosBottom - currentViewPos) / 2;
                moveView(e.getY());
            }
        }
    }
    
    @Override
    protected void processMouseMotionEvent(MouseEvent e)
    {
        //super.processMouseMotionEvent(e);
        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            moveView(e.getY());
        }
    }

    /**
     * Move the view (by setting the scrollbar value), according to the given mouse coordinate
     * within the NaviView component.
     */
    private void moveView(int ypos)
    {
        int modelPos = viewToModel(new Point(0, ypos - dragOffset));
        int lineNum = getDocument().getDefaultRootElement().getElementIndex(modelPos);
        lineNum = Math.max(0, lineNum);
        
        int totalLines = getDocument().getDefaultRootElement().getElementCount();
        int totalAmt = scrollBar.getMaximum() - scrollBar.getMinimum();
        
        int pos = lineNum * totalAmt / totalLines + scrollBar.getMinimum();
        scrollBar.setValue(pos);
    }
    
    /**
     * Convert a scrollbar position to a source line number.
     */
    private int sbPositionToLine(int position)
    {
        int amount = scrollBar.getMaximum() - scrollBar.getMinimum();
        int lines = getDocument().getDefaultRootElement().getElementCount();
        return position * lines / amount;
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {   
        Rectangle clipBounds = new Rectangle(new Point(0,0), getSize());
        Insets insets = getInsets();
        g.getClipBounds(clipBounds);
        
        //Color foreground = MoeSyntaxDocument.getDefaultColor();
        Color background = MoeSyntaxDocument.getBackgroundColor();
        Color notVisible = new Color((int)(background.getRed() * .9f),
                (int)(background.getGreen() * .9f),
                (int)(background.getBlue() * .9f));
        
        g.setColor(notVisible);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

        Document document = getDocument();
        if (document == null) {
            // Should not happen
            return;
        }
        Element map = document.getDefaultRootElement();
        
        int topLine = sbPositionToLine(scrollBar.getValue());
        int bottomLine = sbPositionToLine(scrollBar.getValue() + scrollBar.getVisibleAmount());
        try {
            int topLineV = modelToView(map.getElement(topLine).getStartOffset()).y;
            bottomLine = Math.min(bottomLine, map.getElementCount() - 1);
            Rectangle vBottom = modelToView(map.getElement(bottomLine).getStartOffset());
            int bottomLineV = vBottom.y + vBottom.height;
            int viewHeight = bottomLineV - topLineV;

            g.setColor(background);
            g.fillRect(clipBounds.x, topLineV, clipBounds.width, viewHeight);

            //View view = getEditorKit().getViewFactory().create(document.getDefaultRootElement());
            View view = getUI().getRootView(this);
                       
            
            // Create new image that can be manipulated.
            //
            // TODO optimizations: it might be possible to make the buffered
            // image smaller if x or y are bigger than 0, and doing some
            // translation on the Graphics2D. Also, not sure which image type is
            // best to use here. A INT_ARGB might be better. And, there might be
            // completely different and more efficient way tof doing the same
            // thing.
            BufferedImage img =  getGraphicsConfiguration().createCompatibleImage(clipBounds.x + clipBounds.width, clipBounds.y + clipBounds.height,
                    Transparency.TRANSLUCENT);
            Graphics2D imgG = img.createGraphics();
            imgG.setClip(clipBounds);     

            // Paint text to the offscreen image
            imgG.setFont(getFont());
            Rectangle shape = new Rectangle(0, 0, getBounds().width, getBounds().height);
            view.paint(imgG, shape);

            // Filter the image
            ImageProducer producer = new FilteredImageSource(img.getSource(), new DarkenFilter());
            Image filteredImg = this.createImage(producer);

            // Paint the filtered image onto the graphics
            g.drawImage(filteredImg, 0, 0, null);            
            
            // Draw a border around the visible area
            g.setColor(new Color((int)(background.getRed() * .7f),
                    (int)(background.getGreen() * .7f),
                    (int)(background.getBlue() * .7f)));
            g.drawRect(0 + insets.left, topLineV, getWidth() - insets.left - insets.right - 1,
                    viewHeight);
        }
        catch (BadLocationException ble) {}
       
    }
    
    private final static class DarkenFilter extends RGBImageFilter {
        public DarkenFilter() {
            // When this is set to true, the filter will work with images
            // whose pixels are indices into a color table (IndexColorModel).
            // In such a case, the color values in the color table are filtered.
            canFilterIndexColorModel = true;
        }
    
        // This method is called for every pixel in the image
        public int filterRGB(int x, int y, int rgb) {
            if (x == -1) {
                // The pixel value is from the image's color table rather than the image itself
            }
            Color c = new Color(rgb, true);
            int red = c.getRed();
            int green = c.getGreen();
            int blue = c.getBlue();
            int alpha = c.getAlpha();
            //red = darken(red);
            //green = darken(green);
            //blue = darken(blue);
            
            // Make it more opaque
            alpha = darken(alpha);
            return new Color(red, green, blue, alpha).getRGB();
        }
        
        private int darken(int c)
        {
            c = c << 2;
            if(c>255) c = 255;
            return c;
        }
    }
    

}
