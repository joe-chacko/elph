package io.openliberty.elph;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Command(
        name = Main.TOOL_NAME,
        mixinStandardHelpOptions = true,
        description = "Eclipse Liberty Project Helper - helps to create a usable Eclipse workspace for Open Liberty development",
        version = "Eclipse Liberty Project Helper v0.9",
        subcommands = {
                HelpCommand.class,
                Configure.class,
                Import.class
        }, // other subcommands are annotated methods
        defaultValueProvider = PropertiesDefaultProvider.class
)
public class Main {
    static final String TOOL_NAME = "elph";
    @Option(names = {"-o", "--ol-repo"}, hidden = true)
    Path olRepo;
    @Option(names = {"-e", "--eclipse-home"}, hidden = true)
    Path eclipseHome;
    @Option(names = {"-w", "--eclipse-workspace"}, hidden = true)
    Path eclipseWorkspace;
    @Option(names = "--finish-command", hidden = true)
    List<String> finishCommand;
    @Option(names = {"-h", "-?", "--help"}, usageHelp = true, hidden = true, description = "display this help message")
    boolean usageHelpRequested;
    @Option(names = {"-n", "--dry-run"}, description = "Do not run commands - just print them.")
    boolean dryRun;

    private BndCatalog catalog;

    public static void main(String... args) throws Exception {
        System.exit(new CommandLine(new Main()).setAbbreviatedSubcommandsAllowed(true).execute(args));
    }

    private Path getBndWorkspace() {
        if (null == olRepo) throw error("No OpenLiberty repository configured");
        return olRepo.resolve("dev");
    }

    BndCatalog getCatalog() {
        if (this.catalog == null) {
            Path bndWorkspace = getBndWorkspace();
            if (Files.isDirectory(bndWorkspace)) {
                try {
                    this.catalog = new BndCatalog(bndWorkspace);
                } catch (IOException e) {
                    throw error("Could not inspect bnd workspace: " + bndWorkspace);
                }
            } else {
                throw error("Could not locate bnd workspace: " + bndWorkspace);
            }
        }
        return this.catalog;
    }


    static Error error(String message, String... details) {
        System.err.println("ERROR: " + message);
        for (String detail: details) System.err.println(detail);
        System.exit(1);
        throw new Error();
    }

    static void verbose(String msg, Object...inserts) { System.out.println(String.format(msg, inserts)); }

    void runExternalCmd(List<String> cmd, String...extraArgs) {
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

    static boolean isMacOS() { return "Mac OS X".equals(System.getProperty("os.name")); }

    void invokeEclipse(Path path) { invokeEclipse(path.toString()); }

    void invokeEclipse(String path) {
    }
}