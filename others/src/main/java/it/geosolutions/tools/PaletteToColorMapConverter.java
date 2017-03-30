package it.geosolutions.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.io.ParseException;

public class PaletteToColorMapConverter extends JFrame implements
        DropTargetListener, ActionListener {

/** The LOGGER for this class. */
private static final Logger LOGGER = Logger
        .getLogger(PaletteToColorMapConverter.class.toString());

public static void main(String[] args) {
    new PaletteToColorMapConverter();
}

final static String HEADER = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
        + "<StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
        + "  xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
        + "  xsi:schemaLocation=\"http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd\">\n"
        + "  <NamedLayer>\n" + "    <Name></Name>\n" + "    <UserStyle>\n"
        + "      <Name></Name>\n" + "      <Title>ColorMap</Title>\n"
        + "      <Abstract>Quantities color map</Abstract>\n"
        + "      <FeatureTypeStyle>\n" + "        <Rule>\n"
        + "          <RasterSymbolizer>\n"
        + "            <Opacity>1.0</Opacity>\n"
        + "            <ColorMap type=\"values\">\n";

final static String FOOTER = "            </ColorMap>\n"
        + "          </RasterSymbolizer>\n" + "        </Rule>\n"
        + "      </FeatureTypeStyle>\n" + "    </UserStyle>\n"
        + "  </NamedLayer>\n" + "</StyledLayerDescriptor>\n";

DropTarget dt;

JTextArea textAreaDragAndDrop;

ArrayList<String> files = new ArrayList<String>();

public PaletteToColorMapConverter() {
    super("Palette To ColorMap Converter");
    setSize(300, 300);

    WindowListener wl = new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    };
    addWindowListener(wl);

    JPanel dragPanel = new JPanel(new GridLayout(2, 0));
    JLabel footprint = new JLabel("drag gdalinfo output here");
    textAreaDragAndDrop = new JTextArea();
    textAreaDragAndDrop.setBackground(new Color(200, 200, 200));
    textAreaDragAndDrop.setEditable(false);

    dragPanel.add(footprint, BorderLayout.NORTH);
    dragPanel.add(textAreaDragAndDrop, BorderLayout.SOUTH);
    getContentPane().add(dragPanel, BorderLayout.CENTER);

    JPanel producePanel = new JPanel();

    JButton b1 = new JButton("Start Conversion");
    b1.addActionListener(this);
    b1.setMnemonic(KeyEvent.VK_C);
    b1.setActionCommand("convert");

    JButton bclean = new JButton("Clean");
    bclean.addActionListener(this);
    bclean.setMnemonic(KeyEvent.VK_N);
    bclean.setActionCommand("clean");

    producePanel.add(b1, BorderLayout.SOUTH);
    producePanel.add(bclean, BorderLayout.SOUTH);

    getContentPane().add(producePanel, BorderLayout.AFTER_LAST_LINE);

    dt = new DropTarget(textAreaDragAndDrop, this);
    setVisible(true);
}

public void actionPerformed(ActionEvent e) {
    if ("convert".equals(e.getActionCommand())) {
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to process");

            return;
        }
        String outputFile = null;
        try {
            outputFile = convert();
            if (outputFile != null) {
                StringSelection ss = new StringSelection(outputFile);
                Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(ss, null);
                JOptionPane
                    .showMessageDialog(
                            this,
                            "Done\nStored as "
                                    + outputFile
                                    + "\n\n The resulting path has been copied into the clipboard",
                            "Conversion", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
        } catch (ParseException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
        } catch (NoSuchAuthorityCodeException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
        } catch (FactoryException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
        }

    } else if ("clean".equals(e.getActionCommand())) {

        clean();
    }
}

private void clean() {
    textAreaDragAndDrop.setText("");
    files.clear();

}

private String convert() throws IOException, ParseException,
        NoSuchAuthorityCodeException, FactoryException {
    final String filePath = files.get(0);
    final String filePathSld = filePath + ".sld";
    FileImageInputStream fis = null;
    FileImageOutputStream fos = null;
    int numValues = 256;
    try {
        fos = new FileImageOutputStream(new File(filePathSld));
        fis = new FileImageInputStream(new File(filePath));

        String line = null;
        String index = null;
        boolean hasColorTable = false;
        while ((line = fis.readLine()) != null) {
            if (!line.contains("Color Table"))
                continue;
            else {
                if (line.contains("entries")) {
                    int with = line.indexOf("with");
                    int entries = line.indexOf("entries");
                    String numEntries = line.substring(with + 5, entries)
                            .trim();
                    numValues = Integer.parseInt(numEntries);
                }
                hasColorTable = true;
                break;
            }

        }
        
        if (!hasColorTable)
        {
            JOptionPane
            .showMessageDialog(
                    this,
                    "The provided gdalinfo doesn't contain any colorTable",
                    "Conversion", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        boolean init = false;
        int length = 0;
        boolean hasAlpha = false;
        double opacity = 1.0;
        fos.writeBytes(HEADER);
        while ((line = fis.readLine()) != null) {
            int colon = line.indexOf(":");
            index = line.substring(0, colon).trim();

            String colorComps = line.substring(colon + 1, line.length());
            String[] colors = colorComps.split(",");
            if (!init) {
                init = true;
                length = colors.length;
                if (length == 4) {
                    hasAlpha = true;
                }
            }

            String[] hexStrings = new String[3];
            for (int i = 0; i < 3; i++) {
                hexStrings[i] = Integer.toHexString(Integer.parseInt(colors[i]
                        .trim().toUpperCase()));
                if (hexStrings[i].length() == 1) {
                    hexStrings[i] = "0" + hexStrings[i];
                }
            }
            if (hasAlpha) {
                float alpha = Float.parseFloat(colors[3]);
                opacity = alpha / 255f;
            }

            String map = "              <ColorMapEntry color=\"#"
                    + hexStrings[0] + hexStrings[1] + hexStrings[2]
                    + "\" quantity=\"" + index + "\" "
                    + (hasAlpha ? (" opacity=\"" + opacity + "\" ") : "")
                    + "/>\n";
            fos.writeBytes(map);
        }

        final int finalIndex = Integer.parseInt(index);
        if (finalIndex < numValues) {
            for (int i = finalIndex + 1; i < numValues; i++) {
                String map = "              <ColorMapEntry color=\"#000000\" quantity=\""
                        + i + "\" />\n";
                fos.writeBytes(map);
            }
        }
        fos.writeBytes(FOOTER);
    } finally {

        if (fis != null) {
            try {
                fis.close();
            } catch (Throwable t) {

            }

        }
        if (fos != null) {
            try {
                fos.close();
            } catch (Throwable t) {

            }
        }
    }
    return filePathSld;

}

public void dragEnter(DropTargetDragEvent dtde) {
}

public void dragExit(DropTargetEvent dte) {
}

public void dragOver(DropTargetDragEvent dtde) {
}

public void dropActionChanged(DropTargetDragEvent dtde) {
}

public void drop(DropTargetDropEvent dtde) {
    try {
        // Ok, get the dropped object and try to figure out what it is
        Transferable tr = dtde.getTransferable();
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            System.out.println("Possible flavor: " + flavors[i].getMimeType());
            // Check for file lists specifically
            if (flavors[i].isFlavorJavaFileListType()) {
                // Great! Accept copy drops...
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                // And add the list of file names to our text area
                java.util.List list = (java.util.List) tr
                        .getTransferData(flavors[i]);
                for (int j = 0; j < list.size(); j++) {
                    Object element = list.get(j);
                    String file = element.toString();
                    textAreaDragAndDrop.append(file + "\n");
                    files.add(file);
                }

                // If we made it this far, everything worked.
                dtde.dropComplete(true);

                return;
            }
            // Ok, is it another Java object?
            else if (flavors[i].isFlavorSerializedObjectType()) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                textAreaDragAndDrop.setText("Successful text drop.\n\n");

                Object o = tr.getTransferData(flavors[i]);
                textAreaDragAndDrop.append("Object: " + o);
                dtde.dropComplete(true);

                return;
            }
            // How about an input stream?
            else if (flavors[i].isRepresentationClassInputStream()) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                textAreaDragAndDrop.setText("Successful text drop.\n\n");
                textAreaDragAndDrop.read(
                        new InputStreamReader((InputStream) tr
                                .getTransferData(flavors[i])),
                        "from system clipboard");
                dtde.dropComplete(true);

                return;
            }
        }
        // Hmm, the user must not have dropped a file list
        System.out.println("Drop failed: " + dtde);
        dtde.rejectDrop();
    } catch (Exception e) {
        e.printStackTrace();
        dtde.rejectDrop();
    }
}

}
