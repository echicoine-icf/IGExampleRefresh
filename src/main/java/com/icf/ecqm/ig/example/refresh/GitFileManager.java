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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitFileManager {
    static String repoURL = "https://github.com/echicoine-icf/IGExampleRefresh.git";
    static String folderPath = "src/main/java/resources/QI-CORE";
    static String localFolderPath = "input/resources"; // Replace with your local folder path

    //Clone repo to temp folder which gets deleted when we're done:

    public static void processExampleFiles() {
        // Step 1: Clone the repository to a temporary folder
        String tempFolder1 = "path/to/temp/folder1";

        try (Git git = Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(tempFolder1))
                .call()) {

            // Step 2: Transfer contents of src/main/java/resources/QI-CORE to a new temporary folder
            String tempFolder2 = "path/to/temp/folder2";
            File sourceFolder = new File(tempFolder1, folderPath);
            File destFolder = new File(tempFolder2);
            FileUtils.copyDirectory(sourceFolder, destFolder);

            // Step 3: Process the new temporary folder, transfer files to input/resources, and create a hashmap
            Map<String, List<Pair<String, JsonObject>>> folderContentsMap = new HashMap<>();
            File[] folders = destFolder.listFiles(File::isDirectory);
            if (folders != null) {
                for (File subfolder : folders) {
                    File destSubfolder = new File(subfolder, "input/resources");

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

            StringBuilder sb = new StringBuilder();


            for (Map.Entry<String, List<Pair<String, JsonObject>>> entry : folderContentsMap.entrySet()) {

                sb.append("\r\n**" + entry.getKey() + "**");

                for (Pair<String, JsonObject> jsonPair : entry.getValue()) {
                    JsonObject jsonObject = jsonPair.getRight();
                    String fileName = jsonPair.getLeft();
                    String exampleTitle = jsonObject.get("title").getAsString();

                    sb.append("\n* [" + exampleTitle + "](" + fileName.replace(".json", ".html") + ")");
                }


                System.out.println(sb);
            }

            // Step 5: Delete temporary folders
            FileUtils.deleteDirectory(new File(tempFolder1));
            FileUtils.deleteDirectory(destFolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JsonObject parseJsonFromFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

}
