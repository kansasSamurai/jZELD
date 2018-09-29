
package com.github.kkieffer.jzeld.contextMenu;

import com.github.kkieffer.jzeld.ZCanvas;
import com.github.kkieffer.jzeld.ZCanvas.CombineOperation;
import static com.github.kkieffer.jzeld.ZCanvas.errorIcon;
import com.github.kkieffer.jzeld.element.ZElement;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.UIManager;

/**
 * An implementation of the ZCanvasContextMenu.  
 * 
 * @author kkieffer
 */
public class ZDefaultContextMenu implements ZCanvasContextMenu {
    
    protected JMenu rotateMenu;
    protected JMenuItem rotateCWMenuItem;
    protected JMenuItem rotateCCWMenuItem;
    protected JMenu arrangeMenu;
    protected JMenuItem sendToBackMenuItem;
    protected JMenuItem bringToFrontMenuItem;
    protected JMenuItem sendBackwardsMenuItem;
    protected JMenuItem bringForwardsMenuItem;
    protected JMenuItem copyMenuItem;
    protected JMenuItem pasteMenuItem;
    protected JMenuItem deleteMenuItem;
    protected JMenu editMenu;
    protected JMenuItem flipVertMenuItem;
    protected JMenuItem flipHorizMenuItem;
    protected JMenu attributesMenu;
    protected LineBorderMenu lineWeightMenu;
    protected LineBorderMenu lineDashMenu;
    protected LineStyleMenu lineStyleMenu;
    protected JMenu colorMenu;
    protected ColorMenuItem fillColorMenuItem;
    protected ColorMenuItem lineColorMenuItem;
    protected JMenu alignMenu;
    protected JPopupMenu contextPopupMenu;
    protected final JMenu combineMenu;
    protected final JMenuItem resetVerticalShearMenuItem;
    protected final JMenuItem resetHorizontalShearMenuItem;
    protected final JMenu shearMenu;
    protected final ColorMenuItem removeFillMenuItem;
    
    
    public ZDefaultContextMenu(ZCanvas c) {
        
        Font f = new Font("sans-serif", Font.PLAIN, 14);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        
        contextPopupMenu = new JPopupMenu();   
         
        editMenu = new JMenu("Edit");
        copyMenuItem = new JMenuItem("Copy");
        pasteMenuItem = new JMenuItem("Paste");
        deleteMenuItem = new JMenuItem("Delete");
        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);
        editMenu.add(deleteMenuItem);
   
        rotateMenu = new JMenu("Rotate");
        rotateCWMenuItem = new JMenuItem("Snap Clockwise");
        rotateCCWMenuItem = new JMenuItem("Snap Counter CW");
        rotateMenu.add(rotateCWMenuItem);
        rotateMenu.add(rotateCCWMenuItem);
        
        shearMenu = new JMenu("Shear");
        resetHorizontalShearMenuItem = new JMenuItem("Reset Horizontal Shear");
        resetVerticalShearMenuItem = new JMenuItem("Reset Vertical Shear");
        shearMenu.add(resetHorizontalShearMenuItem);
        shearMenu.add(resetVerticalShearMenuItem);
        
        arrangeMenu = new JMenu("Arrange");
        sendToBackMenuItem = new JMenuItem("Send to Back");
        sendBackwardsMenuItem = new JMenuItem("Send Backward");
        bringToFrontMenuItem = new JMenuItem("Bring to Front");
        bringForwardsMenuItem = new JMenuItem("Bring Forward");
        flipHorizMenuItem = new JMenuItem("Flip Horizontal");
        flipVertMenuItem = new JMenuItem("Flip Vertical");
        
        arrangeMenu.add(sendToBackMenuItem);
        arrangeMenu.add(sendBackwardsMenuItem);
        arrangeMenu.add(bringToFrontMenuItem);
        arrangeMenu.add(bringForwardsMenuItem);
        arrangeMenu.addSeparator();
        arrangeMenu.add(flipHorizMenuItem);
        arrangeMenu.add(flipVertMenuItem);
        
        alignMenu = new JMenu("Align");
        for (ZCanvas.Alignment a : ZCanvas.Alignment.values()) {
            JMenuItem m = new JMenuItem(a.toString());
            m.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                   c.align(a);
                }
            });
            alignMenu.add(m);
        }
        
        
        combineMenu = new JMenu("Combine");
        for (CombineOperation g : CombineOperation.values()) {
            JMenuItem m = new JMenuItem(g.toString());
            m.setName(g.name());
            m.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    int numSel = c.getSelectedElementsArray().length;
                    int combined = c.combineSelectedElements(g);
                
                    if (numSel != combined)
                        JOptionPane.showMessageDialog(c, "Only " + combined + " element" + (combined != 1 ? "s" : "") + " combined.\nOther selected elements are not shapes and cannot be combined. ", "Warning", JOptionPane.ERROR_MESSAGE, errorIcon);

                
                }
            });
            combineMenu.add(m);
            
        }        
        
                
        attributesMenu = new JMenu("Attributes");
        lineWeightMenu = new LineBorderMenu("Line Weight", c, LineBorderMenu.Type.WEIGHT);
        lineDashMenu = new LineBorderMenu("Dash Pattern", c, LineBorderMenu.Type.DASH);
        lineStyleMenu = new LineStyleMenu("Line Style", c);
        colorMenu = new JMenu("Color");
        lineColorMenuItem = new ColorMenuItem("Line Color", c, ColorMenuItem.Type.LINE);
        colorMenu.add(lineColorMenuItem);
        fillColorMenuItem = new ColorMenuItem("Fill Color", c, ColorMenuItem.Type.FILL);
        colorMenu.add(fillColorMenuItem);

        removeFillMenuItem = new ColorMenuItem("Remove Fill Color", c, ColorMenuItem.Type.CLEAR);
        colorMenu.add(removeFillMenuItem);
        
        attributesMenu.add(lineWeightMenu);
        attributesMenu.add(lineStyleMenu);
        attributesMenu.add(lineDashMenu);
        attributesMenu.add(colorMenu);
        
        contextPopupMenu.add(editMenu);
        contextPopupMenu.add(rotateMenu);
        contextPopupMenu.add(shearMenu);
        contextPopupMenu.add(arrangeMenu);
        contextPopupMenu.add(attributesMenu);
        contextPopupMenu.add(new JSeparator());
        contextPopupMenu.add(combineMenu);
        contextPopupMenu.add(alignMenu);
        
        copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               c.copy();
            }
        });
        pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               c.paste();
            }
        });
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               c.delete();
            }
        });
        
        rotateCWMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.rotate90CW();
            }
        });
        
        rotateCCWMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.rotate90CCW();
            }
        });  
        
        sendToBackMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.moveToBack();
            }
        }); 
        
        sendBackwardsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.moveBackward();
            }
        }); 
        
        bringToFrontMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.moveToFront();
            }
        }); 
        
        bringForwardsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.moveForward();
            }
        }); 
        
        flipHorizMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.flip(true);
            }
        }); 
        
        flipVertMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.flip(false);
            }
        }); 
        
        resetHorizontalShearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.resetShear(true);
            }
        });
        resetVerticalShearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.resetShear(false);
            }
        });
        removeFillMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                c.removeFill();
            }
        });
        
        
                
    }

    @Override
    public void show(Component component, int x, int y) {
        contextPopupMenu.show(component, x, y);
    }

    @Override
    public void newSelections(ZElement lastSelected, ArrayList<ZElement> selectedElements) {
        
        if (lastSelected == null)
            return;
            
        
        if (lastSelected.supportsFlip()) {
            flipHorizMenuItem.setEnabled(true);
            flipVertMenuItem.setEnabled(true);
        } else {
            flipHorizMenuItem.setEnabled(false);
            flipVertMenuItem.setEnabled(false);
        }
        lineWeightMenu.setEnabled(lastSelected.hasOutline());
        lineColorMenuItem.setEnabled(lastSelected.hasOutline());
        lineDashMenu.setEnabled(lastSelected.hasDash());
        fillColorMenuItem.setEnabled(lastSelected.hasFill());
         
        alignMenu.setEnabled(selectedElements.size() > 1);
        combineMenu.setEnabled(selectedElements.size() > 1);
        


    }
    
}
