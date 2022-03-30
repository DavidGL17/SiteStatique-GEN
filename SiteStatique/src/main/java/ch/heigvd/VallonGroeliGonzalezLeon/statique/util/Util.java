/*
 * @File Util.java
 * @Authors : David González León, Jade Gröli, Axel Vallon
 * @Date 12 mars 2021
 */
package ch.heigvd.VallonGroeliGonzalezLeon.statique.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A class for general utility functions. This class is final and not instantiable
 */
public final class Util {

   public static final ArrayList<String> imageExtensionSupported = new ArrayList<>(Arrays.asList("jpg", "png"));

   private Util() { }

   /**
    * Copies images from the current directory to the build directory. Images must have extensions defined in the
    * imageExtensionSupported ArrayList.
    *
    * @param currentDirectory the directory where the images are located
    * @param buildDirectory   the destination directory
    */
   public static void copyImages(File currentDirectory, File buildDirectory) {
      File[] files = currentDirectory.listFiles();
      if (files != null) {
         for (File f : files) {
            if (f.isFile() && imageExtensionSupported.contains(FilenameUtils.getExtension(f.getName().toLowerCase()))) {
               File destFile = new File(buildDirectory.getPath() + "/" + f.getName());
               try {
                  FileUtils.copyFile(f, destFile);
               } catch (IOException e) {
                  e.printStackTrace();
                  return;
               }
            }
         }
      }
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
    * @param content the content to write
    * @param writer  The writer to use
    *
    * @throws IOException if there is an issue while using the given writer
    */
   public static void writeFile(String content, Writer writer) throws IOException {
      try {
         writer.write(content);
         writer.flush();
      } finally {
         writer.close();
      }
   }


   /**
    * Generates the path in the build directory of the given file
    *
    * @param baseDirectory the base directory, where the build directory is located
    * @param file          the file
    *
    * @return the path of the file in the build directory
    */
   public static Path generatePathInBuildDirectory(Path baseDirectory, Path file) {
      String endOfPath = file.toString().substring(baseDirectory.toString().length());
      return Paths.get(baseDirectory + "/build/" + endOfPath);
   }
}
