package io.openliberty.elph.cmd;

import io.openliberty.elph.bnd.BndCatalog;
import io.openliberty.elph.util.IO;
import io.openliberty.elph.util.OS;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.openliberty.elph.util.OS.MAC;
import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

@Command(
        name = ElphCommand.TOOL_NAME,
        mixinStandardHelpOptions = true,
        description = "Eclipse Liberty Project Helper - helps to create a usable Eclipse workspace for Open Liberty development",
        version = "Eclipse Liberty Project Helper v0.10",
        subcommands = {
                HelpCommand.class,
                SetupCommand.class,
                EclipseCommand.class,
                ListCommand.class,
                ImportCommand.class,
                ReimportCommand.class,
                ForgetCommand.class,
                CheckCommand.class,
        }, // subcommands can also be annotated methods
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
        else if (!Files.isDirectory(eclipseWorkspace.resolve(DOT_PROJECTS))) io.warn("Eclipse workspace does not contain expected subdirectory: " + eclipseWorkspace.resolve(DOT_PROJECTS));
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

    List<List<String>> getBunchedEclipseCmds(Collection<Path> paths) {
        // Allow an arg list of total String length 4096.
        // Even Windows XP allows twice this length for the whole command.
        // So we should still be safe even after adding the rest of the Eclipse command line.
        final int max = 4096;
        List<List<String>> cmds = new ArrayList<>();
        List<Path> buffer = new ArrayList<>();
        int length = 0;
        for (Path p: paths) {
            // flush the buffer into a new command when we reach the limit
            if (length + p.toString().length() > max) {
                cmds.add(getEclipseCmd(buffer.toArray(Path[]::new)));
                length = 0;
                buffer.clear();
            }
            buffer.add(p);
        }
        // put any remaining paths into a final eclipse command
        cmds.add(getEclipseCmd(buffer.toArray(Path[]::new)));
        return cmds;
    }

    List<String> getEclipseCmd(Path...args) {
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

    void pressFinish() { if (isFinishCommandAvailable()) runExternal(getFinishCommand()); }
    boolean isFinishCommandAvailable() { return null != getFinishCommand(); }

    Set<Path> getBndProjects() {
        return getCatalog().findProjects("*").collect(toCollection(TreeSet::new));
    }

    Set<Path> getEclipseProjects() {
        return getEclipseProjectNames().stream().map(getOpenLibertyRepo().resolve("dev")::resolve).collect(toCollection(TreeSet::new));
    }

    Set<String> getEclipseProjectNames() {
        Path dotProjectsDir = getEclipseDotProjectsDir();
        try {
            io.debugf("Finding known projects");
            return Files.list(dotProjectsDir)
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(not(s -> s.startsWith(".")))
                    .peek(s -> io.debugf("Known project: %s", s))
                    .collect(toCollection(TreeSet::new));
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
