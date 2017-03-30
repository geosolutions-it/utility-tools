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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class SqlResultsToShapeFileConverter extends JFrame implements
        DropTargetListener, ActionListener {

/** The LOGGER for this class. */
private static final Logger LOGGER = Logger
        .getLogger(SqlResultsToShapeFileConverter.class.toString());

public static void main(String[] args) {
    new SqlResultsToShapeFileConverter();
}

DropTarget dt;

JTextArea textAreaDragAndDrop;

ArrayList<String> files = new ArrayList<String>();

public SqlResultsToShapeFileConverter() {
    super("SQL export to Shapefile Converter");
    setSize(300, 300);

    WindowListener wl = new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    };
    addWindowListener(wl);

    JPanel dragPanel = new JPanel(new GridLayout(2, 0));
    JLabel footprint = new JLabel("drag SQL results here");
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
    final File file = new File(filePath);
    final String fileName = file.getName();
    FileImageInputStream fis = null;
    String globalFile = null;
    try {
        fis = new FileImageInputStream(file);

        String line = "";
        final CoordinateReferenceSystem writeCrs = CRS.decode("EPSG:4326");
        ListFeatureCollection outGeodata = null;
        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
      
        String folder = "c:\\tmp\\" + fileName;
        final File folderFile = new File(folder);
        if (!folderFile.exists())
        {
            folderFile.mkdir();
        }

        globalFile = folder + "\\global.shp";
        SimpleFeatureType type = null;
        SimpleFeatureBuilder builder = null;
        boolean begin = true;
        int geomIndex = 0;
        int nFields = 0;
        int k=0;
        while ((line = fis.readLine()) != null)
        {
            if (begin)
            {
                String entries[] = line.split(";");
                nFields = entries.length;
                b.setName("extractedFeatures");
                b.setCRS(writeCrs);
                b.add("the_geom", Polygon.class);
                for (int i=0; i<nFields;i++)
                {
                    String field = entries[i].replace("\"" ,"");
                    if (field.equalsIgnoreCase("asewkt")){
                        geomIndex = i; 
                    } else {
                        b.add(field, String.class);        
                    }
                }
                type = b.buildFeatureType();
                builder = new SimpleFeatureBuilder(type);
                outGeodata = new ListFeatureCollection(type);
                begin = false;
                continue;
            }

            int start = 0;
            int end = 0;
            Object[] objects = new Object[nFields];
            line = line.replace("SRID=4326;POLYGON", "POLYGON");
            for (int i=0; i<nFields;i++)
            {
                start = end + (end == 0 ? 0:1);
                end = line.indexOf(";",start+1);
                String entry = line.substring(start,end!=-1? end:line.length()).replace("\"", "");
                objects[i] = entry;
                
            }

            WKTReader reader = new WKTReader(new GeometryFactory());
            String wkt = (String)objects[geomIndex];
            Geometry geometry = reader.read(wkt);
            

            // add features
            final Object[] values = new Object[nFields];
            values[0] = geometry;
            for (int i=1; i<nFields; i++) {
                    
                    values[i] = objects[i- (i<=geomIndex? 1 :0)];    
            }
            builder.addAll(values);

            final SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + k++);
            outGeodata.add(feature);
            builder.reset();
            // create shapefile
        }
        writeShape(globalFile, writeCrs, outGeodata, type);
    } finally {
        if (fis != null) {
            try {
                fis.close();
            } catch (Throwable t) {
                
            }
        }
    }
        return globalFile;
    }

    public static final void writeShape(final String filePath,
        final CoordinateReferenceSystem crs, SimpleFeatureCollection data,
        SimpleFeatureType type) throws IOException
    {

        final File file = new File(filePath);

        // Creating the schema
        final DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

        final Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", DataUtilities.fileToURL(file));
        params.put("create spatial index", Boolean.TRUE);

        final ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(type);
        if (crs != null)
        {
            newDataStore.forceSchemaCRS(crs);
        }

        // Write the features to the shapefile
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        FeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof FeatureStore)
        {
            FeatureStore featureStore = (FeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try
            {
                featureStore.addFeatures(data);
                transaction.commit();

            }
            catch (Exception problem)
            {
                transaction.rollback();

            }
            finally
            {
                transaction.close();
            }
        }

        try
        {
            if (newDataStore != null)
            {
                newDataStore.dispose();
            }
        }
        catch (Throwable e)
        {

        }

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
