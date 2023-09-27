package io.openliberty.elph.cmd;

import io.openliberty.elph.bnd.BndCatalog;
import io.openliberty.elph.util.IO;
import io.openliberty.elph.util.OS;
import picocli.AutoComplete.GenerateCompletion;
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

import static io.openliberty.elph.util.IO.Verbosity.DEBUG;
import static io.openliberty.elph.util.OS.MAC;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

@Command(
        name = ElphCommand.TOOL_NAME,
        mixinStandardHelpOptions = true,
        description = "Eclipse Liberty Project Helper - helps to create a usable Eclipse workspace for Open Liberty development",
        version = "Eclipse Liberty Project Helper v1.1",
        subcommands = {
                GenerateCompletion.class,
                HelpCommand.class,
                SetupCommand.class,
                AnalyzeCommand.class,
                EclipseCommand.class,
                ListCommand.class,
                ImportCommand.class,
                ProjectCommand.class,
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
        if (!Files.isDirectory(eclipseHome)) io.warn("Eclipse Home is not a valid " + (OS.is(MAC) ? "app" : "directory") + ": " + eclipseHome);
        else if (OS.is(MAC) && !eclipseHome.toString().endsWith(".app")) io.warn("Eclipse Home is a directory but not a .app directory");
        else if (!Files.isExecutable(eclipseHome.resolve(OS.current().pathToExecutable))) io.warn("Eclipse Home does not contain an executable in the expected place: " + eclipseHome.resolve(OS.current().pathToExecutable));
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
                    this.catalog = new BndCatalog(bndWorkspace, io, getRepoSettingsDir());
                } catch (IOException e) {
                    throw io.error("Could not inspect bnd workspace: " + bndWorkspace);
                }
            } else {
                throw io.error("Could not locate bnd workspace: " + bndWorkspace);
            }
        }
        return this.catalog;
    }

    void runExternal(boolean blocking, List<String> cmd) {
        try {
            if (dryRun) {
                io.report(cmd.stream().collect(joining("' '", "'" , "'")));
                return;
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            // output can be excessive e.g. when starting eclipse
            // inheritIO means the child process ALWAYS prints to the console,
            // even if the parent dies, so for non-blocking processes only do this when debug is enabled.
            // For blocking processes, we must ensure the child's pipes are drained or the child process could hang.
            // The simplest way to do this is to inherit IO.
            if (blocking || io.isEnabled(DEBUG)) pb.inheritIO();
            Process p = pb.start();
            if (blocking) p.waitFor();
        } catch (IOException e) {
            io.error("Error invoking command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        } catch (InterruptedException e) {
            io.error("Interrupted waiting for command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        }
    }

    private List<List<String>> getBunchedEclipseCmds(Collection<Path> paths) {
        if (paths.isEmpty()) return emptyList();
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

    private List<String> getEclipseCmd(Path... projects) {
        return OS.current().getEclipseCmd(getEclipseHome(), getEclipseWorkspace(), projects);
    }

    void startEclipse() {
        List<String> cmd = getEclipseCmd();
        runExternal(false, cmd);
    }

    void importProjects(Collection<Path> projectPaths) {
        getBunchedEclipseCmds(projectPaths).forEach(cmd -> runExternal(true, cmd));
    }

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

    Path getRepoSettingsDir() {
        Path dir = getOpenLibertyRepo().resolve("." + TOOL_NAME);
        if (!Files.isDirectory(dir)) {
            io.verifyOrCreateDir(TOOL_NAME + " git repository settings directory", dir);
            // make sure the entire contents of the directory are ignored, including the .gitignore
            io.writeFile(".elph git ignore file", dir.resolve(".gitignore"), "*");
        }
        return dir;
    }

    private Path getEclipseDotProjectsDir() { return io.verifyDir(".projects dir", getEclipseWorkspace().resolve(DOT_PROJECTS)); }
}
