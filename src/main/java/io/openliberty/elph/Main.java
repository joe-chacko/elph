package io.openliberty.elph;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;
import picocli.CommandLine.Spec;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static javax.swing.JFileChooser.APPROVE_OPTION;

@Command(
        name = "elph",
        mixinStandardHelpOptions = true,
        description = "Eclipse Liberty Project Helper - helps to create a usable Eclipse workspace for Open Liberty development",
        version = "Eclipse Liberty Project Helper v0.9",
        subcommands = HelpCommand.class, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class
)
public class Main {
    @Spec
    CommandSpec spec;
    @Option(names = {"-l", "--repo"}, hidden = true)
    Path repo;
    @Option(names = {"-e", "--eclipse-home"}, hidden = true)
    Path eclipseHome;
    @Option(names = {"-w", "--eclipse-workspace"}, hidden = true)
    Path eclipseWorkspace;
    @Option(names = "--finish-command", hidden = true)
    List<String> finishCommand;

    public static void main(String... args) throws Exception {
        System.exit(new CommandLine(new Main()).setAbbreviatedSubcommandsAllowed(true).execute(args));
    }

    @Command(description = "Change the standard directories of ")
    public void configure(
            @Option(names = {"-o", "--ol-repo"}, paramLabel = "DIR", description = "Specify the local open-liberty git repository to read")
            Path newRepo,
            @Option(names = {"-e", "--eclipse-home"}, paramLabel = "DIR", description = "Specify the directory containing Eclipse")
            Path newEclipseHome,
            @Option(names = {"-w", "--eclipse-workspace"}, paramLabel = "DIR", description = "Specify the directory for the Eclipse workspace")
            Path newEclipseWorkspace) {
        if (newRepo == null) newRepo = chooseDirectory(this.repo, "Locate your local Open Liberty git repository");
        if (newRepo != null) save("elph.repo", newRepo);

    }

    public static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

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


    static Path chooseDirectory(Path oldPath, String title) {
        var chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (oldPath != null && Files.isDirectory(oldPath.getParent())) {
            // start from the previous selection if possible
            chooser.setCurrentDirectory(oldPath.getParent().toFile());
            if (Files.isDirectory(oldPath)) chooser.setSelectedFile(oldPath.toFile());
        } else {
            chooser.setCurrentDirectory(HOME_DIR.toFile());
        }
        chooser.setAcceptAllFileFilterUsed(false);
        if (APPROVE_OPTION == chooser.showOpenDialog(null)) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }
}