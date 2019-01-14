package com.github.kkieffer.jzeld.element;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A ZPolygon is a abstract closed polygon
 * 
 * @author kkieffer
 */
@XmlRootElement(name = "ZPolygon")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class ZPolygon extends ZAbstractShape {
    
    transient private Shape shape;
    
    protected ZPolygon(){}
    

    public ZPolygon(ZPolygon copy, boolean forNew) {
        super(copy, forNew);
    }
    
    public ZPolygon(double x, double y, double width, double height, double rotation, boolean canSelect, boolean canResize, boolean canMove, float borderWidth, Color borderColor, Float[] dashPattern, Color fillColor, StrokeStyle borderStyle) {
        super(x, y, width, height, rotation, canSelect, canResize, canMove, borderWidth, borderColor, dashPattern, fillColor, borderStyle);
    }
    
    @Override
    public boolean supportsFlip() {
        return true;
    }
    
    @Override
    protected void fillShape(Graphics2D g, double unitSize, double width, double height) {
        g.fill(shape);
    }
    
    @Override
    protected void drawShape(Graphics2D g, double unitSize, double width, double height) {
        g.draw(shape);
    }
    
    
    protected abstract Shape getPolygon(double width, double height, double scale);
    
    private Shape getTransformedPolygon(double width, double height, double scale) {
        
        Shape polygon = getPolygon(width, height, scale);
        
        if (flipHoriz || flipVert) {
            AffineTransform scaleInstance = AffineTransform.getScaleInstance(flipHoriz ? -1.0 : 1.0, flipVert ? -1.0 : 1.0);  //scaling negative creates a mirror image the other direction
            polygon = scaleInstance.createTransformedShape(polygon);
            AffineTransform translateInstance = AffineTransform.getTranslateInstance(flipHoriz ? width : 0, flipVert ? height : 0);  //move back to where it was
            polygon = translateInstance.createTransformedShape(polygon);
        }
        return polygon;
    }
    
    
    @Override
    protected Shape getAbstractShape() {
        Rectangle2D r = getBounds2D();
        return getTransformedPolygon(r.getWidth(), r.getHeight(), 1.0);
    }

    
    @Override
    public void paint(Graphics2D g, double unitSize, double width, double height) {
 
       shape = getTransformedPolygon(width, height, unitSize);
  
       super.paint(g, unitSize, width, height);
    }

    
    
   

    
}
