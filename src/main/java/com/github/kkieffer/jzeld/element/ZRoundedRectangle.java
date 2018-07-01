
package com.github.kkieffer.jzeld.element;

import com.github.kkieffer.jzeld.ZCanvas;
import static com.github.kkieffer.jzeld.ZCanvas.errorIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A ZRoundedRectangle is a ZRectangle with rounded corners
 * 
 * @author kkieffer
 */
@XmlRootElement(name = "ZRoundedRectangle")
@XmlAccessorType(XmlAccessType.FIELD)
public class ZRoundedRectangle extends ZRectangle {

    public static final ImageIcon radiusIcon = new ImageIcon(ZCanvas.class.getResource("/radius.png")); 

    
    protected double radius;

    protected ZRoundedRectangle(){}
    
     /**
     * Create a ZRoundedRectangle
     * @param x the x coordinate, upper left x, in units
     * @param y the y coordinate, upper left y, in units
     * @param width the width of the object in units, or -1 for unlimited width
     * @param height the height of the object in units, or -1 for unlimited height
     * @param rotation desired rotation of the component in degrees, clockwise
     * @param canSelect if the object can be selected by the ZCanvas mouse click
     * @param canResize if the object can be resized by the mouse drag
     * @param borderWidth unit width of the border, use zero for no border
     * @param borderColor color of the border, which can be null only if the borderWidth is zero
     * @param dashPattern the border dash pattern, null for solid
     * @param fillColor color of the rectangle area, which can be null for transparent (but not in combination with a zero width border)
     * @param radius the radius of the rounded edges, in units
     */
    public ZRoundedRectangle(double x, double y, double width, double height, double rotation, boolean canSelect, boolean canResize, boolean canMove, float borderWidth, Color borderColor, Float[] dashPattern, Color fillColor, double radius) {
        super(x, y, width, height, rotation, canSelect, canResize, canMove, borderWidth, borderColor, dashPattern, fillColor);
        this.radius = radius;
    }
    
    
    public ZRoundedRectangle(ZRoundedRectangle copy) {
        super(copy);
        this.radius = copy.radius;
    }
    
    @Override
    public ZRoundedRectangle copyOf() {
        return new ZRoundedRectangle(this);
    }
    
    /**
     * Adjust the radius
     * @param r the value in units
     */
    public void setRadius(double r) {
        radius = r;
        hasChanges = true;
    }
     
    
    @Override
    public boolean selected(ZCanvas canvas) {
        
        
        
        String rc = (String)JOptionPane.showInputDialog(canvas, "Update Corner Radius", "Modify Rounded Rectangle", JOptionPane.QUESTION_MESSAGE, radiusIcon,
                                                (Object[])null, (Object)String.valueOf(radius));
        if (rc != null) {
            try {
                 setRadius(Double.parseDouble(rc));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(canvas, "Invalid value: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, errorIcon);
            }
        }
        
        return false;
    }
    
     
    @Override
    protected void fillShape(Graphics2D g, int unitSize, int width, int height) {
        g.fillRoundRect(0, 0, width, height, (int)(radius*unitSize*2), (int)(radius*unitSize*2));
    }
    
    @Override
    protected void drawShape(Graphics2D g, int unitSize, int width, int height) {
        g.drawRoundRect(0, 0, width, height, (int)(radius*unitSize*2), (int)(radius*unitSize*2));
    }
    
   

    
}
