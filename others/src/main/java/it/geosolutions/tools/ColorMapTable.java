package it.geosolutions.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;


public class ColorMapTable
{

    public static void main(String[] args) throws FileNotFoundException, IOException
    {
        FileImageInputStream fis = new FileImageInputStream(new File(
                    "c://palette.txt"));
        FileImageOutputStream fos = new FileImageOutputStream(new File(
                    "c://outColormap.txt"));
        String line = null;
        String index = null;
        while ((line = fis.readLine()) != null)
        {

            int colon = line.indexOf(":");
            index = line.substring(0, colon).trim();

            String colorComps = line.substring(colon + 1, line.length());
            String[] colors = colorComps.split(",");
            String[] hexStrings = new String[3];
            for (int i = 0; i < 3; i++)
            {
                hexStrings[i] = Integer.toHexString(Integer.parseInt(colors[i].trim()));
                if (hexStrings[i].length() == 1)
                {
                    hexStrings[i] = "0" + hexStrings[i];
                }
            }

            String map = "              <ColorMapEntry color=\"#" +
                hexStrings[0] +
                hexStrings[1] + hexStrings[2] + "\" quantity=\"" + index +
                "\" />\n";
            fos.writeBytes(map);
        }

        final int finalIndex = Integer.parseInt(index);
        if (finalIndex < 255)
        {
            for (int i = finalIndex + 1; i <= 255; i++)
            {
                String map = "              <ColorMapEntry color=\"#000000\" quantity=\"" +
                    i + "\" />\n";
                fos.writeBytes(map);
            }
        }

        fos.close();
    }

}
