/*
 * @File UtilTest.java
 * @Authors : David González León
 * @Date 12 mars 2021
 */
package ch.heigvd.VallonGroeliGonzalezLeon.statique.util;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

   private final File currentDirectory = new File(new File(".").getCanonicalPath() + "/Workspace");
   private final File buildDirectory = new File(currentDirectory.getPath() + "/build");

   UtilTest() throws IOException {}


   @BeforeEach
   void setUp() {
      currentDirectory.mkdir();
      buildDirectory.mkdir();
   }

   @AfterEach
   void tearDown() throws IOException {
      FileUtils.deleteDirectory(currentDirectory);
   }

   @Test
   void testWritingFileWorks() throws IOException {
      StringWriter writer = new StringWriter();
      String content = "This is a test to see if the writer works\n Let's hope it does.";
      assertDoesNotThrow(() -> Util.writeFile(content, writer));
      assertEquals(content, writer.toString());
   }

   @Test
   void testReadingFileWorks() throws IOException {
      File test = new File(currentDirectory.getPath()+"/test.txt");
      test.createNewFile();
      String content = "This is a test to see if the writer works\n Let's hope it does.";
      Util.writeFile(content, new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(test), StandardCharsets.UTF_8)));

      Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(test)));
      String result = Util.readFile(reader);
      assertEquals(result,content);
      test.delete();
   }

   @Test
   void testImageTransferDoesNothingIfThereIsNoImageToTransfer(){
      assertEquals(buildDirectory.listFiles().length, 0);
      Util.copyImages(currentDirectory,buildDirectory);
      assertEquals(buildDirectory.listFiles().length, 0);
   }

   @Test
   void testImageWorksIfDirectoryContainsImages() throws IOException {
      File pngImage = new File(currentDirectory.getPath()+"/imagepng.png");
      File jpgImage = new File(currentDirectory.getPath() + "/imagejpg.jpg");
      File txtFile = new File(currentDirectory.getPath()+"/test.txt");
      txtFile.createNewFile();
      Util.writeFile("This is a png image", new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(pngImage), StandardCharsets.UTF_8)));
      Util.writeFile("This is a jpg image", new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(jpgImage), StandardCharsets.UTF_8)));
      assertEquals(currentDirectory.listFiles().length,4);
      Util.copyImages(currentDirectory,buildDirectory);
      assertEquals(buildDirectory.listFiles().length,2);
      File pngImageBuild = new File(buildDirectory.getPath()+"/imagepng.png");
      File jpgImageBuild = new File(buildDirectory.getPath() + "/imagejpg.jpg");
      assertTrue(pngImageBuild.exists());
      assertTrue(jpgImageBuild.exists());
      assertEquals("This is a png image",
                   Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(pngImageBuild)))));
      assertEquals("This is a jpg image",
                   Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(jpgImageBuild)))));
   }

}