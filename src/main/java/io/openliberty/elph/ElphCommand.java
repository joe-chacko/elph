package io.openliberty.elph;

import io.openliberty.elph.bnd.BndCatalog;
import io.openliberty.elph.io.IO;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.openliberty.elph.OS.MAC;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Command(
        name = ElphCommand.TOOL_NAME,
        mixinStandardHelpOptions = true,
        description = "Eclipse Liberty Project Helper - helps to create a usable Eclipse workspace for Open Liberty development",
        version = "Eclipse Liberty Project Helper v0.9",
        subcommands = {
                HelpCommand.class,
                ConfigureCommand.class,
                ImportCommand.class,
                ListCommand.class
        }, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class
)
public class ElphCommand {
    static final String TOOL_NAME = "elph";
    public static final String DOT_PROJECTS = ".metadata/.plugins/org.eclipse.core.resources/.projects";
    private Path olRepo;
    private Path eclipseHome;
    private Path eclipseWorkspace;
    @Option(names = "--finish-command", hidden = true)
    private List<String> finishCommand;
    @Option(names = {"-h", "-?", "--help"}, usageHelp = true, hidden = true, description = "display this help message")
    private boolean usageHelpRequested;
    @Option(names = {"-n", "--dry-run"}, description = "Do not run commands - just print them.")
    private boolean dryRun;
    @Mixin
    private IO io;
    private BndCatalog catalog;
    private boolean validationRequired = true;

    @Command(name = "start-eclipse", description = "Start Eclipse with the specified workspace. This is a good test of your " + TOOL_NAME + " configuration, but it does not replace other ways of launching Eclipse.")
    void startEclipse() {runExternal(getEclipseCmd());}

    void allowNullPaths() { validationRequired = false; }
    Path getOpenLibertyRepo() {
        if (validationRequired && null == olRepo) throw io.error("No Open Liberty repository configured");
        return olRepo;
    }
    Path getEclipseHome() {
        if (validationRequired  && null == eclipseHome) throw io.error("No Eclipse home configured");
        return eclipseHome;
    }
    Path getEclipseWorkspace() {
        if (validationRequired  && null == eclipseWorkspace) throw io.error("No Eclipse workspace configured");
        return eclipseWorkspace;
    }

    @Option(names = {"-o", "--ol-repo"}, paramLabel = "PATH", hidden = true)
    void setOpenLibertyRepo(Path path) {
        this.olRepo = path;
        if (!Files.isDirectory(olRepo)) io.warn("Open Liberty repository is not a valid directory: " + olRepo);
        else if (!Files.isDirectory(olRepo.resolve(".git"))) io.warn("Open Liberty repository does not appear to be a git repository: " + olRepo);
        else if (!Files.isDirectory(olRepo.resolve("dev"))) io.warn("Open Liberty repository does not contain an expected 'dev' subdirectory: " + olRepo);
    }
    @Option(names = {"-e", "--eclipse"}, paramLabel = "PATH", hidden = true)
    void setEclipseHome(Path path) {
        this.eclipseHome = path;
        if (!Files.isDirectory(eclipseHome)) io.warn("Eclipse Home is not a valid " + (OS.is(MAC) ? "app" : "directory") + ": " + olRepo);
        else if (OS.is(MAC) && !eclipseHome.toString().endsWith(".app")) io.warn("Eclipse Home is a directory but not a .app directory");
        else if (!Files.isExecutable(eclipseHome.resolve(OS.current().pathToExecutable))) throw io.error("Eclipse Home does not contain an executable in the expected place: " + eclipseHome.resolve(OS.current().pathToExecutable));
    }
    @Option(names = {"-w", "--workspace"}, paramLabel = "PATH", hidden = true)
    void setEclipseWorkspace(Path path) {
        this.eclipseWorkspace = path;
        if (!Files.isDirectory(eclipseWorkspace)) io.warn("Eclipse workspace is not a valid directory: " + eclipseWorkspace, "Hint: this directory will be created automatically by Eclipse if started appropriately.");
        else if (!Files.isDirectory(eclipseWorkspace.resolve(DOT_PROJECTS))) io.warn("Eclipse workspace does not containt expected subdirectory: " + eclipseWorkspace.resolve(DOT_PROJECTS));
    }

    private Path getBndWorkspace() {
        return getOpenLibertyRepo().resolve("dev");
    }

    BndCatalog getCatalog() {
        if (this.catalog == null) {
            Path bndWorkspace = getBndWorkspace();
            if (Files.isDirectory(bndWorkspace)) {
                try {
                    this.catalog = new BndCatalog(bndWorkspace, io);
                } catch (IOException e) {
                    throw io.error("Could not inspect bnd workspace: " + bndWorkspace);
                }
            } else {
                throw io.error("Could not locate bnd workspace: " + bndWorkspace);
            }
        }
        return this.catalog;
    }

    private List<String> getFinishCommand() { return null != finishCommand ? finishCommand : defaultFinishCommand(); }
    private List<String> defaultFinishCommand() {
        return OS.is(MAC) ? asList("osascript", "-e", "tell app \"System Events\" to tell process \"Eclipse\" to click button \"Finish\" of window 1") : null; }

    void runExternal(List<String> cmd, String...extraArgs) {
        cmd.addAll(asList(extraArgs));
        try {
            if (dryRun) io.report(cmd.stream().collect(joining("' '", "'" , "'")));
            new ProcessBuilder(cmd).inheritIO().start().waitFor();
        } catch (IOException e) {
            io.error("Error invoking command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        } catch (InterruptedException e) {
            io.error("Interrupted waiting for command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        }
    }

    void importProject(Path path) {
        io.infof("Importing %s", path);
        // invoke eclipse
        runExternal(getEclipseCmd(path));
        // optionally click finish
        pressFinish();
    }

    private List<String> getEclipseCmd(Path...args) {
        if (OS.is(MAC)) {
            return Stream.of(
                    Stream.of("open", "-a", getEclipseHome()),
                    Stream.of(args),
                    Stream.of("--args", "-data", getEclipseWorkspace())
            ).flatMap(s -> s).map(Object::toString).collect(Collectors.toList());
        } else {
            return Stream.concat(
                    Stream.of(getEclipseHome().resolve("eclipse"), "-data", getEclipseWorkspace()),
                    Stream.of(args)
            ).map(Object::toString).collect(Collectors.toList());
        }
    }

    void pressFinish() {
        var cmd = getFinishCommand();
        if (null == cmd) {
            io.logf("No configured way to press Finish - skipping");
            return;
        }
        io.logf("Invoking command to press finish: ", cmd.stream().collect(joining("\" \"", "\"", "\"")));
        runExternal(getFinishCommand());
    }

    Set<String> getProjectsInEclipse() {
        Path dotProjectsDir = getEclipseDotProjectsDir();
        try {
            return Files.list(dotProjectsDir)
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(not(s -> s.startsWith(".")))
                    .collect(toSet());
        } catch (IOException e) {
            throw io.error("Could not enumerate Eclipse projects despite finding metadata location: " + dotProjectsDir,
                    "Exception was " + e);
        }
    }

    Path getWorkspaceSettingsDir() {
        return io.verifyOrCreateDir(TOOL_NAME + " workspace settings directory", getEclipseWorkspace().resolve("." + TOOL_NAME));
    }

    private Path getEclipseDotProjectsDir() { return io.verifyDir(".projects dir", getEclipseWorkspace().resolve(DOT_PROJECTS)); }
}
