package it.geosolutions.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.xml.parsers.ParserConfigurationException;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.InputStreamInStream;
import com.vividsolutions.jts.io.OutputStreamOutStream;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.gml2.GMLReader;
import com.vividsolutions.jts.io.gml2.GMLWriter;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;




public class FootprintConverter extends JFrame implements DropTargetListener, ActionListener
{

    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger.getLogger(FootprintConverter.class.toString());

    public static void main(String[] args)
    {
        new FootprintConverter();
    }

    public static enum GeometryType
    {
        WKT,
        WKB,
        SHAPEFILE,
        NONE,
        GML,
        GML_TEXT,
        SIMPLIFIED
    }

    DropTarget dt;

    JTextArea textAreaDragAndDrop;

    JTextArea epsgCode;

    JTextArea gmlwkt;

    JTextArea simplifier;

    JTextArea targetEpsgCode;

    OutputTypeChoice checkBoxPanel;

    ArrayList<String> files = new ArrayList<String>();

    WKBReader wkbReader = new WKBReader();
    WKBWriter wkbWriter = new WKBWriter();
    WKTReader wktReader = new WKTReader();
    WKTWriter wktwriter = new WKTWriter();
    GMLReader gmlReader = new GMLReader();
    GMLWriter gmlWriter = new GMLWriter();

    public FootprintConverter()
    {
        super("Footprint Converter");
        setSize(500, 500);

        WindowListener wl = new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    System.exit(0);
                }
            };
        addWindowListener(wl);

        JPanel epsgPanel = new JPanel(new GridLayout(0, 2));
        JLabel epsgLabel = new JLabel("Select source EPSG code:");
        JLabel gmlwktLabel = new JLabel("GML or WKT");
        JLabel simplifierLabel = new JLabel("Simplifier");
        JLabel targetEpsgLabel = new JLabel("Select target EPSG code:");
        epsgCode = new JTextArea();
        epsgCode.setBackground(Color.white);
        epsgCode.setText("4326");
        epsgPanel.add(epsgLabel, BorderLayout.WEST);
        epsgPanel.add(epsgCode, BorderLayout.EAST);

        targetEpsgCode = new JTextArea();
        targetEpsgCode.setBackground(Color.white);
        targetEpsgCode.setText("");
        epsgPanel.add(targetEpsgLabel, BorderLayout.WEST);
        epsgPanel.add(targetEpsgCode, BorderLayout.EAST);
        
        gmlwkt = new JTextArea();
        gmlwkt.setBackground(Color.white);
        epsgPanel.add(gmlwktLabel, BorderLayout.WEST);
        epsgPanel.add(gmlwkt, BorderLayout.EAST);

        simplifier = new JTextArea();
        simplifier.setBackground(Color.white);
        epsgPanel.add(simplifierLabel, BorderLayout.WEST);
        epsgPanel.add(simplifier, BorderLayout.EAST);

        JButton b2 = new JButton(
                "GMLcoords to WKT Polygon / WKT Polygon to GML coords");
        b2.addActionListener(this);
        b2.setMnemonic(KeyEvent.VK_K);
        b2.setActionCommand("convert");
        epsgPanel.add(b2, BorderLayout.AFTER_LAST_LINE);

        getContentPane().add(epsgPanel, BorderLayout.NORTH);

        JPanel dragPanel = new JPanel(new GridLayout(2, 0));
        JLabel footprint = new JLabel("drag footprint input here");
        textAreaDragAndDrop = new JTextArea();
        textAreaDragAndDrop.setBackground(new Color(200, 200, 200));
        textAreaDragAndDrop.setEditable(false);

        dragPanel.add(footprint, BorderLayout.NORTH);
        dragPanel.add(textAreaDragAndDrop, BorderLayout.SOUTH);
        getContentPane().add(dragPanel, BorderLayout.CENTER);

        JPanel producePanel = new JPanel();
        checkBoxPanel = new OutputTypeChoice();
        checkBoxPanel.setOpaque(true); // content panes must be opaque
        producePanel.add(checkBoxPanel, BorderLayout.LINE_START);

        JButton b1 = new JButton("Start Conversion");
        b1.addActionListener(this);
        b1.setMnemonic(KeyEvent.VK_C);
        b1.setActionCommand("process");

        JButton bclean = new JButton("Reset");
        bclean.addActionListener(this);
        bclean.setMnemonic(KeyEvent.VK_R);
        bclean.setActionCommand("clean");

        JButton bcleanList = new JButton("Clean List");
        bcleanList .addActionListener(this);
        bcleanList .setMnemonic(KeyEvent.VK_N);
        bcleanList .setActionCommand("cleanList");
        
        producePanel.add(b1, BorderLayout.SOUTH);
        producePanel.add(bclean, BorderLayout.SOUTH);
        producePanel.add(bcleanList, BorderLayout.SOUTH);

        getContentPane().add(producePanel, BorderLayout.AFTER_LAST_LINE);

        dt = new DropTarget(textAreaDragAndDrop, this);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if ("process".equals(e.getActionCommand())) {
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nothing to process");

                return;
            }

            if (checkBoxPanel.choices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Need to specify an output type");

                return;
            }

            try {
                process();
            } catch (IOException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (ParseException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (NoSuchAuthorityCodeException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (FactoryException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (MismatchedDimensionException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (TransformException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (SAXException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            } catch (ParserConfigurationException e1) {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            }
        }
        else if ("convert".equals(e.getActionCommand()))
        {
            if (gmlwkt.getText().isEmpty())
            {
                JOptionPane.showMessageDialog(this,
                    "no gml/nor wkt polygon specified");

                return;
            }

            try
            {
                convert();
            }
            catch (IOException e1)
            {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            }
            catch (ParseException e1)
            {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            }
            catch (NoSuchAuthorityCodeException e1)
            {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            }
            catch (FactoryException e1)
            {
                LOGGER.log(Level.FINE, e1.getMessage(), e1);
            }
        }
        else if ("clean".equals(e.getActionCommand()))
        {
            clean();
        } else if ("cleanList".equals(e.getActionCommand()))
        {
            cleanList();
        }

    }

    private void clean()
    {
        checkBoxPanel.clean();
        targetEpsgCode.setText("");
        simplifier.setText("");
        gmlwkt.setText("");
        cleanList();
    }
    
    private void cleanList()
    {
        textAreaDragAndDrop.setText("");
        files.clear();
    }

    private void process() throws IOException, ParseException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException, SAXException, ParserConfigurationException
    {
        boolean transformed = false;
        for (String file : files)
        {
            GeometryWrapper wrapper = extractGeometry(file);
            transformed = transformCRS(wrapper);
            if (checkBoxPanel.choices.contains(FootprintConverter.GeometryType.SIMPLIFIED) &&
                    (wrapper.type != GeometryType.SIMPLIFIED))
            {
                simplify(wrapper, file);
            }
            
            if (checkBoxPanel.choices.contains(FootprintConverter.GeometryType.WKT) &&
                    (transformed || wrapper.type != GeometryType.WKT || checkBoxPanel.choices.contains(FootprintConverter.GeometryType.SIMPLIFIED)))
            {
                convertWKT(wrapper, file);
            }
            if (checkBoxPanel.choices.contains(FootprintConverter.GeometryType.WKB) &&
                    (transformed || wrapper.type != GeometryType.WKB))
            {
                convertWKB(wrapper, file);
            }
            if (checkBoxPanel.choices.contains(FootprintConverter.GeometryType.GML) &&
                    (transformed || wrapper.type != GeometryType.GML))
            {
                convertGML(wrapper, file, false);
            }
            if (checkBoxPanel.choices.contains(FootprintConverter.GeometryType.GML_TEXT) &&
                    (transformed || wrapper.type != GeometryType.GML_TEXT))
            {
                convertGML(wrapper, file, true);
            }
            if (checkBoxPanel.choices.contains(FootprintConverter.GeometryType.SHAPEFILE) &&
                    (transformed || wrapper.type != GeometryType.SHAPEFILE))
            {
                String epsgText = transformed ? targetEpsgCode.getText() : epsgCode.getText();
                if ((epsgText == null) || epsgText.isEmpty())
                {
                    JOptionPane.showMessageDialog(
                        this,
                        "Need to specify a proper epsg code when converting to shapefile",
                        "EPSG", JOptionPane.ERROR_MESSAGE);

                    return;
                }
                convertShapefile(wrapper, file, epsgText);
            }
        }
        JOptionPane.showMessageDialog(this, "Done", "Conversion",
            JOptionPane.INFORMATION_MESSAGE);

    }

    private boolean transformCRS(GeometryWrapper wrapper) throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {
        Geometry geometry = wrapper.geometry;
        String epsg = targetEpsgCode.getText();
        if (!epsg.isEmpty()) {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + epsgCode.getText(), true);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:" + epsg, true);
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            wrapper.geometry = JTS.transform(geometry, transform);
            return true;
        }
        return false;
    }

    private void simplify(GeometryWrapper wrapper, String file) {
        Geometry geometry = wrapper.geometry;
        String simplifierFactor = simplifier.getText();
        if (!simplifierFactor.isEmpty()) {
            Double value = Double.parseDouble(simplifierFactor);
            wrapper.geometry = DouglasPeuckerSimplifier.simplify(geometry, value);
        }
        
    }

    private void convert() throws IOException, ParseException, NoSuchAuthorityCodeException, FactoryException
    {
        String gmlCoords = gmlwkt.getText();
        final int commaIndex = gmlCoords.indexOf(",");
        final int spaceIndex = gmlCoords.indexOf(" ");
        boolean gmlToWkt = false;
        if (commaIndex < spaceIndex)
        {
            gmlToWkt = true;
        }

        if (gmlToWkt)
        {
            String wktCoords = null;
            String wkt2 = "POLYGON((";
            wktCoords = gmlCoords.replace(",", "_");
            wktCoords = wktCoords.replace(" ", ",");
            wktCoords = wktCoords.replace("_", " ");
            wkt2 = wkt2 + wktCoords + "))";

            final File tempFile = File.createTempFile("openJump", ".wkt");
            FileImageOutputStream fios = new FileImageOutputStream(tempFile);
            fios.write(wkt2.getBytes());
            fios.close();
            gmlwkt.setText(tempFile.getAbsolutePath());
        }
        else
        {
            String gmlCoords2 = gmlCoords.replace(", ", "_");
            gmlCoords2 = gmlCoords2.replace(" ", ",");
            gmlCoords2 = gmlCoords2.replace("_", " ");
            gmlwkt.setText(gmlCoords2);
        }

    }

    private void convertWKB(GeometryWrapper geometry, String file) throws IOException
    {
        final int suffixIndex = file.lastIndexOf(".");
        final String prefix = file.substring(0, suffixIndex);
        final String newName = prefix + "_converted" + ".wkb";
        FileOutputStream fos = new FileOutputStream(new File(newName));
        OutputStreamOutStream os = new OutputStreamOutStream(fos);
        wkbWriter.write(geometry.geometry, os);
        fos.close();
    }

    private void convertGML(GeometryWrapper geometry, String file, boolean asText) throws IOException
    {
        final int suffixIndex = file.lastIndexOf(".");
        final String prefix = file.substring(0, suffixIndex);
        final String suffix = asText ? "_text" : "_converted";
        final String newName = prefix + suffix + ".gml";
        Writer writer = asText ? new StringWriter() : new FileWriter(newName);
        gmlWriter.write(geometry.geometry, writer);
        writer.flush();
        writer.close();
        if (asText) {
            StringWriter sWriter = (StringWriter) writer;
            String gml = sWriter.toString();
            gml = gml.replace("<", "&lt;").replace(">", "&gt;");
            FileWriter fileWriter = new FileWriter(newName);
            fileWriter.write(gml);
            fileWriter.flush();
            fileWriter.close();
        }
    }

    private GeometryWrapper extractGeometry(String file) throws IOException, ParseException, SAXException, ParserConfigurationException
    {
        FileInputStream fis = new FileInputStream(new File(file));
        InputStreamInStream is = new InputStreamInStream(fis);
        Geometry geom = null;
        GeometryType type = null;

        if (file.toLowerCase().endsWith("wkt"))
        {
            InputStreamReader isReader = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isReader);
            String string = null;
            StringBuilder sb = new StringBuilder();
            while ((string = reader.readLine()) != null)
            {
                sb.append(string);
            }
            type = GeometryType.WKT;
            geom = wktReader.read(sb.toString());
            reader.close();
            isReader.close();
        }
        else if (file.toLowerCase().endsWith("wkb"))
        {
            geom = wkbReader.read(is);
            type = GeometryType.WKB;
        }
        else if (file.toLowerCase().endsWith("shp"))
        {
            ShpFiles files = new ShpFiles(new File(file));
            ShapefileReader reader = new ShapefileReader(files, false, true, new GeometryFactory());
            geom = (Geometry) reader.nextRecord().shape();
            reader.close();
            type = GeometryType.SHAPEFILE;
        }
        else if (file.toLowerCase().endsWith("gml"))
        {
            InputStreamReader isReader = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isReader);
            String string = null;
            StringBuilder sb = new StringBuilder();
            while ((string = reader.readLine()) != null)
            {
                sb.append(string);
            }
            String gmlText = sb.toString();
            if (gmlText.contains("&gt;") && gmlText.contains("&lt;")) {
                type = GeometryType.GML_TEXT;
                gmlText = gmlText.replace("&gt;", ">").replace("&lt;","<");
            } else {
                type = GeometryType.GML;
            }
            geom = gmlReader.read(gmlText, new GeometryFactory());
            reader.close();
            isReader.close();
            
        }
        fis.close();
        return new GeometryWrapper(geom, type);
    }

    private void convertShapefile(GeometryWrapper wrapper, String file, String epsgText) throws IOException,
        NoSuchAuthorityCodeException, FactoryException
    {
        final int suffixIndex = file.lastIndexOf(".");
        final String prefix = file.substring(0, suffixIndex);
        final String newShpName = prefix + "_converted" + ".shp";
        SimpleFeatureCollection outGeodata = null;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:" + epsgText, true);

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("raster2vector");

        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("cat", Integer.class);

        SimpleFeatureType type = b.buildFeatureType();
        outGeodata = new MemoryFeatureCollection(type); 

        // add features
        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        final Object[] values = new Object[] { wrapper.geometry, 0 };
        builder.addAll(values);

        final SimpleFeature feature = builder.buildFeature(type.getTypeName() +
                "." + 0);
        ((MemoryFeatureCollection)outGeodata).add(feature);

        // create shapefile
        final File outFile = new File(newShpName);

        final Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", DataUtilities.fileToURL(outFile));
        params.put("create spatial index", Boolean.TRUE);

        final DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

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
                featureStore.addFeatures(outGeodata);
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

    private void convertWKT(GeometryWrapper geometry, String file) throws IOException
    {
        final int suffixIndex = file.lastIndexOf(".");
        final String prefix = file.substring(0, suffixIndex);
        final String newName = prefix + "_converted" + ".wkt";
        FileImageOutputStream fos = new FileImageOutputStream(new File(newName));
        String wkt = wktwriter.write(geometry.geometry);
        fos.write(wkt.getBytes());
        fos.close();
    }

    public void dragEnter(DropTargetDragEvent dtde)
    {
    }

    public void dragExit(DropTargetEvent dte)
    {
    }

    public void dragOver(DropTargetDragEvent dtde)
    {
    }

    public void dropActionChanged(DropTargetDragEvent dtde)
    {
    }

    public void drop(DropTargetDropEvent dtde)
    {
        try
        {
            // Ok, get the dropped object and try to figure out what it is
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++)
            {
                System.out.println("Possible flavor: " +
                    flavors[i].getMimeType());
                // Check for file lists specifically
                if (flavors[i].isFlavorJavaFileListType())
                {
                    // Great! Accept copy drops...
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                    // And add the list of file names to our text area
                    java.util.List list = (java.util.List) tr.getTransferData(flavors[i]);
                    for (int j = 0; j < list.size(); j++)
                    {
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
                else if (flavors[i].isFlavorSerializedObjectType())
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    textAreaDragAndDrop.setText("Successful text drop.\n\n");

                    Object o = tr.getTransferData(flavors[i]);
                    textAreaDragAndDrop.append("Object: " + o);
                    dtde.dropComplete(true);

                    return;
                }
                // How about an input stream?
                else if (flavors[i].isRepresentationClassInputStream())
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    textAreaDragAndDrop.setText("Successful text drop.\n\n");
                    textAreaDragAndDrop.read(new InputStreamReader((InputStream) tr.getTransferData(flavors[i])),
                        "from system clipboard");
                    dtde.dropComplete(true);

                    return;
                }
            }
            // Hmm, the user must not have dropped a file list
            System.out.println("Drop failed: " + dtde);
            dtde.rejectDrop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }

    class GeometryWrapper
    {

        Geometry geometry;

        GeometryType type;

        public GeometryWrapper(Geometry geometry, GeometryType outputType)
        {
            super();
            this.geometry = geometry;
            this.type = outputType;
        }
    }
}
