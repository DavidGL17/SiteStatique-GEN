package ch.heigvd.VallonGroeliGonzalezLeon.statique.command;


import ch.heigvd.VallonGroeliGonzalezLeon.statique.command.api.TemplateHTML;
import ch.heigvd.VallonGroeliGonzalezLeon.statique.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "build", mixinStandardHelpOptions = true,
                     description = "Builds the site using the given markdown and json files. This command needs to be" +
                                   " executed at the root of the directory generated by the init command.\n" +
                                   "If there are any images that are in the same directory as the markdown and json " +
                                   "files, they will be copied to the corresponding build file.\n" +
                                   "If the config file, the layout file or the main md file are missing from their" +
                                   " normal location, the command will return 1 and delete the build directory\n" +
                                   "In case of an error while writing or reading the command will return 2\n" +
                                   "The program will also compile all md files in subdirectories of the current " +
                                   "directory. ")
public class Build implements Callable<Integer> {

   @CommandLine.Option(names = {"-w", "--watching"},
                       description = "Enables background continuous analysis of the project") boolean watching;

   @Override
   public Integer call() {
      File currentDirectory;
      try {
         currentDirectory = new File(new File(".").getCanonicalPath());
      } catch (IOException e) {
         System.err.println("Error while reading current directory");
         e.printStackTrace();
         return 2;
      }
      File buildDirectory = new File(currentDirectory.getPath() + "\\build");
      if (buildDirectory.exists()) {
         try {
            FileUtils.deleteDirectory(buildDirectory);
         } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Build directory deletion failure.");
         }
      }
      buildDirectory.mkdir();

      File jsonFile = new File(currentDirectory.getPath() + "/config.json");
      if (!jsonFile.exists()) {
         System.err.println("Config file missing");
         return 1;
      }

      File layoutFile = new File(currentDirectory.getPath() + "/template/layout.html");
      if (!layoutFile.exists()) {
         System.err.println("Layout file missing");
         return 1;
      }

      File mdIndexFile = new File(currentDirectory.getPath() + "/index.md");
      if (!mdIndexFile.exists()) {
         System.err.println("Main md file missing");
         return 1;
      }
      TemplateHTML templateHTML;
      try {
         templateHTML = new TemplateHTML(layoutFile, jsonFile);
      } catch (IOException e) {
         System.err.println("Error while reading the layout and config file");
         return 2;
      }

      try {
         buildAll(templateHTML, mdIndexFile, currentDirectory, buildDirectory);
      } catch (IOException e) {
         return 2;
      }

      if (!watching) {
         return 0;
      }
      /**
       * Ajouter un warning : ne pas modifier le dossier build (supprimer,...) pendant que la commande tourne en
       * background, ni le nom du dossier template
       *
       * - md : recompiler fichier on create,modify et supprimer si delete
       * - json,template : tout recompiler on modify et lancer une erreur on delete
       * - images : déplacer dans le dossier build correspondant on create, modify, supprimer on delete
       * - dir : compiler le dossier en cas de création, supprime en cas de delete, et changer le nom du dossier
       * build en cas de modify
       */
      try {
         WatchService watcher = FileSystems.getDefault().newWatchService();
         Path dir = currentDirectory.toPath();
         dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                      StandardWatchEventKinds.ENTRY_DELETE);
         while (true) {
            WatchKey key = watcher.take();
            while (key != null) {
               for (WatchEvent<?> event : key.pollEvents()) {
                  WatchEvent.Kind<?> kind = event.kind();
                  WatchEvent<Path> ev = (WatchEvent<Path>) event;
                  Path filename = ev.context();

                  switch (FileType.getFileTypeFromFile(filename.toFile(), currentDirectory)) {
                     case MD:
                        handleMd(ev, currentDirectory, templateHTML);
                        break;
                     case IMAGE:
                        handleImage(ev);
                        break;
                     case CONFIG:
                     case LAYOUT:
                        handleConfigAndLayout(ev, templateHTML, mdIndexFile, currentDirectory, buildDirectory);
                        break;
                     case DIRECTORY:
                        handleDirectory(ev, templateHTML,null,null);
                        break;
                  }
               }
               key.reset();
               key = watcher.take();
            }
         }
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
      return 0;
   }


   private void handleMd(WatchEvent<Path> event, File baseDirectory, TemplateHTML templateHTML) {
      // /site/machin/2/4/truc/test.md -> /site/build/machin/2/4/truc/test.md
      WatchEvent.Kind<?> kind = event.kind();
      Path fileModified = event.context();
      Path finalPathInMD = Util.generatePathInBuildDirectory(Paths.get(baseDirectory.getPath()), fileModified);
      File fileHTML = new File(finalPathInMD.toString().replace(".md", ".html"));
      //File toEdit = buildDirectory.getPath() + " / " + fileModified.

      if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
         try {
            createHTMLPage(templateHTML, fileModified.toFile(), finalPathInMD.getParent().toFile());
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
         fileHTML.delete();
         try {
            createHTMLPage(templateHTML, fileModified.toFile(), finalPathInMD.getParent().toFile());
         } catch (IOException e) {
            e.printStackTrace();
         }

      } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
         fileHTML.delete();
      }
   }


   private void handleConfigAndLayout(WatchEvent<Path> event, TemplateHTML templateHTML, File mdIndexFile,
                                      File currentDirectory, File buildDirectory) throws IOException {
      WatchEvent.Kind<?> kind = event.kind();
      if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
         buildAll(templateHTML, mdIndexFile, currentDirectory, buildDirectory);
      } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
         //TODO arreter programme?
         System.err.println("Error : if you delete or rename the config.json or layout.html files, the application " +
                            "will not work properly");
      }
   }

   /* - images : déplacer dans le dossier build correspondant on create, modify, supprimer on delete
       * - dir : compiler le dossier en cas de création, supprime en cas de delete, et changer le nom du dossier
       * build en cas de modify
       */
   private void handleDirectory(WatchEvent<Path> event, TemplateHTML templateHTML, File currentDir, File currentBuildDir){
      WatchEvent.Kind<?> kind = event.kind();
      if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
         try {
            recursiveBuild(templateHTML, currentDir, currentBuildDir);
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {

      } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

      }
   }
   private void handleImage(WatchEvent<Path> event){


   }

   /**
    * Creates html files from md in subdirs, and translates all the images found
    *
    * @param templateHTML
    * @param currentDir
    * @param currentBuildDir
    *
    * @throws IOException
    */
   private void recursiveBuild(TemplateHTML templateHTML, File currentDir, File currentBuildDir) throws IOException {
      if (currentDir.listFiles() != null) {
         for (File f : currentDir.listFiles()) {
            if (f.getName().toLowerCase().endsWith(".md")) {
               currentBuildDir.mkdir();
               createHTMLPage(templateHTML, f, currentBuildDir);
            }
         }
         Util.copyImages(currentDir, currentBuildDir);
         for (File f : currentDir.listFiles()) {
            if (f.isDirectory() && !f.getName().equals("build")) {
               File futurBuildDir = new File(currentBuildDir.getPath() + "/" + f.getName());
               recursiveBuild(templateHTML, f, futurBuildDir);
            }
         }
      }
   }

   private void createHTMLPage(TemplateHTML templateHTML, File mdFile, File targetDirectory) throws IOException {
      String htmlContent;
      try {
         htmlContent = templateHTML.generatePage(mdFile);
      } catch (IOException e) {
         System.err.println("Error while reading the mdFile");
         throw e;
      }
      try {
         String fileName = "/" + mdFile.getName().replace(".md", "") + ".html";
         File indexHtmlFile = new File(targetDirectory.getPath() + fileName);
         Util.writeFile(htmlContent, new BufferedWriter(
                 new OutputStreamWriter(new FileOutputStream(indexHtmlFile), StandardCharsets.UTF_8)));
      } catch (IOException e) {
         System.err.println("Error while writing the html file");
         throw e;
      }
   }

   private void buildAll(TemplateHTML templateHTML, File mdIndexFile, File currentDirectory, File buildDirectory)
           throws IOException {
      String indexContent;
      try {
         indexContent = templateHTML.generatePage(mdIndexFile);
      } catch (IOException e) {
         System.err.println("Error while reading the mdFile");
         throw e;
      }

      try {
         File indexHtmlFile = new File(buildDirectory.getPath() + "/index.html");
         Util.writeFile(indexContent, new BufferedWriter(
                 new OutputStreamWriter(new FileOutputStream(indexHtmlFile), StandardCharsets.UTF_8)));
         Util.copyImages(currentDirectory, buildDirectory);
      } catch (IOException e) {
         System.err.println("Error while writing the html file");
         throw e;
      }

      for (File f : currentDirectory.listFiles()) {
         if (f.isDirectory() && !f.getName().equals("build")) {
            File futurBuildDir = new File(buildDirectory.getPath() + "/" + f.getName());
            try {
               recursiveBuild(templateHTML, f, futurBuildDir);
            } catch (IOException e) {
               throw e;
            }
         }
      }
   }

   enum FileType {
      MD, LAYOUT, CONFIG, IMAGE, DIRECTORY, OTHER;

      public static FileType getFileTypeFromFile(File file, File rootDirectory) {
         if (file.isDirectory()) {
            return DIRECTORY;
         }
         String name = file.getName();
         switch (FilenameUtils.getExtension(name).toLowerCase()) {
            case "md":
               return MD;
            case "html":
               if (name.equals("layout.html") && file.getParentFile().getName().equals("template") &&
                   file.getParentFile().getParentFile().equals(rootDirectory)) {
                  return LAYOUT;
               }
               break;
            case "json":
               if (name.equals("config.json") && file.getParentFile().equals(rootDirectory)) {
                  return CONFIG;
               }
               break;
            case "png":
            case "jpg":
               return IMAGE;
         }
         return OTHER;
      }
   }

}