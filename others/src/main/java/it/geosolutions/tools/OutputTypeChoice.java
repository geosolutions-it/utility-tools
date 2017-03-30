package it.geosolutions.tools;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class OutputTypeChoice extends JPanel
                          implements ItemListener {
    JCheckBox wktButton;
    JCheckBox wkbButton;
    JCheckBox gmlButton;
    JCheckBox gmlTextButton;
    JCheckBox shapeFileButton;
    JCheckBox simplifierButton;
 
   protected Set<FootprintConverter.GeometryType> choices = new HashSet<FootprintConverter.GeometryType>();
 
    public OutputTypeChoice() {
        super(new BorderLayout());
 
        JLabel outputType = new JLabel("Select output type");
                
        //Create the check boxes.
        wktButton = new JCheckBox("Wkt");
        wktButton.setMnemonic(KeyEvent.VK_W);
        wktButton.setSelected(false);
 
        wkbButton = new JCheckBox("Wkb");
        wkbButton.setMnemonic(KeyEvent.VK_B);
        wkbButton.setSelected(false);

        gmlButton = new JCheckBox("GML");
        gmlButton.setMnemonic(KeyEvent.VK_G);
        gmlButton.setSelected(false);

        gmlTextButton = new JCheckBox("GMLText");
        gmlTextButton.setMnemonic(KeyEvent.VK_T);
        gmlTextButton.setSelected(false);

        shapeFileButton = new JCheckBox("Shapefile");
        shapeFileButton.setMnemonic(KeyEvent.VK_S);
        shapeFileButton.setSelected(false);
 
        simplifierButton = new JCheckBox("simplifier");
        simplifierButton.setMnemonic(KeyEvent.VK_F);
        simplifierButton.setSelected(false);
 
        //Register a listener for the check boxes.
        wktButton.addItemListener(this);
        wkbButton.addItemListener(this);
        gmlButton.addItemListener(this);
        gmlTextButton.addItemListener(this);
        shapeFileButton.addItemListener(this);
        simplifierButton.addItemListener(this);
 
        //Indicates what's on the geek.
//        choices = new StringBuffer("wbs");
 
        //Put the check boxes in a column in a panel
        JPanel checkPanel = new JPanel(new GridLayout(0, 1));
        checkPanel.add(outputType);
        checkPanel.add(wktButton);
        checkPanel.add(wkbButton);
        checkPanel.add(gmlButton);
        checkPanel.add(gmlTextButton);
        checkPanel.add(shapeFileButton);
        checkPanel.add(simplifierButton);
         
        add(checkPanel, BorderLayout.LINE_START);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
    }
 
    /** Listens to the check boxes. */
    public void itemStateChanged(ItemEvent e) {
        int index = 0;
        FootprintConverter.GeometryType type = FootprintConverter.GeometryType.NONE;
        Object source = e.getItemSelectable();
 
        if (source == wktButton) {
            index = 0;
            type = FootprintConverter.GeometryType.WKT;
        } else if (source == wkbButton) {
            index = 1;
            type = FootprintConverter.GeometryType.WKB;
        } else if (source == shapeFileButton) {
            index = 2;
            type = FootprintConverter.GeometryType.SHAPEFILE;
        } else if (source == simplifierButton) {
            index = 3;
            type = FootprintConverter.GeometryType.SIMPLIFIED;
        } else if (source == gmlButton) {
            index = 4;
            type = FootprintConverter.GeometryType.GML;
        } else if (source == gmlTextButton) {
            index = 5;
            type = FootprintConverter.GeometryType.GML_TEXT;
        }
        
        //Now that we know which button was pushed, find out
        //whether it was selected or deselected.
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            choices.remove(type);
        } else {
            choices.add(type);
        }
    }
 
    
    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = OutputTypeChoice.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
 
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("CheckBoxDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the content pane.
        JComponent newContentPane = new OutputTypeChoice();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
 
    public void clean(){
        choices.clear();
        wktButton.setSelected(false);
        wkbButton.setSelected(false);
        shapeFileButton.setSelected(false);
        simplifierButton.setSelected(false);
        gmlButton.setSelected(false);
        gmlTextButton.setSelected(false);
    }
}