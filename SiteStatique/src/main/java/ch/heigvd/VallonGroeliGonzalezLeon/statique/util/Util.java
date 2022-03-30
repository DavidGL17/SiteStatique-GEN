/*
 * @File Util.java
 * @Authors : David González León
 * @Date 12 mars 2021
 */
package ch.heigvd.VallonGroeliGonzalezLeon.statique.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class for general utility functions
 */
public class Util {

   private static final ArrayList<String> imageExtensionSupported = new ArrayList<>(Arrays.asList("jpg", "png"));

   /**
    * Copies images from the current directory to the build directory. Images must have extensions defined in the
    * imageExtensionSupported ArrayList
    *
    * @param currentDirectory the directory where the images are located
    * @param buildDirectory the destination directory
    *
    * @return - 0 if the images were copied succesfully or if there was no image to copy
    *         - 1 if there was an error while copying the images
    */
   public static int copyImages(File currentDirectory, File buildDirectory) {
      File[] files = currentDirectory.listFiles();
      for (File f : files) {
         if (f.isFile() && imageExtensionSupported.contains(FilenameUtils.getExtension(f.getName()))) {
            File destFile = new File(buildDirectory.getPath() + "/" + f.getName());
            try {
               FileUtils.copyFile(f, destFile);
            } catch (IOException e) {
               e.printStackTrace();
               return 1;
            }
         }
      }
      return 0;
   }

   /**
    * Read a file using the given reader
    *
    * @param reader the reader to use
    *
    * @return the content given by the reader
    *
    * @throws IOException if there is an issue while reading the file
    */
   public static String readFile(Reader reader) throws IOException {
      StringBuilder result = new StringBuilder();
      try {
         int c = reader.read();
         while (c != -1) {
            result.append((char) c);
            c = reader.read();
         }
      } finally {
         reader.close();
      }
      return result.toString();
   }

   /**
    * Writes the given content using the given Writer
    *
    * @param content the sonctent to write
    * @param writer  The writer to use
    *
    * @throws IOException if there is an issue while using the given writer
    */
   public static void writeFile(String content, Writer writer) throws IOException {
      try {
         writer.write(content);
         writer.flush();
      }finally {
         writer.close();
      }
   }
}
