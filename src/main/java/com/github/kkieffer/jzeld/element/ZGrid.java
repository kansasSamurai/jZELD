
package com.github.kkieffer.jzeld.element;

import com.github.kkieffer.jzeld.UnitMeasure;
import com.github.kkieffer.jzeld.ZCanvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A ZGrid paints a grid across the canvas. The grid attributes support a thickness, color, and dash pattern.
 * Units of inches or cm are supported (drawn on every inch or cm)
 * 
 * @author kkieffer
 */
@XmlRootElement(name = "ZGrid")
@XmlAccessorType(XmlAccessType.FIELD)
public final class ZGrid extends ZLine {

    private UnitMeasure unit;
    private int majorTickStep;
    private float outlineWidth = 0.5f;
    
    private transient ZCanvas canvas;
    
    protected ZGrid() {}
    
    /**
     * Create a layout grid on the unit boundaries
     * @param thickness the thickness of the gridlines
     * @param color the color of the gridlines
     * @param dashPattern the pattern for dashed lines (see BasicStroke), or null for solid line
     * @param unit the grid unit size
     * @param majorTickStep the steps between major unit markings
    */
    public ZGrid(float thickness, Color color, Float[] dashPattern, UnitMeasure unit, int majorTickStep) {
        super(0, 0, -1, 0.0, false, true, false, thickness, color, dashPattern, StrokeStyle.SQUARE);
        this.unit = unit;
        this.majorTickStep = majorTickStep;
        this.outlineWidth = thickness;
        setSize(-1, -1, -1, 1);
    }
    
    @Override
    public ZGrid copyOf(boolean forNew) {
        return this;
    }
    

    
    @Override
    public void paint(Graphics2D g, double unitSize, double width, double height) {
        setOutlineWidth(outlineWidth/(float)canvas.getZoomFactor());
        super.paint(g, unitSize, width, height);
    }
    
    @Override
    protected void drawShape(Graphics2D g, double unitSize, double width, double height) {
        
        double scale = majorTickStep * (double)unitSize / unit.getScale();
        
        //Paint vertical gridlines
        for (double i=0; i<width; i+=scale) {
            int inc = (int)Math.round(i);
            g.draw(new Line2D.Double(inc, 0, inc, height)); 
        }

        //Paint horizontal gridlines   
        for (double j=0; j<height; j+=scale) {
            int jnc = (int)Math.round(j);
            g.draw(new Line2D.Double(0, jnc, width, jnc)); 
        }
    }

    @Override
    public void addedTo(ZCanvas c) {
        canvas = c;
    }
    
    @Override
    public void removedFrom(ZCanvas c) {
        canvas = null;
    }
    
   
}
