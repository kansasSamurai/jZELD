
package com.github.kkieffer.jzeld.contextMenu;

import com.github.kkieffer.jzeld.ZCanvas;
import com.github.kkieffer.jzeld.element.ZElement.StrokeStyle;
import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JMenu;

/**
 * LineStyleMenu class provides a JMenu with line styles. When an menu item is selected
 * it calls the method to set the element line style in the canvas on whatever the selected element is.
 * 
 * @author kkieffer
 */
public class LineStyleMenu extends JMenu {

   
    
    private static class StyleMenuItem extends AbstractContextMenu {
        
        
        public StyleMenuItem(ZCanvas c, StrokeStyle style) {
            
            super(c);
            
            createMenuGraphics();
       
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(8.0f, style.getCapType(), style.getJoinType()));
            int strW = g.getFontMetrics().stringWidth(style.toString()) + 10;
            g.drawString(style.toString(), 0, 12);
            g.drawLine(strW, 8, strW + 25, 8);
            g.dispose();

            this.setIcon(new ImageIcon(bufferedImage));
                        
            
            addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    if (canvas == null)
                        return;
                    canvas.setOutlineStyle(style);
                    lineStyleChanged(style);
                }
            });
        }
        
    }
    
   
    
   
    
    private final ArrayList<AbstractContextMenu> menuItemList = new ArrayList<>();
    
  
    public LineStyleMenu(String text, ZCanvas c) {
        super(text);

        for (StrokeStyle s : StrokeStyle.values()) {
               StyleMenuItem weightMenuItem = new StyleMenuItem(c, s);
               menuItemList.add(weightMenuItem);
               this.add(weightMenuItem);   
        }
            
    }
    
     public void addListener(ContextMenuListener l) {
        for (AbstractContextMenu m : menuItemList)
            m.addListener(l);
    }
    
    public void removeListener(ContextMenuListener l) {
        for (AbstractContextMenu m : menuItemList)
            m.removeListener(l);
    }
    
    public void setCanvas(ZCanvas c) {
        for (AbstractContextMenu m : menuItemList)
            m.setCanvas(c);
    }
    
    
    
}
