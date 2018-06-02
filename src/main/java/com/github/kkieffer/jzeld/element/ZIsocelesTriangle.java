
package com.github.kkieffer.jzeld.element;

import java.awt.Color;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author kkieffer
 */
@XmlRootElement(name = "ZIsocelesTriangle")
@XmlAccessorType(XmlAccessType.FIELD)
public class ZIsocelesTriangle extends ZAbstractTriangle {

        
    protected ZIsocelesTriangle() {
        super();
    }
    
    public ZIsocelesTriangle(double x, double y, double width, double height, double rotation, boolean canSelect, boolean canResize, float borderWidth, Color borderColor, Float[] dashPattern, Color fillColor) {
        super(Type.ISOCELES, x, y, width, height, rotation, canSelect, canResize, borderWidth, borderColor, dashPattern, fillColor);
    }
    
    protected ZIsocelesTriangle(ZIsocelesTriangle copy) {
        super(copy);    
    }
    
    @Override
    public ZIsocelesTriangle copyOf() {
        return new ZIsocelesTriangle(this);
    }
    
}