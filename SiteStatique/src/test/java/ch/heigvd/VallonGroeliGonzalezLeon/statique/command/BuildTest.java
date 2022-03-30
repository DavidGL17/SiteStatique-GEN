package ch.heigvd.VallonGroeliGonzalezLeon.statique.command;/*
 * @File BuildTest.java
 * @Authors : David González León
 * @Date 19 mars 2021
 */

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
              "article </title>\n</head>\n<body>\n{%include menu.html}\n<h1>Mon premier article</h1>\n<h2>Mon " +
              "sous-titre</h2>\n<p>Le contenu de mon article.</p>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);
   }

   @Test
   void testBuildWorksWithOption() throws IOException {
      new CommandLine(new Statique()).execute("init", "/workspace");
      File testDirectory = new File(new File(".").getCanonicalPath()+"/workspace");
      assertTrue(testDirectory.exists());
      File buildDirectory = new File(testDirectory.getPath() + "\\build");
      new CommandLine(new Statique()).execute("build", "-p=/workspace");
      assertTrue(buildDirectory.exists());
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
      FileUtils.deleteDirectory(testDirectory);
   }

   @Test
   void testBuildWorksRecursivelyWithImages() throws IOException {
      File subDir = new File(new File(".").getCanonicalPath() + "/tmpDir");
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

      File buildDirectory = new File(new File(".").getCanonicalPath() + "\\build");
      new CommandLine(new Statique()).execute("build");

      File subHtmlFile = new File(buildDirectory.getPath() + "/tmpDir/test.html");
      assertTrue(subHtmlFile.exists());
      String content = Util.readFile(
              new BufferedReader(new InputStreamReader(new FileInputStream(subHtmlFile), StandardCharsets.UTF_8)));
      content = content.replace("\n", "").replace("\r", "");
      String expectedContent =
              "<html lang=\"FR\">\n<head>\n<meta charset=\"UTF-8\">\n<title> My statique website | Mon premier " +
              "article </title>\n</head>\n<body>\n{%include menu" +
              ".html}\n<h1>Test</h1>\n<h2>esperons que ça marche</h2>\n\n</body>\n</html>";
      expectedContent = expectedContent.replace("\n", "").replace("\r", "");
      assertEquals(expectedContent, content);
      File transferedImages = new File(buildDirectory.getPath() + "/tmpDir/image.png");
      assertTrue(transferedImages.exists());
      FileUtils.deleteDirectory(subDir);
   }

}