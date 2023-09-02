package io.openliberty.elph;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static io.openliberty.elph.ElphCommand.TOOL_NAME;
import static java.util.stream.Collectors.joining;

@Command(name = "configure", description = "Change the directories used by " + TOOL_NAME + ".")
public class ConfigureCommand implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

    @ParentCommand
    ElphCommand elph;
    @Spec
    CommandSpec spec;

    @Option(names = {"-o", "--ol-repo"}, paramLabel = "DIR", description = "the local open-liberty git repository")
    Path newRepo;
    @Option(names = {"-e", "--eclipse-home"}, paramLabel = "DIR", description = "the app / directory containing Eclipse")
    Path newEclipseHome;
    @Option(names = {"-w", "--eclipse-workspace"}, paramLabel = "DIR", description = "the Eclipse workspace directory")
    Path newEclipseWorkspace;
    @Option(names = {"-i", "--interactive"}, description = "ask for replacement values")
    boolean interactive;
    @Mixin
    IO io;

    @Override
    public void run() {
        updatePath(elph.getOpenLibertyRepo(), newRepo, "elph.ol-repo", "Open Liberty git repository");
        updatePath(elph.getEclipseHome(), newEclipseHome, "elph.eclipse-home", "Eclipse Application");
        updatePath(elph.getEclipseWorkspace(), newEclipseWorkspace, "elph.eclipse-workspace", "Eclipse Workspace");
    }

    private boolean reportingOnly() { return null == newRepo && null == newEclipseHome && null == newEclipseWorkspace && !interactive; }

    private void updatePath(Path oldPath, Path newPath, String pathTypeName, String uiMsg) {
        io.debugf("updatePath(%s, %s, \"%s\", \"%s\")%n", oldPath, newPath, pathTypeName, uiMsg);
        if (null == newPath && interactive) newPath = io.chooseDirectory(uiMsg, oldPath);
        else io.reportDirectory(uiMsg, oldPath, newPath);
        if (null != newPath) save(pathTypeName, newPath);
    }

    private void save(String name, Path dir) {
        Properties prefs = new Properties();
        try {
            if (PROPS_FILE.exists()) prefs.load(new FileReader(PROPS_FILE));
            dir = dir.toAbsolutePath();
            // normalize may simplify the path, but isn't guaranteed to be possible on all systems
            try {
                Path norm = dir.normalize();
                dir = norm;
            } catch (Throwable ignored) {}
            prefs.put(name, dir.toString());
            prefs.store(new FileWriter(PROPS_FILE), "Written programmatically using: " + spec.name() + " " + spec.commandLine().getParseResult().originalArgs().stream().collect(joining(" ")));
        } catch (IOException e) {
            // TODO report error
            throw new RuntimeException(e);
        }
    }
}
