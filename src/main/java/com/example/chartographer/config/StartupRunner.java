package com.example.chartographer.config;

import com.example.chartographer.image.Charta;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
public class StartupRunner implements CommandLineRunner {

    private static Path pathToWorkingDirectory;
    private static StartOptions startOptions;
    @Autowired
    private Gson gson;

    public static Path getPathToWorkingDirectory() {
        return pathToWorkingDirectory;
    }

    public static Path workDirResolve(String filename) {
        return pathToWorkingDirectory.resolve(filename);
    }

    @Override
    public void run(String... args) {
        if (args.length == 0 || !initWorkingDirectory(args[0])) {
            pathToWorkingDirectory = Path.of(System.getProperty("user.dir"));
            System.err.println("Unable to configure given path. Using app directory.");
        }
        if (!loadStartOptions()) {
            startOptions = new StartOptions();
        }
        Charta.setNextId(startOptions.getNextId());
    }

    private boolean initWorkingDirectory(String path) {
        try {
            Path pathToWorkDir = Path.of(path);
            if (Files.isDirectory(pathToWorkDir)
                    && Files.isWritable(pathToWorkDir)) {
                pathToWorkingDirectory = pathToWorkDir;
                return true;
            }
        } catch (InvalidPathException e) {
            return false;
        }
        return false;
    }

    private boolean loadStartOptions() {
        try (Reader reader = Files.newBufferedReader(workDirResolve("options.json"))) {
            StartOptions options = gson.fromJson(reader, StartOptions.class);
            startOptions = options;
            return options != null;
        } catch (IOException e1) {
            try (Writer writer = Files.newBufferedWriter(workDirResolve("options.json"))) {
                gson.toJson(startOptions = new StartOptions(), writer);
            } catch (IOException e2) {
                System.err.println("Can't init options. Make sure the working directory is fully accessible.");
                return false;
            }
        }
        return true;
    }

    @PreDestroy
    private void onShutdown() {
        saveStartOptions();
    }

    private void saveStartOptions() {
        try (Writer writer = Files.newBufferedWriter(workDirResolve("options.json"))) {
            startOptions.setNextId(Charta.getNextId());
            gson.toJson(startOptions, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
