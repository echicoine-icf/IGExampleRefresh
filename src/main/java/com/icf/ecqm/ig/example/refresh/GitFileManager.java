package com.icf.ecqm.ig.example.refresh;

import netscape.javascript.JSObject;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import com.google.gson.*;

import java.io.*;
import java.util.*;

public class GitFileManager {
    static String repoURL = "https://github.com/echicoine-icf/IGExampleRefresh.git";
    static String folderPath = "src/main/resources/SAMPLE";
    static String localFolderPath = "input/resources";
    static String examplesFilePath = "input/pagecontent/examples.md";
    //Clone repo to temp folder which gets deleted when we're done:

    public static void processExampleFiles() {
        // Step 1: Clone the repository to a temporary folder
        String repoLocation = "temp/repo";
        String filesMovedLocation = "temp/files";
        deleteFolder(new File("temp"));

        try (Git git = Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(repoLocation))
                .call()) {

            // Step 2: Transfer contents of folderPath to a new temporary folder
            File sourceFolder = new File(repoLocation, folderPath);
            File destFolder = new File(filesMovedLocation);
            FileUtils.copyDirectory(sourceFolder, destFolder);

            // Step 3: Process the new temporary folder, transfer files to input/resources, and create a hashmap
            Map<String, List<Pair<String, JsonObject>>> folderContentsMap = new HashMap<>();
            File[] folders = destFolder.listFiles(File::isDirectory);
            System.out.println(Arrays.toString(folders));
            if (folders != null) {
                for (File subfolder : folders) {
                    File destSubfolder = new File(localFolderPath);

                    //put the json files into the resource folder:
                    FileUtils.copyDirectory(subfolder, destSubfolder);

                    String folderName = subfolder.getName();
                    File[] fileNames = destSubfolder.listFiles();
                    List<Pair<String, JsonObject>> collectedFiles = new ArrayList<>();
                    if (fileNames != null) {
                        for (File file : fileNames) {
                            if (file.getAbsolutePath().endsWith(".json")) {
                                JsonObject json = parseJsonFromFile(file);
                                collectedFiles.add(new Pair<>(file.getName(), json));
                            }
                        }

                        folderContentsMap.put(folderName, collectedFiles);
                    }
                }
            }

            // Step 4: create example.md page:
            StringBuilder examplesMDContent = new StringBuilder();
            for (Map.Entry<String, List<Pair<String, JsonObject>>> entry : folderContentsMap.entrySet()) {
                examplesMDContent.append("\r\n**" + entry.getKey() + "**");
                for (Pair<String, JsonObject> jsonPair : entry.getValue()) {
                    JsonObject jsonObject = jsonPair.getRight();
                    String fileName = jsonPair.getLeft();
                    String exampleTitle = jsonObject.get("title").getAsString();
                    examplesMDContent.append("\n* [" + exampleTitle + "](" + fileName.replace(".json", ".html") + ")");
                }
                System.out.println(examplesMDContent);

                // Specify the path to the file
                File examplesMDFileLocationCurrent = new File(examplesFilePath);
                if (!examplesMDFileLocationCurrent.exists()){
                    examplesMDFileLocationCurrent.getParentFile().mkdirs();
                }
                // Write the content to the file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(examplesFilePath))) {
                    writer.write(examplesMDContent.toString());
                    System.out.println("Content written to file successfully!");
                } catch (IOException e) {
                    System.out.println("Error writing to file: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        //cleanup
        deleteFolder(new File("temp"));
    }


    private static boolean deleteFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursive call to delete subdirectory
                        deleteFolder(file);
                    } else {
                        // Delete file
                        if (!file.delete()) {
                            System.out.println("Failed to delete file: " + file.getAbsolutePath());
                            return false;
                        }
                    }
                }
            }

            // Delete the empty folder
            return folder.delete();
        } else {
            System.out.println("Folder does not exist: " + folder.getAbsolutePath());
            return false;
        }
    }

    private static JsonObject parseJsonFromFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

}
