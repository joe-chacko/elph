package io.openliberty.elph;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
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
    @Option(names = {"-o", "--ol-repo"}, hidden = true)
    Path repo;
    @Option(names = {"-e", "--eclipse-home"}, hidden = true)
    Path eclipseHome;
    @Option(names = {"-w", "--eclipse-workspace"}, hidden = true)
    Path eclipseWorkspace;
    @Option(names = {"-c", "--eclipse-command"}, split="\n", splitSynopsisLabel = "\\n", description = "Command to open a directory for import into eclipse")
    List<String> eclipseCommand;
    @Option(names = "--finish-command", hidden = true)
    List<String> finishCommand;
    @Option(names = {"-h", "-?", "--help"}, usageHelp = true, hidden = true, description = "display this help message")
    boolean usageHelpRequested;
    @Option(names = {"-n", "--dry-run"}, description = "Do not run commands - just print them.")
    boolean dryRun;

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
        updatePath(this.repo, newRepo, "elph.ol-repo", "Locate your local Open Liberty git repository");
        updatePath(this.eclipseHome, newEclipseHome, "elph.eclipse-home", "Locate your Eclipse Application");
        updatePath(this.eclipseWorkspace, newEclipseWorkspace, "elph.eclipse-workspace", "Locate your local Open Liberty git repository");
    }

    @Command(description = "Automatically add project to Eclipse. \nPlease ensure both your terminal and eclipse applications aren't full screen.")
    public void importProject(
            @Parameters(paramLabel = "project", arity = "1", description = "The name the project you want to import")
            String projectName) {
        if(null == repo) {
            System.out.println("You must first specify the location of your Open Liberty dev repo using the 'elph configure'");
            return;
        }
        invokeEclipse(projectName);
    }

    private void updatePath(Path oldPath, Path newPath, String pathTypeName, String uiMsg) {
        if (newPath == null) newPath = chooseDirectory(oldPath, uiMsg);
        if (newPath != null) save(pathTypeName, newPath);
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

    static Error error(String message, String... details) {
        System.err.println("ERROR: " + message);
        for (String detail: details) System.err.println(detail);
        System.exit(1);
        throw new Error();
    }

    void verbose(String msg, Object...inserts) { System.out.println(String.format(msg, inserts)); }

    private void run(List<String> cmd, String...extraArgs) {
        Stream.of(extraArgs).forEach(cmd::add);
        try {
            if (dryRun) cmd.stream().collect(joining(" ", "'" , "'"));
            new ProcessBuilder(cmd).inheritIO().start().waitFor();
        } catch (IOException e) {
            error("Error invoking command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        } catch (InterruptedException e) {
            error("Interrupted waiting for command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        }
    }

    private static boolean isMacOS() { return "Mac OS X".equals(System.getProperty("os.name")); }

    private List<String> requireEclipseCommand() { return Optional.ofNullable(getEclipseCommand()).orElseThrow(() -> error("Must specify eclipse command in order to automate eclipse actions.")); }

    private List<String> getEclipseCommand() { return null != eclipseCommand ? eclipseCommand : defaultEclipseCommand(); }

    private List<String> defaultEclipseCommand() {
        if (!isMacOS())  return null;
        String eclipseAppLocation = eclipseHome == null ? "Eclipse" : eclipseHome.toString();
        return isMacOS() ? new ArrayList<>(asList(String.format("open -a %s", eclipseHome).split(" "))) : null;
    }

    private List<String> getFinishCommand() { return  null != finishCommand ? finishCommand : defaultFinishCommand(); }
    private List<String> defaultFinishCommand() { return isMacOS() ? asList("osascript", "-e", "tell app \"System Events\" to tell process \"Eclipse\" to click button \"Finish\" of window 1") : null; }


    void invokeEclipse(Path path) { invokeEclipse(path.toString()); }
    void invokeEclipse(String path) {
        String fullPath = repo+"/dev/"+path;
        verbose("Invoking eclipse to import %s", fullPath);
        // invoke eclipse
        run(requireEclipseCommand(), fullPath);
        // optionally click finish
        Optional.ofNullable(getFinishCommand()).filter(not(List::isEmpty)).ifPresent(this::run);
    }
}