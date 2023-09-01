package io.openliberty.elph;

import picocli.CommandLine.Command;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import static io.openliberty.elph.Main.TOOL_NAME;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

@Command(name = "configure", description = "Change the directories used by " + TOOL_NAME + ".")
public class Configure implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

    @ParentCommand
    Main main;
    @Spec
    CommandSpec spec;

    @Option(names = {"-o", "--ol-repo"}, paramLabel = "DIR", description = "the local open-liberty git repository")
    Path newRepo;
    @Option(names = {"-e", "--eclipse-home"}, paramLabel = "DIR", description = "the app / directory containing Eclipse")
    Path newEclipseHome;
    @Option(names = {"-w", "--eclipse-workspace"}, paramLabel = "DIR", description = "the Eclipse workspace directory")
    Path newEclipseWorkspace;


    @Override
    public void run() {
        updatePath(main.olRepo, newRepo, "elph.ol-repo", "Locate your local Open Liberty git repository");
        updatePath(main.eclipseHome, newEclipseHome, "elph.eclipse-home", "Locate your Eclipse Application");
        updatePath(main.eclipseWorkspace, newEclipseWorkspace, "elph.eclipse-workspace", "Locate your local Open Liberty git repository");
    }

    private void updatePath(Path oldPath, Path newPath, String pathTypeName, String uiMsg) {
        if (newPath == null) newPath = Input.chooseDirectory(oldPath, uiMsg);
        if (newPath != null) save(pathTypeName, newPath);
    }

    private void save(String name, Path dir) {
        Properties prefs = new Properties();
        try {
            if (PROPS_FILE.exists()) prefs.load(new FileReader(PROPS_FILE));
            prefs.put(name, dir.toAbsolutePath().toString());
            prefs.store(new FileWriter(PROPS_FILE), "Written programmatically using: " + spec.name() + " " + spec.commandLine().getParseResult().originalArgs().stream().collect(joining(" ")));
        } catch (IOException e) {
            // TODO report error
            throw new RuntimeException(e);
        }
    }

    enum Input {
        ;
        final static Scanner SCANNER = new Scanner(System.in);

        static Path chooseDirectory(Path oldPath, String title) {
            System.out.printf("=== %s ===%n", title);
            String oldVal = oldPath == null ? "<not specified>" : oldPath.toString();
            System.out.println("Old value: " + oldVal);
            try {
                System.in.readNBytes(System.in.available());
            } catch (IOException ignored) {}
            System.out.println("Enter a new value (blank to leave unchanged): ");
            try {
                return Optional.of(SCANNER.nextLine()).filter(not(String::isBlank)).map(Paths::get).orElse(null);
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }
}
