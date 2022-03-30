/*
 * @File BuildTest.java
 * @Authors : David González León
 * @Date 19 mars 2021
 */
package ch.heigvd.VallonGroeliGonzalezLeon.statique.command;

import ch.heigvd.VallonGroeliGonzalezLeon.statique.Statique;
import ch.heigvd.VallonGroeliGonzalezLeon.statique.command.api.JsonAPI;
import ch.heigvd.VallonGroeliGonzalezLeon.statique.command.api.MdAPI;
import ch.heigvd.VallonGroeliGonzalezLeon.statique.command.api.TemplateHTML;
import ch.heigvd.VallonGroeliGonzalezLeon.statique.util.Util;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildTest {

   @BeforeEach
   void setUp() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath());
      File fileIndex = new File(testDirectory + "/index.md");
      if (fileIndex.exists()) {
         fileIndex.delete();
      }
      fileIndex.createNewFile();
      File fileConfig = new File(testDirectory + "/config.json");
      if (fileConfig.exists()) {
         fileConfig.delete();
      }
      fileConfig.createNewFile();
      File templateDir = new File(testDirectory.getPath() + "/template");

      templateDir.mkdir();
      File layout = new File(testDirectory + "/template/layout.html");
      if (layout.exists()) {
         layout.delete();
      }
      layout.createNewFile();
      MdAPI.initMdIndexFile(fileIndex);
      JsonAPI.initJSONConfigFile(fileConfig);
      TemplateHTML.initLayoutFile(layout);
      new CommandLine(new Statique()).execute("init", "/tmp/workspace");

   }

   @AfterEach
   void tearDown() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath());
      File fileIndex = new File(testDirectory + "/index.md");
      File fileConfig = new File(testDirectory + "/config.json");
      File templateDir = new File(testDirectory.getPath() + "/template");
      File buildDirectory = new File(new File(".").getCanonicalPath() + "/build");
      fileIndex.delete();
      fileConfig.delete();
      FileUtils.deleteDirectory(templateDir);
      FileUtils.deleteDirectory(buildDirectory);

      File workspace = new File(new File(".").getCanonicalPath() + "/tmp");
      if (workspace.exists()) {
         FileUtils.deleteDirectory(workspace);
      }
   }

   @Test
   void testBuildCreatesDirectory() throws IOException {
      new CommandLine(new Statique()).execute("build");
      File buildDirectory = new File(new File(".").getCanonicalPath() + "\\build");
      assertTrue(buildDirectory.exists());
   }


   @Test
   void testBuildCreatesFileWithGoodContent() throws IOException {
      File buildDirectory = new File(new File(".").getCanonicalPath() + "\\build");
      new CommandLine(new Statique()).execute("build");
      assertTrue(buildDirectory.exists());
      File index = new File(buildDirectory.getPath() + "/index.html");
      assertTrue(index.exists());
      String content = Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(index))));
      content = content.replace("\n", "").replace("\r", "");
      String expectedContent =
              "<html lang=\"FR\">\n<head>\n<meta charset=\"UTF-8\">\n<title> My statique website | Mon premier " +
              "article </title>\n</head>\n<body>\n<h1>Mon premier article</h1>\n<h2>Mon " +
              "sous-titre</h2>\n<p>Le contenu de mon article.</p>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);
   }

   @Test
   void testBuildWorksWithOption() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath() + "/tmp/workspace");
      assertTrue(testDirectory.exists());
      File buildDirectory = new File(testDirectory.getPath() + "\\build");
      new CommandLine(new Statique()).execute("build", "-p=/tmp/workspace");
      assertTrue(buildDirectory.exists());
      File index = new File(buildDirectory.getPath() + "/index.html");
      assertTrue(index.exists());
      String content = Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(index))));
      content = content.replace("\n", "").replace("\r", "");
      String expectedContent =
              "<html lang=\"FR\">\n<head>\n<meta charset=\"UTF-8\">\n<title> My statique website | Mon premier " +
              "article </title>\n</head>\n<body>\n<h1>Mon premier article</h1>\n<h2>Mon " +
              "sous-titre</h2>\n<p>Le contenu de mon article.</p>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);
   }

   @Test
   void testBuildWorksRecursivelyWithImages() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath() + "/tmp/workspace");
      File subDir = new File(testDirectory + "/tmpDir");
      subDir.mkdir();
      File pngFile = new File(subDir.getPath() + "/image.png");
      Util.writeFile("adoasihdoa",
                     new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pngFile), StandardCharsets.UTF_8)));
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      LocalDateTime now = LocalDateTime.now();
      String fileContent =
              "titre:Mon premier article\n" + "auteur:John Smith\n" + "date:" + dtf.format(now) + "\n" + "---\n";
      File mdFileSub = new File(subDir.getPath() + "/test.md");
      Util.writeFile(fileContent + "# Test\n## esperons que ça marche\n", new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(mdFileSub), StandardCharsets.UTF_8)));

      File buildDirectory = new File(testDirectory + "\\build");
      new CommandLine(new Statique()).execute("build", "-p=/tmp/workspace");

      File subHtmlFile = new File(buildDirectory.getPath() + "/tmpDir/test.html");
      assertTrue(subHtmlFile.exists());
      String content = Util.readFile(
              new BufferedReader(new InputStreamReader(new FileInputStream(subHtmlFile), StandardCharsets.UTF_8)));
      content = content.replace("\n", "").replace("\r", "");
      String expectedContent =
              "<html lang=\"FR\">\n<head>\n<meta charset=\"UTF-8\">\n<title> My statique website | Mon premier " +
              "article </title>\n</head>\n<body>\n<h1>Test</h1>\n<h2>esperons que ça marche</h2>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);
      File transferedImages = new File(buildDirectory.getPath() + "/tmpDir/image.png");
      assertTrue(transferedImages.exists());
   }

   @Test
   void testBuildDoesNotWorkWithoutKeyFiles() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath());
      File fileIndex = new File(testDirectory + "/index.md");
      File fileConfig = new File(testDirectory + "/config.json");
      File layout = new File(testDirectory + "/template/layout.html");
      fileIndex.delete();
      assertEquals(1, new CommandLine(new Statique()).execute("build"));
      layout.delete();
      assertEquals(1, new CommandLine(new Statique()).execute("build"));
      fileConfig.delete();
      assertEquals(1, new CommandLine(new Statique()).execute("build"));
   }

   @Test
   void fileTypeWorksCorrectly() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath());
      File fileIndex = new File(testDirectory + "/index.md");
      File fileConfig = new File(testDirectory + "/config.json");
      File templateFile = new File(testDirectory.getPath() + "/template/layout.html");
      assertEquals(Build.FileType.CONFIG, Build.FileType.getFileTypeFromFile(fileConfig, testDirectory));
      assertEquals(Build.FileType.LAYOUT, Build.FileType.getFileTypeFromFile(templateFile, testDirectory));
      assertEquals(Build.FileType.MD, Build.FileType.getFileTypeFromFile(fileIndex, testDirectory));
      assertEquals(Build.FileType.DIRECTORY, Build.FileType.getFileTypeFromFile(testDirectory, testDirectory));
      File tmp = new File(testDirectory + "/tmp.txt");
      assertEquals(Build.FileType.OTHER, Build.FileType.getFileTypeFromFile(tmp, testDirectory));
   }

   /* Marchent en local mais pas à distance
   @Test
   void watchingWorksCorrectlyMD() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath() + "/tmp/workspace");
      File mdIndex = new File(testDirectory + "/index.md");
      File buildDirectory = new File(testDirectory.getPath() + "/build");
      File toDeleteMd = new File(new File(".").getCanonicalPath() + "/index.md");
      toDeleteMd.delete();

      Thread thread = new Thread(() -> new CommandLine(new Statique()).execute("build", "-w", "-p=/tmp/workspace"));
      thread.start();
      try {
         Thread.sleep(3000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      //md files
      File index = new File(buildDirectory.getPath() + "/index.html");
      assertTrue(index.exists());
      String content = Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(index))));
      content = content.replace("\n", "").replace("\r", "");
      String expectedContent =
              "<html lang=\"FR\">\n<head>\n<meta charset=\"UTF-8\">\n<title> My statique website | Mon premier " +
              "article </title>\n</head>\n<body>\n{%include menu.html}\n<h1>Mon premier article</h1>\n<h2>Mon " +
              "sous-titre</h2>\n<p>Le contenu de mon article.</p>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);
      mdIndex.delete();
      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      assertFalse(index.exists());

      mdIndex.createNewFile();
      MdAPI.initMdIndexFile(mdIndex);

      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      assertTrue(index.exists());
      content = Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(index))));
      content = content.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);

      thread.interrupt();
   }

   @Test
   void watchingWorksCorrectlyConfigLayout() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath() + "/tmp/workspace");
      File fileConfig = new File(testDirectory + "/config.json");
      File buildDirectory = new File(testDirectory + "/build");

      Thread thread = new Thread(() -> new CommandLine(new Statique()).execute("build", "-w", "-p=/tmp/workspace"));
      thread.start();
      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      String chaine = Util.readFile(
              new BufferedReader(new InputStreamReader(new FileInputStream(fileConfig), StandardCharsets.UTF_8)));
      JSONObject obj = new JSONObject(chaine);
      obj.put("siteTitle", "My modified statique website");
      Util.writeFile(obj.toString(), new FileWriter(fileConfig));

      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      File index = new File(buildDirectory.getPath() + "/index.html");
      assertTrue(index.exists());
      String content = Util.readFile(new BufferedReader(new InputStreamReader(new FileInputStream(index))));
      content = content.replace("\n", "").replace("\r", "");
      String expectedContent =
              "<html lang=\"FR\">\n<head>\n<meta charset=\"UTF-8\">\n<title> My modified statique website | Mon " +
              "premier article </title>\n</head>\n<body>\n{%include menu.html}\n<h1>Mon premier article</h1>\n<h2>Mon" +
              " sous-titre</h2>\n<p>Le contenu de mon article.</p>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);

      thread.interrupt();
   }


   @Test
   void watchingWorksCorrectlyImage() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath() + "/tmp/workspace");
      File buildDirectory = new File(testDirectory + "/build");

      Thread thread = new Thread(() -> new CommandLine(new Statique()).execute("build", "-w","-p=/tmp/workspace"));
      thread.start();
      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      File testImage1 = new File(testDirectory.getPath() + "/truc.png");
      File testImage2 = new File(testDirectory.getPath() + "/machin.jpg");

      testImage1.createNewFile();
      testImage2.createNewFile();

      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      File outputImage1 = new File(buildDirectory.getPath() + "/truc.png");
      File outputImage2 = new File(buildDirectory.getPath() + "/machin.jpg");
      assertTrue(outputImage1.exists());
      assertTrue(outputImage2.exists());

      testImage1.delete();
      testImage2.delete();

      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      assertFalse(outputImage1.exists());
      assertFalse(outputImage2.exists());

      thread.interrupt();
   }

   @Test
   void watchingWorksCorrectlyDirectory() throws IOException {
      File testDirectory = new File(new File(".").getCanonicalPath() + "/tmp/workspace");
      File buildDirectory = new File(testDirectory + "/build");

      Thread thread = new Thread(() -> new CommandLine(new Statique()).execute("build", "-w","-p=/tmp/workspace"));
      thread.start();
      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      File tmpDirectory = new File(testDirectory.getPath() + "/truc");
      tmpDirectory.mkdir();
      File tmpIndexFile = new File(tmpDirectory.getPath() + "/tmp.md");
      MdAPI.initMdIndexFile(tmpIndexFile);

      try {
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      File outputDir = new File(buildDirectory.getPath() + "/truc");
      assertTrue(outputDir.exists());

      thread.interrupt();
      FileUtils.deleteDirectory(tmpDirectory);
   }*/
}