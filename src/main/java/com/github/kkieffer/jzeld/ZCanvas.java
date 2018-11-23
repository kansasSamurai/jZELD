
package com.github.kkieffer.jzeld;

import com.github.kkieffer.jzeld.contextMenu.ZCanvasContextMenu;
import com.github.kkieffer.jzeld.adapters.JAXBAdapter.ColorAdapter;
import com.github.kkieffer.jzeld.adapters.JAXBAdapter.DimensionAdapter;
import com.github.kkieffer.jzeld.adapters.JAXBAdapter.FontAdapter;
import com.github.kkieffer.jzeld.adapters.JAXBAdapter.PointAdapter;
import com.github.kkieffer.jzeld.adapters.JAXBAdapter.Rectangle2DAdapter;
import com.github.kkieffer.jzeld.draw.DrawClient;
import com.github.kkieffer.jzeld.element.ZElement;
import com.github.kkieffer.jzeld.element.ZAbstractShape;
import com.github.kkieffer.jzeld.element.ZCanvasRuler;
import com.github.kkieffer.jzeld.element.ZElement.StrokeStyle;
import com.github.kkieffer.jzeld.element.ZGrid;
import com.github.kkieffer.jzeld.element.ZGroupedElement;
import com.github.kkieffer.jzeld.element.ZShape;
import java.awt.BasicStroke;
import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * ZCanvas is where the ZElements are drawn and manipulated
 * 
 * Once they are added, the can be selected by the mouse, and will be highlighted with a dashed line.  Once selected it
 * can be dragged to new locations, and resized by dragging the corner drag box.  Moving the mouse wheel resizes the object
 * maintaining the aspect ratio.  Holding shift while rotating the mouse wheel rotates the object.
 * 
 * Double clicking an element will notify it that it is selected and it will receive mouse events. Clicking outside that
 * element will notify the element it no longer is selected.  Shift clicking will select multiple items.
 *  
 * A selected elements can be moved with the arrow keys.
 * 
 * Holding shift while clicking elements selects multiple elements.
 * 
 * The canvas can be zoomed with the plus/minus keys, zoom in up to 4.0 factor.
 * 
 * The canvas can be moved with the arrow keys if nothing is selected.
 * 
 * A popup menu can be added when selecting each element.  Additional hotkeys can also be added.
 *
 * 
 * @author kkieffer
 */
public class ZCanvas extends JComponent implements Printable, MouseListener, MouseMotionListener, MouseWheelListener  {


    public enum HighlightAnimation {Fast(200), Slow(510), None(510);

        private final int delayPeriod;
        private HighlightAnimation(int delay) {
            delayPeriod = delay;
        }
        private int delaySpeed() {
            return delayPeriod;
        }
    }
    
    public enum CombineOperation {Join, Subtract, Intersect, Exclusive_Join;
    
        @Override
        public String toString() {
            return this.name().replace("_", " ");
        }
        
        public String getHtmlHelp() {
            
            String common = "<br><br>The element at the lowest layer is the reference element, and elements are combined in sequence moving up the Z-plane layers to the top. " + 
                            "The resultant shape inherits all the color and line properties from the reference element.";
            
            
            switch (this) {
                case Join:
                    return "Joins multiple shapes into a single shape.  All parts of the selected shapes are included, both overlapping and non-overlapping." + common;
                case Subtract:
                    return "Subtracts from the first selected shape the overlapping areas from all the other selected shapes" + common;
                case Intersect:
                    return "Creates a shape from only the overlapping parts of the selected shapes." + common;
                case Exclusive_Join:
                    return "Joins multiple shapes into a single shape.  All parts of the selected shapes are included except the ones that overlap with each other." + common;
                default:
                    throw new RuntimeException("Unhandled CombineOperation case");
            }
        }
    }
    
    public enum Orientation {LANDSCAPE, PORTRAIT, REVERSE_LANDSCAPE}  //The ordinals conform to the PageFormat integer defines
   
    public enum Alignment {Auto, Left_Edge, Top_Edge, Right_Edge, Bottom_Edge, Centered_Vertically, Centered_Horizontally, Centered_Both;
        
        @Override
        public String toString() {
            return this.name().replace("_", " ");
        }
        
        public String getHtmlHelp() {     
            
            switch (this) {
                case Auto:
                    return "Align element centers, either horizontally or vertically, whichever minimizes the movement.";
                case Left_Edge:
                    return "Aligns element left edges at the same horizontal (X) position";
                case Top_Edge:
                    return "Aligns element top edges at the same vertical (Y) position";
                case Right_Edge:
                    return "Aligns element right edges at the same horizontal (X) position";
                case Bottom_Edge:
                    return "Aligns element bottom edges at the same vertical (Y) position";
                case Centered_Vertically:
                    return "Centers the elements along a vertical (Y) axis";
                case Centered_Horizontally:
                    return "Centers the elements along a horizontal (X) axis";
                case Centered_Both:
                    return "Centers the elements to the same point";
                default:
                    throw new RuntimeException("Unhandled Align case");
            }
        }
    
    }
    
    public static final ImageIcon errorIcon = new ImageIcon(ZCanvas.class.getResource("/error.png")); 
  
    
    private static final double ROTATION_MULTIPLIER = 1.0;
    private static final double SHEAR_MULTIPLIER = 0.1;
    private static final double SIZE_INCREASE_MULTIPLIER = 0.5;
    private final static float SCALE = 72.0f;

    /* -------- FIELDS BELOW CAN BE SAVED TO FILE USING JAXB ---------------*/
    /* This class is used because JaxB can't handle JComponent superclass*/
    @XmlRootElement(name="ZCanvas")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CanvasStore implements Serializable {

     
        @XmlJavaTypeAdapter(ColorAdapter.class)
        private Color backgroundColor;  //canvas background color

        @XmlElement(name="MouseCursorColor")
        @XmlJavaTypeAdapter(ColorAdapter.class)
        private Color mouseCursorColor;
        
        @XmlElement(name="MouseCoordFont")
        @XmlJavaTypeAdapter(FontAdapter.class)
        private Font mouseCoordFont;
        
        @XmlElement(name="MeasureScale")
        private UnitMeasure unit;

        @XmlElement(name="UndoStackCount")        
        private int undoStackCount = 1;       
        
        @XmlElement(name="Origin")
        @XmlJavaTypeAdapter(PointAdapter.class)
        private Point origin;
        
        @XmlElement(name="Bounds")
        @XmlJavaTypeAdapter(DimensionAdapter.class)
        private Dimension bounds;
        
        @XmlElement(name="Orientation")        
        private Orientation orientation;
        
        @XmlElement(name="PageSize")
        @XmlJavaTypeAdapter(DimensionAdapter.class)
        private Dimension pageSize;
        
        @XmlElement(name="Margins")
        @XmlJavaTypeAdapter(Rectangle2DAdapter.class)
        private Rectangle2D.Double margins;

        @XmlElement(name="MarginsOn")        
        private boolean marginsOn;
            
        @XmlElement(name="HorizontalRuler")        
        private ZCanvasRuler horizontalRuler;
        
        @XmlElement(name="VerticalRuler")        
        private ZCanvasRuler verticalRuler;
        
        @XmlElement(name="Grid")
        private ZGrid grid;
        
        @XmlElement(name="ZElement")        
        private LinkedList<ZElement> zElements = new LinkedList<>();  //list of all Z-plane objects, first is top, bottom is last
 
        @XmlElement(name="Zoom")
        private double zoom = 1.0;
       
        static Class<?>[] getContextClasses() {
            return new Class<?>[] {UnitMeasure.class, Orientation.class, ZCanvasRuler.class, ZGrid.class};
        }
        
    }
    /*----------------------------------------------------------------------*/
    
    CanvasStore fields = new CanvasStore();
        
    private final float pixScale = SCALE/72.0f;
    
    private final float[] dashedBorder = new float[]{0.0f, pixScale*5.0f, pixScale*5.0f};
    private final float[] altDashedBorder = new float[]{pixScale*5.0f};

    private final int DRAG_BOX_SIZE = (int)(10 * pixScale);
    
    
    //private final ArrayList<ZElement> selectedElements = new ArrayList<>();
    private LinkedList<ZElement> clipboard;

    private UndoStack undoStack;

    private Cursor currentCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    
    private ZElement passThruElement = null;

    private Point2D selectedObj_mousePoint;
    private double selectedObj_yOffset;
    private double selectedObj_xOffset;
    private double selectedObj_xOffset_toRightCorner;
    private double selectedObj_yOffset_toRightCorner;

    private Point2D mouseIn;
    private boolean selectedElementResizeOn = false;
    private Rectangle2D selectedResizeElementOrigDim;
    private ZElement selectedResizeElement = null;
    private ZElement lastSelectedElement;
    
    private Timer animationTimer;
    private HighlightAnimation animation = HighlightAnimation.Fast;

    private boolean selectedAlternateBorder;
    private Method lastMethod = null;
    private String lastMethodName = null;
    private Object[] lastMethodParams;
    private boolean zoomEnabled = true;
    
    private boolean canvasModified;  //tracks any changes to the Z-plane order of the elements
    private boolean printOn = false;  //if printing is turned on (hides some pieces during paint)
    
    private long mouseWheelLastMoved = -1;
    private long mouseFirstPressed = -1;
    private DrawClient drawClient = null;
    
    private boolean shiftPressed = false;
    private boolean altPressed = false;
    private boolean shearXPressed = false;
    private boolean shearYPressed = false;
    private Point2D selectedMouseDrag;
    private Point2D mouseDrag;
    private Point2D mousePress;
    private Point2D selectedMousePress;
    private ZCanvasContextMenu contextMenu;
    
    private final HashMap<UUID, ZElement> uuidMap = new HashMap<>();  //quick lookup of UUID 
    
    private final ArrayList<ZCanvasEventListener> canvasEventListeners = new ArrayList<>();

    //For restoration by JAXB
    private ZCanvas() {
        super();
    }
    
    /**
     * Create the ZCanvas
     * @param background the background color of the canvas
     * @param mouseCoordFont the font of the mouse coordinates, null to remove the coordinates from being drawn
     * @param unitType the unit to use for mouse coordinates
     * @param mouseCursorColor color for the mouse cursor lines, use null to remove the cursor
     * @param undoStackCount the amount of history to keep in undo (the higher the count, the more memory used
     * @param origin the desired coordinate origin of the top left corner. If null, origin = 0,0
     * @param bounds the maximum bounds of the canvas (width and height).  If null, unlimited
     */
    public ZCanvas(Color background, Font mouseCoordFont, UnitMeasure unitType, Color mouseCursorColor, int undoStackCount, Point origin, Dimension bounds) {
        super();
        fields.backgroundColor = background;
        fields.unit = unitType;
        fields.mouseCoordFont = mouseCoordFont;
        fields.mouseCursorColor = mouseCursorColor;
        fields.undoStackCount = undoStackCount;
        if (origin == null)
            fields.origin = new Point(0,0);
        else
            fields.origin = new Point(origin);
        
        setCanvasBounds(bounds);
        init();
    }
    
   
    private void init() {
    
        this.setFocusTraversalKeysEnabled(false);
        undoStack = new UndoStack(fields.undoStackCount);
        canvasModified = false;
        
        uuidMap.clear();
        for (ZElement e : fields.zElements)  //add all the elements to the hash map
            uuidMap.put(e.getUUID(), e);
        
        
        addMouseListener(this);	
        addMouseMotionListener(this);    
        addMouseWheelListener(this);
       

        animationTimer = new Timer(animation.delaySpeed(), new ActionListener() {  //timer to animate the selected dashed line border, draw coordinates
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean repaint = false;
                if (hasSelectedElements() && animation != HighlightAnimation.None) {
                    selectedAlternateBorder = !selectedAlternateBorder;
                    repaint = true;
                }
                if (selectedMousePress != null && System.nanoTime() - mouseFirstPressed > 500000000) {
                    selectedMouseDrag = selectedMousePress;
                    repaint = true;
                }
                    
                if (repaint)
                    repaint();
            }
        });
        animationTimer.start();
        
        
        //Set up the standard hotkeys for the canvas, more can be added by custom implementations
        InputMap im = getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

       
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "Tab");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "Plus");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "Minus");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_DOWN_MASK), "ShiftPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, KeyEvent.ALT_DOWN_MASK), "AltPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true), "ShiftReleased");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true), "AltReleased");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK), "S_AltPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "S_AltShiftPressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "MoveLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "MoveRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "MoveUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "MoveDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.META_DOWN_MASK), "Increase");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK), "Decrease");
 
       
        am.put("Tab", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                selectNextElement();
            }
        });
        am.put("Plus", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (passThruElement == null)  //don't zoom if another element is pass thru (might be using the keyboard)
                    zoomIn();
            }
        });
        am.put("Minus", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (passThruElement == null)  //don't zoom if another element is pass thru (might be using the keyboard)
                    zoomOut();
            }
        });
        am.put("ShiftPressed", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftPressed = true;
            }
        });
        am.put("ShiftReleased", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftPressed = false;
                shearYPressed = false;
            }
        });
        am.put("S_AltPressed", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                shearYPressed = false;
                shearXPressed = true;
            }
        });
        am.put("S_AltShiftPressed", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                shearYPressed = true;
                shearXPressed = false;
            }
        });
        am.put("AltPressed", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                altPressed = true;
            }
        });
        am.put("AltReleased", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                shearYPressed = false;
                shearXPressed = false;
                altPressed = false;
            }
        });
        am.put("MoveLeft", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelected(-1/SCALE, 0);
            }
        });
        am.put("MoveRight", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelected(1/SCALE, 0);
            }
        });
        am.put("MoveUp", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelected(0, -1/SCALE);
            }
        });
        am.put("MoveDown", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelected(0, 1/SCALE);
            }
        });
        
        am.put("Increase", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent a) {
                for (ZElement e : getSelectedElements()) {
                    boolean r = e.isResizable();
                    e.setResizable(true);  //override resizable
                    e.increaseSizeMaintainAspect(1, 1, SCALE);
                    e.setResizable(r);
                }
            }
        });
        am.put("Decrease", new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent a) {
                for (ZElement e : getSelectedElements()) {
                    boolean r = e.isResizable();
                    e.setResizable(true);  //override resizable
                    e.increaseSizeMaintainAspect(-1, 1, SCALE);
                    e.setResizable(r);
                }
            }
        });
        
        
        
             
        updatePreferredSize();
        repaint();

    }
    
    /**
     * Cleanup the canvas, removing all elements.  Typically used when loading a new canvas or clearing out a canvas to be reused.
     */
    public void clearAll() {
        
        //Tell all they were removed (use array to avoid elements deleting other elements (concurrent mod issues)
        ZElement[] elements = new ZElement[fields.zElements.size()];
        fields.zElements.toArray(elements);
        
        fields.zElements.clear();

        for (ZElement e : elements) 
            e.removedFrom(this);
        
        uuidMap.clear();
        repaint();
 
    }
    
    
    /**
     * Places the popup frame (container) to the right of the canvas's frame
     * @param c 
     */
    public void arrangePopup(Container c) {
        Component parent = SwingUtilities.getRoot(this);
        c.setLocation(parent.getX() + parent.getWidth(), parent.getY() + parent.getHeight()/2 - c.getHeight()/2);   
    }
    
    /**
     * Retrieve the number of elements on the canvas
     * @return the number of elements
     */
    public int getNumElements() {
        return fields.zElements.size();
    }
    
    /**
     * Returns the Z-plane layer of the specified element, from 0 (top) to the last value (bottom)
     * @param e the element to find
     * @return the layer number, or -1 if not found
     */
    public int getElementLayerPosition(ZElement e) {
        int i=0;
        for (ZElement ze : fields.zElements) {
            if (ze == e)
                return i;
            i++;
        }
        return -1;
    }
    
    private boolean hasSelectedElements() {  //A little faster than getSelectedElements, because it returns on first one found
        for (ZElement e : fields.zElements) {
            if (e.isSelected())
                return true;
        }
        return false;
    }
    
    private ArrayList<ZElement> getSelectedElements() {
        ArrayList<ZElement> selected = new ArrayList<>();
        for (ZElement e : fields.zElements) {
            if (e.isSelected())
                selected.add(e);
        }
        return selected;
    }
    
    public void setHighlightAnimation(HighlightAnimation a) {
        animation = a;
        canvasModified = true;
        animationTimer.setDelay(animation.delaySpeed());
        animationTimer.start();
        
        repaint();
    }
    
    /**
     * Set the horizontal ruler
     * @param r the ruler to use, or null to remove
     */
    public void setHorizontalRuler(ZCanvasRuler r) {
        fields.horizontalRuler = r;
        canvasModified = true;
        repaint();
    }
    
     /**
     * Set the vertical ruler
     * @param r the ruler to use, or null to remove
     */
    public void setVerticalRuler(ZCanvasRuler r) {
        fields.verticalRuler = r;
        canvasModified = true;
        repaint();
    }
    
    /**
     * Set the page size and orientation for printing.  The page is colored to the canvas background color
     * @param pageSize the page size for printing purposes
     * @param o the orientation, for printing purposes
     */
    public void setPageSize(Dimension pageSize, Orientation o) {
        fields.pageSize = new Dimension(pageSize);
        fields.orientation = o;
        
        if (fields.grid != null)
            fields.grid.changeSize(pageSize.width, pageSize.height, .0001, SCALE);
             
        canvasModified = true;
        repaint();
    }
    
    
    /**
     * Set the page margins (the rectangle defines the interior area). 
     * @param margins the margin rectangle, or null to remove the margins
     */
    public void setPageMargins(Rectangle2D margins) {
        fields.margins = margins == null ? null : new Rectangle2D.Double(margins.getX(), margins.getY(), margins.getWidth(), margins.getHeight());
        repaint();     
    }
    
    /**
     * Returns true if the margins are set to on. Regardless of the setting, they are not shown until they have been defined with setPageMargins()
     * @return 
     */
    public boolean areMarginsOn() {
        return fields.marginsOn;
    }
    
    /**
     * Set the margins on or off.  Regardless of the setting, they are not shown until they have been defined with setPageMargins()
     * @param on 
     */
    public void marginsOn(boolean on) {
        fields.marginsOn = on;
        repaint();
    }
    
    /**
     * Set the grid to use
     * @param g the grid to use, or null to remove
     */
    public void setGrid(ZGrid g) {
        if (g == null && fields.grid != null) 
            fields.grid.removedFrom(this);
        
            
        fields.grid = g;
        if (fields.grid != null)
            fields.grid.addedTo(this);
        
        if (fields.grid != null && fields.pageSize != null) 
            fields.grid.changeSize(fields.pageSize.width, fields.pageSize.height, .0001, SCALE);
            
        canvasModified = true;
        repaint();
    }
    
    /**
     * True if there is a grid set, false otherwise
     * @return 
     */
    public boolean hasGrid() {
        return fields.grid != null;
    }
    
    
    /**
     * Change the measure unit
     * @param u the new measure unit (cannot be null)
     */
    public void changeUnit(UnitMeasure u) {
        fields.unit = u;
        canvasModified = true;
        for (ZElement e : fields.zElements)
            e.unitChanged(this, u);
        repaint();
    }
    
    
    public UnitMeasure getUnit() {
        return fields.unit;
    }
    
    public Color getMouseCrosshairColor() {
        return fields.mouseCursorColor;
    }
    
    /**
     * Set the color of the crosshair cursor 
     * @param c the color to use, or null to remove
     */
    public void setMouseCrosshairColor(Color c) {
        fields.mouseCursorColor = c;
        repaint();
    }
    
    /**
     * Register an event listener with the canvas.  When an element is selected or other actions occur, the listener is notified.
     * @param l the listener to register, if already registered, fails silently
     */
    public void registerEventListener(ZCanvasEventListener l) {
        if (!canvasEventListeners.contains(l))
            canvasEventListeners.add(l);
    }
    
    /**
     * Deregisters an event listener 
     * @param l the listener to deregister, if not registered, fails silently
     */
    public void deRegisterEventListener(ZCanvasEventListener l) {
        canvasEventListeners.remove(l);
    }
    
    
    //This method is overriden to force the component to draw only to its bounds. 
    @Override
    public void reshape(int x, int y, int width, int height) {
        
        if (fields.bounds != null) {
            if (width > fields.bounds.width * fields.zoom + fields.origin.x)
                width = (int)(fields.bounds.width * fields.zoom + fields.origin.x);
            if (height > fields.bounds.height * fields.zoom + fields.origin.y)
                height = (int)(fields.bounds.height * fields.zoom + fields.origin.y);
        }
        
        super.reshape(x, y, width, height);
        repaint();     
    }
    
    /**
     * Returns the origin in values of units
     * @return 
     */
    public Point2D getOrigin() {
        return new Point2D.Double(fields.origin.getX()/SCALE, fields.origin.getY()/SCALE);
    }
    
    /**
     * Provides the page drawing area of the canvas, in units 
     * @return a Rectangle2D where x,y are the origin and width,height are the bounds, returns null if unbounded
     */
    public Rectangle2D getCanvasBounds() {
        if (fields.pageSize == null)
            return null;
        else
            return new Rectangle2D.Double(fields.origin.getX()/SCALE, fields.origin.getY()/SCALE, fields.pageSize.width/SCALE, fields.pageSize.height/SCALE);
    }
    
    
    /**
     * Provides the page drawing area of the canvas, in pixels 
     * @return a Rectangle2D where x,y are the origin and width,height are the bounds, returns null if unbounded
     */
    public Rectangle getCanvasPixelBounds() {
        if (fields.pageSize == null)
            return null;
        else
            return new Rectangle((int)fields.origin.getX(), (int)fields.origin.getY(), fields.pageSize.width, fields.pageSize.height);
    }
    
    
    public Orientation getOrientation() {
        return fields.orientation;
    }
    
    /**
     * Sets the drawing area of the canvas, in pixels 
     * @param bounds the drawing area dimension
     */
    public final void setCanvasBounds(Dimension bounds) {
        if (bounds != null) { 
            fields.bounds = new Dimension(bounds.width, bounds.height);
        }
        
        canvasModified = true;
        updatePreferredSize();
        repaint();
    }
    
    private void updatePreferredSize() {
        if (fields.bounds == null)
            setPreferredSize(null);
        else {
            Dimension d = new Dimension((int)(fields.bounds.width * fields.zoom + fields.origin.x), (int)(fields.bounds.height * fields.zoom + fields.origin.y));
            setPreferredSize(d);
            setSize(d);
        }
        canvasModified = true;

        revalidate();
    }
      
    public double getScale() {
         return SCALE;
    }

    /**
     * Set the context menu (generally right mouse click popup) when selecting an element
     * @param m the menu to set, use null to remove
     */
    public void setContextMenu(ZCanvasContextMenu m) {
        contextMenu = m;
    }
    
    /**
     * If the canvas is being drawn usign a draw client, return true, else false
     * @return 
     */
    public boolean usingDrawClient() {
        return drawClient != null;
    }
    
    /**
     * Starts drawing using the specified draw client.
     * @param c the desired drawing client
     * @return true if accepted, false if there is already a draw client
     */
    public boolean drawOn(DrawClient c) {
        if (drawClient != null)
            return false;
        
        drawClient = c;
        for (ZCanvasEventListener l : canvasEventListeners)
            l.canvasHasDrawClient(true);
        
        return true;
    }
    
    /**
     * Stops the draw client drawing.
     */
    public void drawOff() {
        drawClient = null;
        for (ZCanvasEventListener l : canvasEventListeners)
            l.canvasHasDrawClient(false);
        
        setCurrentCursor(Cursor.getDefaultCursor());

        repaint();
    }
    
    /**
     * Restore the view to the default of no zoom and 0,0 at the top left corner
     */
    public void resetView() {
        
        if (!zoomEnabled)
            return;
               
        fields.zoom = 1.0;
        updatePreferredSize();
        repaint();
        for (ZCanvasEventListener l : canvasEventListeners)
            l.canvasChangedZoom();
    }
    
    
    public double getZoomFactor() {
        return fields.zoom;
    }
    
    
    /**
     * Turns the zoom capability on and off
     * @param e true if zoom is allowed, false otherwise
     */
    public void zoomEnabled(boolean e) {
        zoomEnabled = e;
    }
    
    /**
     * Returns the state of the zoom enable.  Note: the zoom enable is not saved in the CanvasStore
     * @return 
     */
    public boolean zoomEnabled() {
        return zoomEnabled;
    }
    
    /**
     * Zoom in, to 4:1
     */
    public void zoomIn() {
        
        if (!zoomEnabled)
            return;
        
        if (fields.zoom < 8.0) {
            fields.zoom += .25;
         
            updatePreferredSize();
            repaint();
            for (ZCanvasEventListener l : canvasEventListeners)
                l.canvasChangedZoom();
        }
    }
    
    /**
     * Zooms out, as far as 1:1.5 
     */
    public void zoomOut() {
        
        if (!zoomEnabled)
            return;
        
        if (fields.zoom > 0.5) {
            fields.zoom -= .25;
        
            updatePreferredSize();
            repaint();
            for (ZCanvasEventListener l : canvasEventListeners)
                l.canvasChangedZoom();
        }
    }
    
    /**
     * Gets the current pixel scaling of the canvas, 1.0 is normal view, higher values are zoomed in view
     * @return 
     */
    public double getPixScale() {
        return fields.zoom * pixScale;
    }
    
    
    private Rectangle2D getDragSelectRectangle() {
    
        if (mouseDrag == null)
            return null;
        
        double x = mousePress.getX() < mouseDrag.getX() ? mousePress.getX() : mouseDrag.getX();
        double y = mousePress.getY() < mouseDrag.getY() ? mousePress.getY() : mouseDrag.getY();
        double w = mousePress.getX() < mouseDrag.getX() ? mouseDrag.getX() - mousePress.getX() : mousePress.getX() - mouseDrag.getX();
        double h = mousePress.getY() < mouseDrag.getY() ? mouseDrag.getY() - mousePress.getY() : mousePress.getY() - mouseDrag.getY();
        
        return new Rectangle2D.Double(x, y, w, h);
    }
    
   
    private void setLastMethod(String methodName, String friendlyName, Object... params) {
        try {
            Class[] classes = new Class[params.length];
            for (int i=0; i<params.length; i++)
                classes[i] = params[i].getClass();
            
            lastMethod = ZCanvas.class.getMethod(methodName, classes); 
            lastMethodName = friendlyName;
            lastMethodParams = params;
        } catch (NoSuchMethodException |SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public String getLastRepeatOperation() {
        if (lastMethod == null)
            return null;
        else
            return lastMethodName;            
    }
    

    /**
     * Saves the canvas context to the undo stack.  Useful when modifications to elements occur outside the ZCanvas class
     */
    public void saveCanvasContext() {
        undoStack.saveContext(fields.zElements);
    }
    
    /**
     * Enables or suspends saving the canvas context to the undo stack.  When suspended, canvas changes after this call cannot be 
     * undone. This is useful if a feature needs to change multiple items at once, and its not desirable to back out any one 
     * individual change but only the whole thing.
     * @param enable true to enable the undo, false to disable
     */
    public void enableUndoContextSave(boolean enable) {
        if (enable)
            undoStack.resumeSave();
        else
            undoStack.suspendSave();
    }
    
    private void restoreContext(LinkedList<ZElement> restoreContext) {
        if (restoreContext != null) {
            
            clearAll();  //clear out all elements
          
            fields.zElements = restoreContext;  //replace all the elements

            for (ZElement e : fields.zElements)  //restore all the elements to the hash map
                uuidMap.put(e.getUUID(), e);
        
            for (ZElement e : fields.zElements)
                e.addedTo(this); //tell they were added
            
            
            selectNone();
        }
    }
    
    /**
     * Removes the previous change from the canvas
     */
    public void undo() {
        
        restoreContext(undoStack.undo(fields.zElements));
        repaint();
    }
    
    
    public void redo() {
                
        restoreContext(undoStack.redo());
        repaint();
        
    }
    
    
    /**
     * Repeats the previous change 
     */
    public void repeat() {
        if (!hasSelectedElements() || passThruElement != null || lastMethod == null) 
            return;
        
        try {
            lastMethod.invoke(this, lastMethodParams);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void clearShear() {
        setShear(true, 0.0);
        setShear(false, 0.0);
        setLastMethod("clearShear", "Clear Shear");

    }
    
    public void setShear(Boolean horiz, Double ratio) {
        ArrayList<ZElement> selectedElements = getSelectedElements();
        
        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
                       
        undoStack.saveContext(fields.zElements);

        for (ZElement e : selectedElements) {
            if (horiz)
                e.setShearX(ratio);
            else
                e.setShearY(ratio);
        }
        
        setLastMethod("setShear", "Set Shear", horiz, ratio);
        repaint();     

    }
    
    /**
     * Flips the selected elements.
     * @param horiz if true, flips horizontal, else vertical
     */
    public void flip(Boolean horiz) {       
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
                       
        undoStack.saveContext(fields.zElements);

        for (ZElement e : selectedElements) {
            if (horiz)
                e.flipHorizontal();
            else
                e.flipVertical();
        }
        
        setLastMethod("flip", "Flip " + (horiz ? "Horizontal" : "Vertical"), horiz);
        repaint();     
   
    }

    /**
     * Rotates the selected elements clockwise
     */
    public void rotate90CW() {
        rotate90(true);
        setLastMethod("rotate90CW", "Rotate Clockwise");
    }
    
    /**
     * Rotates the selected elements counter-clockwise
     */
    public void rotate90CCW() {
        rotate90(false);
        setLastMethod("rotate90CCW", "Rotate Counter-clockwise");
    }
    
    /**
     * Rotate the object 90 degrees
     * @param clockwise true to rotate clockwise, false to rotate counterclockwise
     */
    private void rotate90(boolean clockwise) {
        
        ArrayList<ZElement> selectedElements = getSelectedElements();
       
        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
 
        undoStack.saveContext(fields.zElements);
        
        for (ZElement selectedElement : selectedElements) {
            double curr = selectedElement.getRotation();
            double newRotation = curr;

            if (curr % 90.0 == 0) { //already was at floor or ceil
                if (clockwise)
                    newRotation += 90.0;
                else
                    newRotation -= 90.0;
            } 
            else {
                if (clockwise)
                    newRotation = 90.0 * Math.ceil(curr / 90.0);
                else
                    newRotation = 90.0 * Math.floor(curr / 90.0);

            }
            selectedElement.setRotation(newRotation);
        }
        repaint();
    }
    
    public void moveSelected(double x, double y) {
         
        for (ZElement e : getSelectedElements()) {
           e.move(x, y, getMaxXPosition()/SCALE, getMaxYPosition()/SCALE);
        }
        repaint();
    }
    
    /**
     * Align the selected elements to the desired alignment type.  At least 2 elements must be selected
     * @param atype the desired alignment type
     */
    public void align(Alignment atype) {
        
        if (atype == Alignment.Centered_Both) {
            align(Alignment.Centered_Horizontally);
            align(Alignment.Centered_Vertically);
            return;
        }
        
        ArrayList<ZElement> selectedElements = getSelectedElements();
        
        if (selectedElements.size() <= 1 || passThruElement != null) 
            return;
 
        undoStack.saveContext(fields.zElements);
        
        ZElement key = selectedElements.get(0);
        ZElement key2 = key;
        for (ZElement e : selectedElements) {  //loop through elements, looking for the key - closest to the edge
            
            Rectangle2D p = e.getBounds2D(SCALE);
            Rectangle2D k = key.getBounds2D(SCALE);
            
            switch (atype) {
                case Left_Edge:
                case Centered_Vertically:
                    if (p.getX() < k.getX())
                        key = e;
                    break;
                case Right_Edge:
                    if (p.getX() + p.getWidth() > k.getX() + k.getWidth())
                        key = e;
                    break;
                case Top_Edge:
                case Centered_Horizontally:
                    if (p.getY() < k.getY())
                        key = e;
                    break;
                case Bottom_Edge:
                    if (p.getY() + p.getHeight() > k.getY() + k.getHeight())
                        key = e;
                    break;
                case Auto:
                    if (p.getX() < k.getX())  //vert
                        key = e;
                    if (p.getY() < k.getY())  //horiz
                        key2 = e;
                    break;
            }  
        }
        
        double minVert = 0;
        double minHoriz = 0;
        Rectangle2D k = key.getBounds2D(SCALE);  //vertical key
        Rectangle2D k2 = key2.getBounds2D(SCALE); //horizontal key

        if (atype == Alignment.Auto) {  //find the minimum
            
            for (ZElement e : selectedElements) {
                Rectangle2D p = e.getBounds2D(SCALE);
                minVert += Math.abs(k.getX() - p.getX());   //vert delta
                minHoriz += Math.abs(k2.getY() - p.getY());  //horiz delta
            }
            
            if (minVert < minHoriz) {
                atype = Alignment.Centered_Vertically;  //already using this key
            }
            else {
                atype = Alignment.Centered_Horizontally;
                key = key2;
            }
        }
            
            
             
        for (ZElement e : selectedElements) {
                    
            Rectangle2D p = e.getBounds2D(SCALE);

             switch (atype) {
                case Left_Edge:
                    e.reposition(k.getX()/SCALE, p.getY()/SCALE, Double.MAX_VALUE, Double.MAX_VALUE);
                    break;
                case Right_Edge:
                    e.reposition((k.getX() + k.getWidth() - p.getWidth())/SCALE, p.getY()/SCALE, Double.MAX_VALUE, Double.MAX_VALUE);
                    break;
                case Top_Edge:
                    e.reposition(p.getX()/SCALE, k.getY()/SCALE, Double.MAX_VALUE, Double.MAX_VALUE);
                    break;
                case Bottom_Edge:
                    e.reposition(p.getX()/SCALE, (k.getY() + k.getHeight() - p.getHeight())/SCALE, Double.MAX_VALUE, Double.MAX_VALUE);
                    break;
                case Centered_Vertically:
                    double ctrX = k.getX() + k.getWidth()/2;
                    e.reposition((ctrX - p.getWidth()/2)/SCALE, p.getY()/SCALE, Double.MAX_VALUE, Double.MAX_VALUE);
                    break;
                case Centered_Horizontally:
                    double ctrY = k.getY() + k.getHeight()/2;
                    e.reposition(p.getX()/SCALE, (ctrY - p.getHeight()/2)/SCALE, Double.MAX_VALUE, Double.MAX_VALUE);
                    break;
                    
            }
        }
        repaint();     
        setLastMethod("align", "Align " + atype.toString().replace("_", " "), atype);

    }
    
    
    
    
    

    /**
     * For all selected elements, builds a ZGroupedElement from them, removes the elements the canvas, and adds the ZGroupedElement to the 
     * canvas, and selects it.
     */
    public void groupSelectedElements() {
        
        ArrayList<ZElement> selectedElements = getSelectedElements();
        Iterator<ZElement> it = selectedElements.iterator();
        while (it.hasNext()) {  //remove any unmutable objects
            if (!it.next().isMutable())
                it.remove();
        }

        if (selectedElements.size() <= 1 || passThruElement != null) 
            return;

        undoStack.saveContext(fields.zElements);

        undoStack.suspendSave();  //don't push all the remove and add changes to the undo stack
        
        Collections.reverse(selectedElements); //the selected elements are ordered with top z plane first.  But the Grouped Element draws grouped elements in the order provided, so we need to reverse the list
        ZGroupedElement group = ZGroupedElement.createGroup(selectedElements);  //create the group of elements
        
        for (ZElement e: selectedElements) //remove all selected
            removeElement(e);
        
        addElement(group);  //add the group element
        selectNone();
        group.select();
        
        undoStack.resumeSave();

    }
    
    /**
     * For all selected elements that are of type ZGroupedElement, removes the elements from the group, adds them back to the canvas,
     * and deletes the ZGroupedElement
     */
    public void ungroup() {

        ArrayList<ZElement> selectedElements = getSelectedElements();
        
        if (selectedElements.isEmpty() || passThruElement != null) 
            return;

        int numGroups = 0;
        Iterator<ZElement> it = selectedElements.iterator();
        while (it.hasNext()) {   
            if (it.next() instanceof ZGroupedElement)
                numGroups++;
        }
        if (numGroups == 0) //nothing to ungroup
            return;
        
        undoStack.saveContext(fields.zElements);

        undoStack.suspendSave();  //don't push all the removed and restored element changes to the undo stack

        ArrayList<ZElement> restoredElements = new ArrayList<>();  //create a temporary list to hold all restored elements

        it = selectedElements.iterator();
        while (it.hasNext()) {
            
            ZElement e = it.next();
            
            if (e instanceof ZGroupedElement) {
                
                ZGroupedElement g =  (ZGroupedElement)e;
                ArrayList<ZElement> ungroup = g.ungroup();
                
                for (ZElement u : ungroup)
                    restoredElements.add(u); 
                
                it.remove();
                this.removeElement(g);
                
            }
            
            
        }
        
        selectNone();
        for (ZElement e : restoredElements) {  //added back in the z-plane order such that the top zplane is last in list
            this.addElement(e);
            e.select();
        }
         
        undoStack.resumeSave();

    }
    
    
    /**
     * Merge the selected elements, starting with the lowest layer selected and applying the operation to each next layer selected.  Only elements that
     * extend ZAbstractShape can be combined, the others are ignored. The ZAbstractShapes are removed from the canvas, and merged into a new ZShape, which
     * is added back to the canvas.
     * The attributes of the newly combined shape are those of the lowest layer selected ZAbstractShape.  
     * @param operation the operation to apply
     * @return the number of shapes combined including the first selected one. If there are no selected ZAbstractShape
     * elements, 0 is returned.  If only one ZAbstractShape is selected, zero is returned and it is not modified.
     * If the combine operation results in a shape with no area, then -1 is returned.
     */
    public int combineSelectedElements(CombineOperation operation) {
                
        ArrayList<ZElement> selectedElements = getSelectedElements();
        Iterator<ZElement> it = selectedElements.iterator();
        while (it.hasNext()) {  //remove any unmutable objects
            if (!it.next().isMutable())
                it.remove();
        }

        if (selectedElements.size() <= 1 || passThruElement != null) 
            return 0;

        undoStack.saveContext(fields.zElements);
        
        undoStack.suspendSave();  //don't push all the removal and adding to the undo stack

        
        ZAbstractShape ref = null;
        ArrayList<ZAbstractShape> combineList = new ArrayList<>();
        
        for (int i=selectedElements.size()-1; i>=0; i--) {  //go backward, from the one at the bottom of the Z stack first
            
            ZElement e = selectedElements.get(i);
            
            if (e instanceof ZAbstractShape) {  
                 
                ZAbstractShape abs = (ZAbstractShape)e;
                
                //Check first one - it becomes reference element
                if (ref == null) 
                    ref = abs;  //lowest layer selected is the reference element            
                else 
                    combineList.add(abs);  //add all others to our list
               
            }
        }
        
        if (ref == null || combineList.isEmpty())  //nothing to merge
            return 0;
        
        //Remove all abstract shapes to be merged from the canvas - except the reference (it will be replaced) 
        for (ZElement e : combineList)
            removeElement(e);
        
        
        Shape mergedShape = ref.combineWith(operation, combineList);  //combine reference with list of other elements
        if (mergedShape == null)  //combine resulted in shape with no area
            return -1;
           
        
        Rectangle2D bounds = mergedShape.getBounds2D();  //find the new position of the joined object

        //Move back to zero position reference
        AffineTransform pos = AffineTransform.getTranslateInstance(-bounds.getX(), -bounds.getY());
        mergedShape = pos.createTransformedShape(mergedShape);
   
        
        ZShape shape = new ZShape(bounds.getX(), bounds.getY(), mergedShape, 0.0, true, true, true, ref.getOutlineWidth(), ref.getOutlineColor(), ref.getDashPattern(), ref.getFillColor(), ref.getPaintAttributes(), ref.getCustomStroke(), ref.getOutlineStyle());               
        
        this.replaceElement(ref, shape);
        
        lastMethod = null;
        selectNone();
        selectElement(shape, false);
        
        undoStack.resumeSave(); 
  
        repaint();
        return combineList.size() + 1;
    }
   
    
    /**
     * Changes the background color of the canvas
     * @param c the color to set
     */
    public void setCanvasBackgroundColor(Color c) {
        fields.backgroundColor = c;
        lastMethod = null;
        canvasModified = true;

        repaint();
    }
    
    
    /**
     * Sets the outline width of the selected elements
     * @param width the desired width, 0 for no outline
     */
    public void setOutlineWidth(Float width) {
        
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
        
        undoStack.saveContext(fields.zElements);
    
        for (ZElement selectedElement : selectedElements)
            selectedElement.setOutlineWidth(width);
        
        setLastMethod("setOutlineWidth", "Line Width", width);
        repaint();
        
    }
    
    public void setOutlineStyle(StrokeStyle borderStyle) {
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
        
        undoStack.saveContext(fields.zElements);
    
        for (ZElement selectedElement : selectedElements)
            selectedElement.setOutlineStyle(borderStyle);
        
        setLastMethod("setOutlineStyle", "Line Style", borderStyle);
        repaint();
    }
    
    
    /**
     * Sets the selected elements border dash pattern
     * @param dash the desired pattern, or null to use a solid line
     */
    public void setDashPattern(Float[] dash) {

        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
        
        undoStack.saveContext(fields.zElements);
    
        setLastMethod("setDashPattern", "Dash Pattern", (Object)dash);
        
        if (dash.length == 0)
            dash = null;
        
        for (ZElement selectedElement : selectedElements)
            selectedElement.setDashPattern(dash);
        
        repaint();
        
    }
    
    /**
     * Sets the outline color of the selected elements
     * @param c 
     */
    public void setOutlineColor(Color c) {

        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;
        
        undoStack.saveContext(fields.zElements);

        for (ZElement selectedElement : selectedElements)
            selectedElement.setOutlineColor(c);
        
        setLastMethod("setOutlineColor", "Line Color", c);
        repaint();
    }
    
    /**
     * Sets the fill color of the selected elements
     * @param c color to fill, null to remove color
     */
    public void setFillColor(Color c) {

        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;

        undoStack.saveContext(fields.zElements);
    
        for (ZElement selectedElement : selectedElements)
            selectedElement.setFillColor(c);
        
        setLastMethod("setFillColor", "Fill Color", c);
        repaint();     

    }
    
    /**
     * Removes the fill color and paint attributes of the selected elements
     */
    public void removeFill() {

        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty() || passThruElement != null) 
            return;

        undoStack.saveContext(fields.zElements);
    
        for (ZElement selectedElement : selectedElements)
            selectedElement.removeFill();
        
        setLastMethod("removeFill", "Remove Fill");
        repaint();     

    }
   
    
    /**
     * Adds an element to the canvas, on the top layer
     * @param e element to add
     * @return true if added, false if already exists on canvas
     */
    public boolean addElement(ZElement e) {
        if (fields.zElements.contains(e))
            return false;
        
        undoStack.saveContext(fields.zElements);

        fields.zElements.addFirst(e);
        uuidMap.put(e.getUUID(), e);

        e.addedTo(this);

        lastMethod = null;
        canvasModified = true;
        repaint();     

        return true;
    }
    
    /**
     * Removes an element from the canvas.  Fails silently if element is not on canvas
     * @param e element to remove
     */
    public void removeElement(ZElement e) {
        if (!fields.zElements.remove(e))
            return;
            
        undoStack.saveContext(fields.zElements);

        uuidMap.remove(e.getUUID());
        e.removedFrom(this);
    
        canvasModified = true;

        lastMethod = null;
        repaint();     

    }
    
    /**
     * Replaces an element in the canvas with another one.  The Z position is maintained.
     * @param replace element to replace
     * @param with element to replace with
     * @return true if the element was replaced, false if "replace" element was not found
     */
    public boolean replaceElement(ZElement replace, ZElement with) {
        if (!fields.zElements.contains(replace))
            return false;
        
        undoStack.saveContext(fields.zElements);
        
        fields.zElements.set(fields.zElements.indexOf(replace), with);
        
        replace.removedFrom(this);
        with.addedTo(this);
                
        repaint();     

        return true;
    }
    
    
    /**
     * Sends the selected elements to the lowest Z plane layer, fails silently if nothing selected
     */
    public void moveToBack() {
 
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty())
            return;

        undoStack.saveContext(fields.zElements);
        
        for (ZElement selectedElement : selectedElements) {
            fields.zElements.remove(selectedElement);
            fields.zElements.addLast(selectedElement);   
        }
        canvasModified = true;

        setLastMethod("moveToBack", "Move To Back");
        repaint();     
    }
    
    /**
     * Moves the selected elements to the top Z plane layer, fails silently if nothing selected
     */
    public void moveToFront() {

        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty())
            return;

        undoStack.saveContext(fields.zElements);
        
        for (ZElement selectedElement : selectedElements) {
            fields.zElements.remove(selectedElement);
            fields.zElements.addFirst(selectedElement);
        }
        canvasModified = true;

        setLastMethod("moveToFront", "Move To Front");
        repaint();

    }
    
    /**
     * Moves the selected elements one Z plane layer backward, unless already at back.  Fails silently if not found.
     */
    public void moveBackward() {
                
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty())
            return;

        undoStack.saveContext(fields.zElements);
        
        for (ZElement selectedElement : selectedElements) {

            int index = fields.zElements.indexOf(selectedElement);
            if (index < 0 || index == fields.zElements.size()-1)  //not found or already at back
                continue;

            fields.zElements.remove(index);
            fields.zElements.add(index+1, selectedElement);
        }
        canvasModified = true;

        setLastMethod("moveBackward", "Move Backward");
        repaint();
        
    }
   
    /**
     * Moves the selected elements one Z plane layer forward, unless already at front.  Fails silently if not found.
     */
    public void moveForward() {
        
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty())
            return;

        undoStack.saveContext(fields.zElements);
        
        for (ZElement selectedElement : selectedElements) {
        
            int index = fields.zElements.indexOf(selectedElement);
            if (index < 0 || index == 0)  //not found or already at front
                continue;

            fields.zElements.remove(index);
            fields.zElements.add(index-1, selectedElement);
        }

        canvasModified = true;

        setLastMethod("moveForward", "Move Forward");
        repaint();
    }
    
    /**
     * Search all the elements on the canvas for the matching UUID
     * @param id the UUID of the element to find
     * @return the found element, or null if not found
     */
    public ZElement getElementByUUID(UUID id) {
        return uuidMap.get(id);
    }
    
    
    /**
     * Return all the elements that are instances of the classType.
     * @param classType the classType to match, elements must be equal, subclasses of, or implement the classType
     * @return 
     */
    public ZElement[] getElementsByClass(Class<? extends ZElement> classType) {
        
        ArrayList<ZElement> list = new ArrayList<>();
        for (ZElement e : fields.zElements) {
            if (classType.isAssignableFrom(e.getClass())) {
                list.add(e);
            }
        }
        ZElement[] array = new ZElement[list.size()];
        list.toArray(array);
        return array;
    }
    
    /**
     * Returns the currently selected elements
     * @return the selected elements, or null if nothing is currently selected
     */
    public ZElement[] getSelectedElementsArray() {
        ArrayList<ZElement> selectedElements = getSelectedElements();
        ZElement[] e = new ZElement[selectedElements.size()];
        selectedElements.toArray(e);
        return e;
    }
    
    /**
     * Returns the lat selected element
     * @return last selected element, null if nothing is selected
     */
    public ZElement getLastSelectedElement() {
        return lastSelectedElement;
    }
    
    
    /**
     * Makes a deep copy of the selected elements and stores it for later.  Does nothing if the control is currently with an element.
     * @return a copy of the copied elements, or null if none was copied
     */
    public ZElement[] copy() {

        ArrayList<ZElement> selectedElements = getSelectedElements();
        Iterator<ZElement> it = selectedElements.iterator();
        while (it.hasNext()) {  //remove any unmutable objects
            if (!it.next().isMutable())
                it.remove();
        }
        
        if (selectedElements.size() > 0) {
            
            clipboard = new LinkedList<>();
            ZElement[] externalCopy = new ZElement[selectedElements.size()];
            int i=0;
            for (ZElement e : selectedElements) {
                clipboard.add(e.copyOf(true));
                externalCopy[i] = e.copyOf(true);
                i++;
            }
            
            return externalCopy;
        }
        else
            return null;
    }
    
    /**
     * Makes a deep copy of the last selected element, if there is one, and returns it _and_ stores it for later.
     * @return a copy of the cut elements, or null if none was cut
     */
    public ZElement[] cut() {
        ZElement[] copied = copy();
        if (copied != null) {
            deleteSelected();
            return copied;
        }
        else
            return null;
    }
    
    
    /**
     * Pastes any stored elements (and slightly shifts it to distinguish it from its source). Does nothing if no element was copied or control is
     * currently with an element.
     * @return true if at least one element was pasted, null otherwise
     */
    public boolean paste() {
        if (clipboard != null && passThruElement == null) {
            selectNone();

            undoStack.saveContext(fields.zElements);
            
            //Since elements are stored top z plane to bottom, use a descending iterator so the first element is the last one pasted
            Iterator<ZElement> it = clipboard.descendingIterator();
            while (it.hasNext())
                paste(it.next());   
            
            return true;
        }
        else
            return false;
             
    }
    
    /**
     * Pastes an external ZElement to the top Z-plane and selects it
     * @param e the element to paste 
     */
    public void paste(ZElement e) {

        e.move(0.2, 0.2, getMaxXPosition()/SCALE, getMaxYPosition()/SCALE);  //move slighty down to distinguish from original
        
        ZElement toPaste = e.copyOf(true);  //make a copy to paste, for multiple pastes
        
        addElement(toPaste);
        toPaste.select();

        repaint();  
    }
    
 
    /**
     * Deletes the selected elements. Does nothing if no element is selected or control is
     * currently with an element.
     */
     public void deleteSelected() {
        ArrayList<ZElement> selectedElements = getSelectedElements();
        Iterator<ZElement> it = selectedElements.iterator();
        while (it.hasNext()) {  //remove any unmutable objects
            if (!it.next().isMutable())
                it.remove();
        }

        if (selectedElements.isEmpty() && passThruElement != null)
            return;

        undoStack.saveContext(fields.zElements);

        it = selectedElements.iterator();
        while (it.hasNext()) {
            ZElement e = it.next();
            fields.zElements.remove(e);
            uuidMap.remove(e.getUUID());
            e.removedFrom(this);
            it.remove();
        }
          
        canvasModified = true;
        selectNone();
        lastSelectedElement = null;
        lastMethod = null;            
        repaint();
     }
    
    
    //Paint the element, if the element has no width or height, provide the canvas width and height
    private void paintElement(Graphics2D g2d, ZElement o, boolean highlightSelectedOnly) {
        if (o != null) {
            
            Rectangle2D r = o.getBounds2D(SCALE);  //find the location and bounds of the element to paint
            AffineTransform t = g2d.getTransform();
            g2d.translate(r.getX() + r.getWidth()/2, r.getY() + r.getHeight()/2);  //translate to the center of the element
            g2d.rotate(Math.toRadians(o.getRotation()));  //rotate
            g2d.shear(o.getShearX(), o.getShearY());
            g2d.translate(-r.getWidth()/2, -r.getHeight()/2);  //translate so that 0,0 is the top left corner
            
            if (!highlightSelectedOnly) {  //paint the element
                o.paint(g2d, SCALE, r.getWidth()<0 ? getScaledWidth() : r.getWidth(), r.getHeight()<0 ? getScaledHeight() : r.getHeight());      
            }
                                 
            if (o.isSelected() && highlightSelectedOnly && r.getWidth() > 0 && r.getHeight() > 0) {  //highlight selected element, just outside its boundaries
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f * (float)(pixScale/fields.zoom), CAP_SQUARE, JOIN_MITER, 10.0f, selectedAlternateBorder ? dashedBorder : altDashedBorder, 0.0f));
                double margin = Math.ceil(o.getOutlineWidth()/2.0) + (1.0 * pixScale/fields.zoom);  //add the outline width, plus 2 pixels out
                g2d.draw(new Rectangle2D.Double(-margin, -margin, r.getWidth()+margin*2, r.getHeight()+margin*2)); 
 
                g2d.setColor(Color.WHITE);
                margin+=pixScale/fields.zoom;
                g2d.draw(new Rectangle2D.Double(-margin, -margin, r.getWidth()+margin*2, r.getHeight()+margin*2)); 
                
                //draw drag box in the corner, if resizable and the 
                if (o.isResizable() && passThruElement == null) {  
                    double dragBoxWidth = DRAG_BOX_SIZE * pixScale/fields.zoom;
                    if (dragBoxWidth*2 < r.getWidth() || dragBoxWidth*2 < r.getHeight()) { //dont' draw drag box if shape is too small
                        Rectangle2D box = new Rectangle2D.Double(r.getWidth()-dragBoxWidth, r.getHeight()-dragBoxWidth, dragBoxWidth, dragBoxWidth);
                        g2d.setStroke(new BasicStroke(1.0f * pixScale/(float)fields.zoom));
                        g2d.setColor(Color.BLACK);  
                        g2d.fill(box);
                        g2d.setColor(Color.WHITE);  
                        g2d.draw(box);                      
                    }
                }
                               
            }

            g2d.setTransform(t);  //restore transform

        } 
    }
    
    /**
     * Select  an element, if the element is on the canvas
     * @param toSel the element to select
     * @param passThru true to also pass through events to the element (aka double click)
     * @return if the element was selected, returns true.  Returns false if the element is not selectable or not found on the canvas.
     */
    public boolean selectElement(ZElement toSel, boolean passThru) {
        
        if (!toSel.isSelectable())
            return false;
                    
        for (ZElement e : fields.zElements) {
            if (e.equals(toSel)) {
                
                e.select();
                lastSelectedElement = e;   
                for (ZCanvasEventListener l : canvasEventListeners)
                    l.elementSelected(e);

                setCurrentCursor(Cursor.getDefaultCursor());

                
                if (passThru) {
                    if (lastSelectedElement.selectedForEdit(this))  //tell the element it was selected
                        passThruElement = lastSelectedElement; 
                }


                
                repaint();
                return true;
            }
        }
        return false;
     
    }
    
    /**
     * Find the next element after toFind, if toFind is at end, loop back to first. 
     * @param toFind find next element after this.  If this is null, return the first.  If not found, return the first
     * @return 
     */
    private ZElement getNext(ZElement toFind) {  
        
        if (toFind == null)
            return fields.zElements.getFirst();
        
        Iterator<ZElement> it = fields.zElements.iterator();
        while (it.hasNext()) {
            ZElement e = it.next();
            if (e.equals(toFind)) {
                if (it.hasNext())
                    return it.next();
                else
                    return fields.zElements.getFirst();
            } 
        }
        return fields.zElements.getFirst();
    }
    
    /**
     * Selects the next selectable ZElement after the last selected element. If nothing is currently selected, selects the first 
     * selectable ZElement.  If nothing is selectable, does nothing
     * If there is an element already selected for pass through events, does nothing.
     */
    public void selectNextElement() {
        
        if (passThruElement != null)
            return;
                    
        ZElement first = lastSelectedElement;
        ZElement next = first;
        
        do {
            next = getNext(next);
            if (next.isSelectable()) {
                selectNone();
                selectElement(next, false);
                repaint();
                return;
            }
            
        } while (next != first);
        

    }
    
    /**
     * Select all elements.
     */
    public void selectAll() {
        
        for (ZElement e : fields.zElements) {
            if (e.isSelectable()) {
                e.select();
                lastSelectedElement = e;
                for (ZCanvasEventListener l : canvasEventListeners)
                    l.elementSelected(e);   
            }
        }
        
        repaint();
    }
    
    /**
     * Remove selection for all elements
     */
    public void selectNone() {

        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (!selectedElements.isEmpty()) {
            for (ZElement e : selectedElements) {
                e.deselectedForEdit();
                e.deselect();
            }
            passThruElement = null;
        }
        selectedResizeElement = null;
        lastSelectedElement = null;
        for (ZCanvasEventListener l : canvasEventListeners)
            l.elementSelected(null);

        shearXPressed = false;
        shearYPressed = false;
        shiftPressed = false;
        altPressed = false;
        
        setCurrentCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        repaint();     

    }
    
    
    private void paintString(Graphics2D g2d, String s, double x, double y) {
        g2d.drawString(s, (int)Math.round(x), (int)Math.round(y));

    }
    
    @Override
    public synchronized void paintComponent(Graphics g) {

        super.paintComponent(g);  
        Graphics2D g2d = (Graphics2D)g;
        
        DecimalFormat degreeFormat = new DecimalFormat("0.00\u00b0");
          
          
        //Paint any rulers before scaling and translations
        if (fields.horizontalRuler != null) {
            g2d.translate(fields.origin.x, 0); 
            fields.horizontalRuler.paint(g2d, (int)(SCALE*fields.zoom), getScaledWidth(), getScaledHeight());    
            g2d.translate(-fields.origin.x, 0);         
        }
        if (fields.verticalRuler != null) {
            g2d.translate(0, fields.origin.y); 
            fields.verticalRuler.paint(g2d, (int)(SCALE*fields.zoom), getScaledWidth(), getScaledHeight());    
            g2d.translate(0, -fields.origin.y);         
        }

        g2d.translate(fields.origin.x, fields.origin.y);
        g2d.scale(1/pixScale, 1/pixScale);
        g2d.scale(fields.zoom, fields.zoom);
               
        if (fields.backgroundColor != null) {
            g2d.setBackground(fields.backgroundColor);
            if (fields.pageSize != null)
                g2d.clearRect(0, 0, (int)(fields.pageSize.width * pixScale), (int)(fields.pageSize.height * pixScale));
            else
                g2d.clearRect(0, 0, getScaledWidth(), getScaledHeight());
        }
        
        if (!printOn && fields.grid != null)
            paintElement(g2d, fields.grid, false);
        
        if (fields.margins != null && fields.marginsOn && !printOn) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(0.5f));
            g2d.draw(fields.margins);
        }
        
        ArrayList<ZElement> selectedElements = new ArrayList<>();  //for speed - so we don't need to iterate twice
        //Start from the deepest point in the stack, drawing elements up to the top z layer
        Iterator<ZElement> it = fields.zElements.descendingIterator();  
        while (it.hasNext()) {
            ZElement o = it.next();
            if (o.isSelected())
                selectedElements.add(o);
            paintElement(g2d, o, false); 
        }
     
        for (ZElement s : selectedElements)
            paintElement(g2d, s, true); //apply highlights to selected elements
        
        Font mouseFont = new Font(fields.mouseCoordFont.getFontName(), fields.mouseCoordFont.getStyle(), (int)Math.ceil(fields.mouseCoordFont.getSize2D()*pixScale/fields.zoom));
        FontMetrics fontMetrics = g2d.getFontMetrics(mouseFont);
        g2d.setFont(mouseFont);
   
        //ELEMENT IS BEING DRAGGED - DRAW SELECTED HIGHLIGHT AND POSITION LINES/TEXT
        if (selectedMouseDrag != null && !selectedElements.isEmpty() && lastSelectedElement != null) {
                        
            
            AffineTransform t = lastSelectedElement.getElementTransform(SCALE, false);
            Point2D tMouse = t.transform(selectedMouseDrag, null);   
            
            //Draw crosshair
            if (fields.mouseCursorColor != null) {
                g2d.setColor(fields.mouseCursorColor);
                g2d.setStroke(new BasicStroke(1.0f * pixScale / (float)fields.zoom, CAP_SQUARE, JOIN_MITER, 10.0f, selectedAlternateBorder ? dashedBorder : altDashedBorder, 0.0f));
                g2d.drawLine(-fields.origin.x, (int)(tMouse.getY()), (int)(tMouse.getX()), (int)(tMouse.getY())); //horiz crosshair
                g2d.drawLine((int)(tMouse.getX()), -fields.origin.y, (int)(tMouse.getX()), (int)(tMouse.getY())); //vert crosshair
            }
            
            //Draw Position string
            if (fields.mouseCoordFont != null) {
                g2d.setColor(Color.BLACK);               

                String mouseCoord = fields.unit.format(tMouse.getX()/SCALE, true) + ", " + fields.unit.format(tMouse.getY()/SCALE, true);                                       
                int stringX = (int)tMouse.getX() - (int)Math.ceil(fontMetrics.stringWidth(mouseCoord) + 10.0 * pixScale/fields.zoom);
                int stringY = (int)tMouse.getY() - (int)Math.ceil(10*pixScale/fields.zoom);
                
                g2d.drawString(mouseCoord, stringX, stringY);
                               
                String rotationString = degreeFormat.format(lastSelectedElement.getRotation());
                stringX = (int)tMouse.getX() - (int)Math.ceil(fontMetrics.stringWidth(rotationString) + 10.0 * pixScale/fields.zoom);
                stringY = (int)tMouse.getY() + (int)Math.ceil(10*pixScale/fields.zoom) + fontMetrics.getHeight();

                g2d.drawString(rotationString, stringX, stringY);
            }

        }
        
        //Draw Resize String
        if (selectedElementResizeOn && mouseIn != null && lastSelectedElement != null && fields.mouseCoordFont != null) {
            g2d.setColor(Color.BLACK);
            Rectangle2D bounds = lastSelectedElement.getBounds2D();
            String mouseCoord = fields.unit.format(bounds.getWidth(), false) + " x " + fields.unit.format(bounds.getHeight(), true);
            
            paintString(g2d, mouseCoord, mouseIn.getX() + DRAG_BOX_SIZE*2*pixScale/(float)fields.zoom, mouseIn.getY() + DRAG_BOX_SIZE*2*pixScale/(float)fields.zoom);
            
        }
        
        
        //When nothing selected, draw the mouse
        if (!printOn && mouseIn != null && selectedElements.isEmpty() && mouseIn.getX() >= 0 && mouseIn.getY() >= 0) {  
                        
            //Draw crosshair
            if (fields.mouseCursorColor != null) {
                g2d.setColor(fields.mouseCursorColor);
                g2d.setStroke(new BasicStroke(1.0f * pixScale / (float)fields.zoom));
                g2d.draw(new Line2D.Double(-fields.origin.x, mouseIn.getY(), getMaxWidth(), mouseIn.getY())); //horiz crosshair
                g2d.draw(new Line2D.Double(mouseIn.getX(), -fields.origin.y, mouseIn.getX(), getMaxHeight())); //vert crosshair
            }

            Rectangle2D dragRect = getDragSelectRectangle();

            if (mouseDrag != null) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f * pixScale / (float)fields.zoom));
                g2d.draw(dragRect);
            }
            
            //Draw mouse position or drag box size
            if (fields.mouseCoordFont != null) {
                g2d.setColor(Color.BLACK);
                String s;
                String measString = null;
                if (mouseDrag == null)
                    s = fields.unit.format(mouseIn.getX()/SCALE, true) + ", " + fields.unit.format(mouseIn.getY()/SCALE, true);
                else {
                    double area = (dragRect.getWidth()/SCALE) * (dragRect.getHeight()/SCALE);  //area is w*h in units
                    s = fields.unit.format(dragRect.getWidth()/SCALE, false) + " x " + fields.unit.format(dragRect.getHeight()/SCALE, true);
                    measString = fields.unit.formatArea(area, true);
                }
                double x = mouseIn.getX() + (int)Math.ceil(10.0 * pixScale/fields.zoom);
                double y = mouseIn.getY() + (int)Math.ceil(fontMetrics.getHeight() + 10.0 * pixScale/fields.zoom);
                paintString(g2d, s, x, y);
                if (measString != null)
                    paintString(g2d, measString, x, y + (int)Math.ceil(fontMetrics.getHeight() + 10.0 * pixScale/fields.zoom));
            }
  
            
        }
            
        //Paint anything the client is drawing
        if (drawClient != null) {
            drawClient.drawClientPaint(g, mouseIn);
        }
        
        for (ZCanvasEventListener l : canvasEventListeners)
            l.canvasRepainted();
        
        
    }

 
    public void editElement() {
        
        if (lastSelectedElement.supportsEdit()) {
        
            if (lastSelectedElement.selectedForEdit(this))  //tell the element it was selected
                passThruElement = lastSelectedElement;  

            undoStack.saveContext(fields.zElements);

            lastMethod = null; 
        }
        
         
        for (ZCanvasEventListener l : canvasEventListeners)
            l.elementEdited(lastSelectedElement, lastSelectedElement.supportsEdit());
        
        selectedElementResizeOn = false;    

    }
    
    
    //Determine the selected object, if any, from the mouse pick. 
    private void selectElement(MouseEvent e) {
        
        Point2D mouseLoc = getScaledMouse(e);
        
        //Select the pointed object, if there is one
        //See if the mouse click was within the bounds of any component, checking upper objects before moving down the z stack
        Iterator<ZElement> it = fields.zElements.iterator();
        while (it.hasNext()) {
            ZElement o = it.next();
            if (!o.isSelectable()) //don't select anything that's unselectable
                continue;
                                   
            if (o.isSelected() && altPressed)  //ignore selected objects when alt pressed
                continue;
            
            Rectangle2D boundsBox = o.getBounds2D(SCALE);

            Point2D lowerRightCorner = new Point2D.Double(boundsBox.getX() + boundsBox.getWidth(), boundsBox.getY() + boundsBox.getHeight());
            AffineTransform t = o.getElementTransform(SCALE, false);
            Shape s = t.createTransformedShape(boundsBox);
            Point2D lowerRightTransformed = t.transform(lowerRightCorner, null);  //also transform the lower right corner
      
            if (s.contains(mouseLoc)) {  //see if the mouse point is in the shape
                
                if (!o.isSelected()) {  //newly selected element
                    
                    if (!shiftPressed && hasSelectedElements()) //no shift, so clear all others
                        selectNone();  
                        
                    o.select();
                    repaint();
                        
                } else {  //element was already selected
                    
                    if (shiftPressed) {  //deselect this
                        o.deselect();
                        
                        if (passThruElement == o)
                            passThruElement = null;
        
                        selectedResizeElement = null;
                        lastSelectedElement = null;
                        return;
                    }
                    
                    
                }
                
                lastSelectedElement = o;
                
                if (contextMenu != null)
                    contextMenu.newSelections(lastSelectedElement, getSelectedElements());
               
                for (ZCanvasEventListener l : canvasEventListeners)
                    l.elementSelected(o);
       
                Point2D location = o.getPosition(SCALE);  //get the upper left, find the mouse offset from the upper left
                
                selectedObj_mousePoint = mouseLoc;
                
                selectedObj_xOffset = mouseLoc.getX() - location.getX();
                selectedObj_yOffset = mouseLoc.getY() - location.getY();
                
                selectedObj_xOffset_toRightCorner = lowerRightTransformed.getX() - mouseLoc.getX();
                selectedObj_yOffset_toRightCorner = lowerRightTransformed.getY() - mouseLoc.getY();
                
                //Check if the mouse was within the drag box
                if (o.isResizable() && lowerRightTransformed.distance(mouseLoc)/pixScale < DRAG_BOX_SIZE) { 
                    selectedElementResizeOn = true;
                    selectedResizeElement = o;
                    selectedResizeElementOrigDim = selectedResizeElement.getBounds2D(SCALE);
                }
                else {
                    selectedElementResizeOn = false;         
                    selectedResizeElement = null;
                }

                return;
                
            }
            

        }
        selectNone();
    }
    
    
    private boolean passThroughMouse(MouseEvent e) {
        
        if (passThruElement == null)
            return false;
                
        AffineTransform elementTransform = passThruElement.getElementTransform(SCALE, true);
        Point2D transformedMouse = elementTransform.transform(getScaledMouse(e), null);
        Point2D position = passThruElement.getPosition(SCALE);

        double xOffset = transformedMouse.getX() - position.getX();
        double yOffset = transformedMouse.getY() - position.getY();
        
        MouseEvent m = new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), (int)Math.round(xOffset), (int)Math.round(yOffset), e.getClickCount(), false, e.getButton());
        boolean swallowed = passThruElement.mouseEvent(this, m);  //tell the element about the mouse                  
        repaint();
        return swallowed;
    }
    
    
    @Override
    public void mouseClicked(MouseEvent e) {
                        
        if (drawClient != null) {
            drawClient.drawClientMouseClicked(getScaledMouse(e), e);
            repaint();
            return;
        }
        
        if (passThruElement != null) {
            passThroughMouse(e);
            return;
        }

                         
        if (e.getClickCount() > 1 && SwingUtilities.isLeftMouseButton(e) && lastSelectedElement != null) {  //Transfer control to the selected element
            editElement();
        }
       
        selectedElementResizeOn = false;    
                    
    }

    
    public void setCurrentCursor(Cursor c) {
        currentCursor = c;
        changeCursor(currentCursor);
    }
    
    
    private void changeCursor(Cursor cursor) {
        Component c = this;
        while (c != null) { //keep setting all the parents, until we get to the panel
          c.setCursor(cursor);   
          c = c.getParent();
        }
    }
    
    private Point2D getScaledMouse(MouseEvent e) {
        double x = e.getPoint().x;
        double y = e.getPoint().y;

        x *= (pixScale / fields.zoom);
        y *= (pixScale / fields.zoom);
        
        x -= (double)fields.origin.x / fields.zoom;
        y -= (double)fields.origin.y / fields.zoom;
        
        return new Point2D.Double(x, y);
    }
    
    public int getScaledHeight() {
        return (int)(this.getHeight() * pixScale);
    }
    
    public int getScaledWidth() {
        return (int)(this.getWidth() * pixScale);
    }
    
    private int getMaxWidth() {
        return (int)(getScaledWidth() / fields.zoom);
    }
    
    private int getMaxHeight() {
        return (int)(getScaledHeight() / fields.zoom);
    }
     
    private int getMaxXPosition() {
        return (int)((getScaledWidth() - fields.origin.x - 10)/ fields.zoom);
    }
    private int getMaxYPosition() {
        return (int)((getScaledHeight() - fields.origin.y - 10)/ fields.zoom);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        
        this.requestFocusInWindow();
        
        if (mouseFirstPressed < 0)
            mouseFirstPressed = System.nanoTime();
        
        
        Point2D mouseLoc = getScaledMouse(e);
        mousePress = mouseLoc;
        
        if (drawClient != null) {
            drawClient.drawClientMousePressed(mouseLoc, e);
            repaint();
            return;
        }
               
        if (passThruElement != null) { 
            if (passThroughMouse(e))  //if swallowed, don't further process the mouse event
                return;
        } 

        selectElement(e);  //check to select an object

        if (hasSelectedElements()) {
            
            setCurrentCursor(Cursor.getDefaultCursor());

            if (SwingUtilities.isRightMouseButton(e) && contextMenu != null) { 
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
                return;
            }
            
            if (!selectedElementResizeOn) 
                selectedMousePress = new Point2D.Double((mouseLoc.getX() - selectedObj_xOffset), (mouseLoc.getY() - selectedObj_yOffset));  


            undoStack.saveContext(fields.zElements);


        } else 
            setCurrentCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        
        if (drawClient != null) {
            drawClient.drawClientMouseReleased(getScaledMouse(e), e);
            repaint();
            return;
        }
        
        if (passThruElement != null) { 
            passThroughMouse(e);
            return;
        }
        
        //If drag-selecting, select all elements falling in the bounds
        if (mousePress != null && mouseDrag != null) {
            
            Rectangle2D dragSelect = getDragSelectRectangle();
            
            Iterator<ZElement> it = fields.zElements.iterator();
            while (it.hasNext()) {
                ZElement o = it.next();
                if (!o.isSelectable()) //don't select anything that's unselectable
                    continue;
                
                Rectangle2D boundsBox = o.getBounds2D(SCALE);
                AffineTransform t = o.getElementTransform(SCALE, false);
                Shape s = t.createTransformedShape(boundsBox);
                
                if (dragSelect.contains(s.getBounds()))
                    selectElement(o, false);
            }
            
            
        }
        
        selectedMouseDrag = null;
        selectedMousePress = null;
        mousePress = null;
        mouseDrag = null;
        mouseFirstPressed = -1;
        selectedElementResizeOn = false;
        
        repaint();
    }

    @Override
    public synchronized void mouseEntered(MouseEvent e) {
        changeCursor(currentCursor);
        mouseIn = getScaledMouse(e);
        repaint();
    }


    @Override
    public synchronized void mouseExited(MouseEvent e) {
        changeCursor(Cursor.getDefaultCursor());
        mouseIn = null;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
                 
        Point2D mouseLoc = getScaledMouse(e);
        mouseIn = mouseLoc;

        selectedMouseDrag = null;
        selectedMousePress = null;
 
        if (drawClient != null) {
            drawClient.drawClientMouseDragged(getScaledMouse(e), e);
            repaint();
            return;
        }
        
        
        if (passThruElement != null) { 
            passThroughMouse(e);
            return;
        }
        
        //If element selected and mouse is within the canvas
        if (lastSelectedElement != null && mouseLoc.getX() < getMaxWidth() && mouseLoc.getY() < getMaxHeight())  {      
                
            
            if (!selectedElementResizeOn) { //Reposition the object to the mouse, only when it won't take the object off the component
                
                if (lastSelectedElement.isMoveable()) {
                    lastSelectedElement.reposition((mouseLoc.getX() - selectedObj_xOffset)/SCALE, (mouseLoc.getY() - selectedObj_yOffset)/SCALE, getMaxXPosition()/SCALE, getMaxYPosition()/SCALE);  
                    selectedMouseDrag = new Point2D.Double((mouseLoc.getX() - selectedObj_xOffset), (mouseLoc.getY() - selectedObj_yOffset));  
                }
            } else if (selectedResizeElement != null) { //Resize the object
                    
                
                //move mouse and originally selected point to base coordinates
                AffineTransform t = selectedResizeElement.getElementTransform(1, true);  //pix scale
                Point2D mouseT = t.transform(mouseLoc, null);
                Point2D selectT = t.transform(selectedObj_mousePoint, null);

                //calculate difference
                double xDiff = mouseT.getX() - selectT.getX();
                double yDiff = mouseT.getY() - selectT.getY();
                  
                //apply difference to find new width/height, and resize
                double newWidth = selectedResizeElementOrigDim.getWidth() + xDiff;
                double newHeight = selectedResizeElementOrigDim.getHeight() + yDiff;
                selectedResizeElement.changeSize(newWidth, newHeight, DRAG_BOX_SIZE, SCALE);

                //Get the transformed position of the lower right coordinate
                t = selectedResizeElement.getElementTransform(SCALE, false);
                Rectangle2D bounds = selectedResizeElement.getBounds2D(SCALE);
                Point2D lowerRightT = t.transform(new Point2D.Double(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight()), null);
                
                //Find the amount to move in order to keep the drag box co-located with the mouse point
                double xMove = mouseLoc.getX() - lowerRightT.getX() + selectedObj_xOffset_toRightCorner;
                double yMove = mouseLoc.getY() - lowerRightT.getY() + selectedObj_yOffset_toRightCorner;

                //move the shape 
                selectedResizeElement.move(xMove/SCALE, yMove/SCALE, getMaxXPosition()/SCALE, getMaxYPosition()/SCALE);

            }
                        
        }
        else {  //nothing selected
            mouseDrag = mouseLoc;

        }
        repaint();  //update selected object
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseIn = getScaledMouse(e);
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        
        if (drawClient != null)
            return;
        
        
        if (passThruElement != null) { 
            passThroughMouse(e);
            return;
        } 
            
        ArrayList<ZElement> selectedElements = getSelectedElements();
    
        if (!selectedElements.isEmpty()) {
            if (System.nanoTime() - mouseWheelLastMoved > 1000000000) {
                undoStack.saveContext(fields.zElements);
            }
            
            for (ZElement selectedElement : selectedElements) {
                if (!shiftPressed && !shearXPressed && !shearYPressed) {
                    double increase = e.getPreciseWheelRotation() * SIZE_INCREASE_MULTIPLIER;
                    selectedElement.increaseSizeMaintainAspect(increase, DRAG_BOX_SIZE, SCALE);
                }
                else if (shiftPressed && !shearXPressed && !shearYPressed) {       
                    selectedElement.rotate(e.getPreciseWheelRotation() * ROTATION_MULTIPLIER);
                }
                else if (shearXPressed) {
                    selectedElement.shearX(e.getPreciseWheelRotation() * SHEAR_MULTIPLIER);
                }
                else if (shearYPressed) {
                    selectedElement.shearY(e.getPreciseWheelRotation() * SHEAR_MULTIPLIER);
                }
            }
            
            mouseWheelLastMoved = System.nanoTime();
            repaint();
        }

    }

    /**
     * For all elements on the canvas, checks to see if there are unsaved changes
     * @return true if there are unsaved changes, false otherwise
     */
    public boolean hasUnsavedChanges() {
                
        if (canvasModified)
            return true;
        
        for (ZElement e : fields.zElements)  //mark all has having been saved
            if (e.hasChanges())
                return true;
        
        return false;
    }
   
    
    
    /**
     * Retrieves ContextClasses object for storing a ZCanvas using a JAXB context.  Includes required classes plus all element classes for elements
     * that have been added to the canvas, and for any ZGroupedElements, the classes that it contains
     * @return 
     */
    public ContextClasses getContextClasses() {   
        return ContextClasses.getContextClasses(fields.zElements);
    }
    
    
    
    /**
     * Retrieves an object that can be used to store the ZCanvas, in a custom format (if JAXB is not desired). The returned object is
     * serializable and also marshallable with JAXB
     * @return 
     */
    public CanvasStore getCanvasStore() {
        return this.fields;
    }
    
    /**
     * Creates a new ZCanvas from the provided canvas store
     * @param s the store to use
     * @return a restored ZCanvas
     */
    public static ZCanvas fromCanvasStore(CanvasStore s) {

        ZCanvas c = new ZCanvas();
        c.fields = s;
        
        Iterator<ZElement> it = c.fields.zElements.iterator();
        while (it.hasNext()) {
            it.next().addedTo(c);
        }
        
        c.setGrid(c.fields.grid);
        
        c.canvasModified = false;
        c.init();
        return c;
    }
    
    /**
     * Mark the canvas and its elements has having been saed
     */
    public void markAsSaved() {
        for (ZElement e : fields.zElements)  //mark all has having been saved
            e.wasSaved();
        
        canvasModified = false;
    }
    
    
   
    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {   
        if (pageIndex > 0) 
            return(NO_SUCH_PAGE);
      
        drawOff();
        resetView();
        selectNone();  //prevents drawing any selections or client draw
        
        printOn = true;  //prevents drawing of grid, margins, and mouse cursor/information

        RepaintManager currentManager = RepaintManager.currentManager(this);
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(-fields.origin.x, -fields.origin.y);
        currentManager.setDoubleBufferingEnabled(false);
        this.paint(g2d);
        currentManager.setDoubleBufferingEnabled(true);
        
        printOn = false;
        
        return(PAGE_EXISTS);
      
    }
    
    /**
     * Print the canvas to a supplied Graphics2D context, the margins, canvas rulers, and grid are hidden
     * @param g the context to draw on
     */
    public void paintToGraphicsContext(Graphics2D g) {
        
        //Hide margins, ruler, and grid
        boolean marginsOn = areMarginsOn();
        marginsOn(false);
        ZGrid savedGrid = fields.grid; //save grid
        ZCanvasRuler horizontalRuler = fields.horizontalRuler;
        ZCanvasRuler verticalRuler = fields.verticalRuler;
        
        setGrid(null);
        setHorizontalRuler(null);
        setVerticalRuler(null);
        
        
        drawOff();
        resetView();
        selectNone();  //prevents drawing any selections or client draw
        
        printOn = true;  //prevents drawing of grid, margins, and mouse cursor/information

        RepaintManager currentManager = RepaintManager.currentManager(this);
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(-fields.origin.x, -fields.origin.y);
        currentManager.setDoubleBufferingEnabled(false);
        this.paint(g2d);
        currentManager.setDoubleBufferingEnabled(true);
        
        printOn = false;
        
        
        //Restore margins, ruler and grid
        marginsOn(marginsOn);
        setGrid(savedGrid);
        setHorizontalRuler(horizontalRuler);
        setVerticalRuler(verticalRuler);
                
    }
    
    /**
     * Grab an image of the canvas, the margins and grid are hidden
     * @param resolutionScale the desired resolution multiplier. A value of 1 = 72dpi (screen resolution).  2 doubles this, and so on.
     * @return the image of the canvas
     */
    public BufferedImage printToImage(int resolutionScale) {
        
        if (fields.pageSize == null || resolutionScale < 1)
            return null;
        
        //Hide margins and grid
        boolean marginsOn = areMarginsOn();
        marginsOn(false);
        ZGrid savedGrid = fields.grid; //save grid
        setGrid(null);
        
        //Create Buffered Image
        BufferedImage bi = new BufferedImage(fields.pageSize.width*resolutionScale, fields.pageSize.height*resolutionScale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.scale(resolutionScale, resolutionScale);
        print(g, null, 0);
        g.dispose();
        
        //Restore margins and grid
        marginsOn(marginsOn);
        setGrid(savedGrid);
        
        
        return bi;   
    }

    
    /**
     * Print all selected elements by first creating a grouped element from them, and printing that element to an image
     * @param resolutionScale the desired resolution multiplier. A value of 1 = 72dpi (screen resolution).  2 doubles this, and so on.
     * @return the image of the selected elements
     */
    public BufferedImage printSelectedElementsToImage(int resolutionScale) {
        
        if (resolutionScale < 1)
            return null;
        
        ArrayList<ZElement> selectedElements = getSelectedElements();

        if (selectedElements.isEmpty()) 
            return null;
        
        Collections.reverse(selectedElements); //the selected elements are ordered with top z plane first.  But the Grouped Element draws grouped elements in the order provided, so we need to reverse the list
        ZGroupedElement group = ZGroupedElement.createGroup(selectedElements);  //create the group of elements
        
        group.reposition(0, 0, Double.MAX_VALUE, Double.MAX_VALUE); //no offset (not on canvas, painting to image
        
        //Find the shape that holds the element with its margins
        Rectangle2D margins = group.getMarginBounds(SCALE*fields.zoom); 
        AffineTransform t = group.getElementTransform(SCALE*fields.zoom, false);
        Shape s = t.createTransformedShape(margins);
      
        Rectangle2D bounds = s.getBounds2D(); //make bounds something that can hold the transformed shape
                
        int imgWidth = (int)Math.ceil(bounds.getWidth() - bounds.getX());
        int imgHeight = (int)Math.ceil(bounds.getHeight() - bounds.getY());
        
        //Create Buffered Image
        BufferedImage bi = new BufferedImage(imgWidth*resolutionScale, imgHeight*resolutionScale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(resolutionScale, resolutionScale);
        
        //Set the position to paint such that the margins will end up at the top left of the image
        g.translate(-bounds.getX() - margins.getX(), -bounds.getY() - margins.getY());
        g.scale(1/pixScale, 1/pixScale);
        g.scale(fields.zoom, fields.zoom);
        
        RepaintManager currentManager = RepaintManager.currentManager(this);

        currentManager.setDoubleBufferingEnabled(false);
        this.paintElement(g, group, false);
        currentManager.setDoubleBufferingEnabled(true);
      
        g.dispose();
        return bi; 
    }

    
}
