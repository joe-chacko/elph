package io.openliberty.elph;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

@Command(
        name = ElphCommand.TOOL_NAME,
        mixinStandardHelpOptions = true,
        description = "Eclipse Liberty Project Helper - helps to create a usable Eclipse workspace for Open Liberty development",
        version = "Eclipse Liberty Project Helper v0.9",
        subcommands = {
                HelpCommand.class,
                ConfigureCommand.class,
                ImportCommand.class
        }, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class
)
public class ElphCommand {
    static final String TOOL_NAME = "elph";
    @Option(names = {"-o", "--ol-repo"}, hidden = true)
    private
    Path olRepo;
    @Option(names = {"-e", "--eclipse-home"}, hidden = true)
    private
    Path eclipseHome;
    @Option(names = {"-w", "--eclipse-workspace"}, hidden = true)
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

    Path getOpenLibertyRepo() { return olRepo; }
    Path getEclipseHome() { return eclipseHome; }
    Path getEclipseWorkspace() { return eclipseWorkspace; }

    private Path getBndWorkspace() {
        if (null == getOpenLibertyRepo()) throw io.error("No OpenLiberty repository configured");
        return getOpenLibertyRepo().resolve("dev");
    }

    BndCatalog getCatalog() {
        if (this.catalog == null) {
            Path bndWorkspace = getBndWorkspace();
            if (Files.isDirectory(bndWorkspace)) {
                try {
                    this.catalog = new BndCatalog(bndWorkspace);
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
    private List<String> defaultFinishCommand() { return Util.isMacOS() ? asList("osascript", "-e", "tell app \"System Events\" to tell process \"Eclipse\" to click button \"Finish\" of window 1") : null; }

    void runExternal(List<String> cmd, String...extraArgs) {
        Stream.of(extraArgs).forEach(cmd::add);
        try {
            if (dryRun) cmd.stream().collect(joining("' '", "'" , "'"));
            new ProcessBuilder(cmd).inheritIO().start().waitFor();
        } catch (IOException e) {
            io.error("Error invoking command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        } catch (InterruptedException e) {
            io.error("Interrupted waiting for command " + cmd.stream().collect(joining("' '", "'", "'")) + e.getMessage());
        }
    }

    void importProject(Path path) {
        io.logf("Invoking eclipse to import %s", path);
        var args = Util.isMacOS() ?
                Stream.of("open", "-a", getEclipseHome(), path, "--args", "-data", getEclipseWorkspace()) :
                Stream.of(getEclipseHome().resolve("eclipse"), "-data", getEclipseWorkspace(), path);
        var eclipseCommand = args
                .map(Object::toString)
                .collect(Collectors.toList());
        // invoke eclipse
        runExternal(eclipseCommand);
        // optionally click finish
        pressFinish();
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
}
