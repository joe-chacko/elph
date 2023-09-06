package io.openliberty.elph.cmd;

import io.openliberty.elph.util.OS;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Consumer;

import static io.openliberty.elph.cmd.ElphCommand.TOOL_NAME;
import static io.openliberty.elph.util.OS.MAC;

@Command(name = "setup", description = "Review/configure the directories used by " + TOOL_NAME + ".")
class SetupCommand extends AbstractCommand implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

    static class NonInteractiveOptions {
        @Option(names = {"--ol-repo"}, paramLabel = "DIR", description = "the local open-liberty git repository")
        Path newRepo;
        @Option(names = {"--eclipse"}, paramLabel = "DIR", description = "the app / directory containing Eclipse")
        Path newEclipseHome;
        @Option(names = {"--workspace"}, paramLabel = "DIR", description = "the Eclipse workspace directory")
        Path newEclipseWorkspace;
    }

    static class InteractiveOptions {
        @Option(names = {"-i", "--interactive"}, description = "interactively configure all the settings")
        boolean interactive;
    }

    static class Args {
        @ArgGroup
        NonInteractiveOptions nonInteractiveOptions;
        @ArgGroup
        InteractiveOptions interactiveOptions;
    }

    @ArgGroup
    Args args;

    Path newRepo;
    Path newEclipseHome;
    Path newEclipseWorkspace;
    boolean interactive;

    @Override
    public void run() {
        if (null != args) {
            if (null != args.interactiveOptions) {
                this.interactive = args.interactiveOptions.interactive;
            }
            if (null != args.nonInteractiveOptions) {
                this.newRepo = args.nonInteractiveOptions.newRepo;
                this.newEclipseHome = args.nonInteractiveOptions.newEclipseHome;
                this.newEclipseWorkspace = args.nonInteractiveOptions.newEclipseWorkspace;
            }
        }
        elph.allowNullPaths();
        io.reportf("%n");
        updatePath(elph.getOpenLibertyRepo(), newRepo, "elph.ol-repo", "Open Liberty git repository", elph::setOpenLibertyRepo);
        io.reportf("%n");
        updatePath(elph.getEclipseHome(), newEclipseHome, "elph.eclipse", OS.is(MAC) ? "Eclipse home (Eclipse.app)": "Eclipse home directory", elph::setEclipseHome);
        io.reportf("%n");
        updatePath(elph.getEclipseWorkspace(), newEclipseWorkspace, "elph.workspace", "Eclipse workspace", elph::setEclipseWorkspace);
        io.reportf("%n");
    }

    private void updatePath(Path oldPath, Path newPath, String pathTypeName, String uiMsg, Consumer<Path> validator) {
        io.debugf("updatePath(%s, %s, \"%s\", \"%s\")%n", oldPath, newPath, pathTypeName, uiMsg);
        if (null == newPath && interactive) newPath = io.chooseDirectory(uiMsg, oldPath);
        else io.reportDirectory(uiMsg, oldPath, newPath);
        if (null != newPath) {
            save(pathTypeName, newPath);
            validator.accept(newPath);
        }
    }

    private void save(String name, Path dir) {
        Properties prefs = new Properties();
        try {
            if (PROPS_FILE.exists()) prefs.load(new FileReader(PROPS_FILE));
            dir = dir.toAbsolutePath();
            // normalize may simplify the path, but isn't guaranteed to be possible on all systems
            try {
                dir = dir.normalize();
            } catch (Throwable ignored) {}
            prefs.put(name, dir.toString());
            prefs.store(new FileWriter(PROPS_FILE), "Written programmatically using: " + spec.name() + " " + String.join(" ", spec.commandLine().getParseResult().originalArgs()));
        } catch (IOException e) {
            // TODO report error
            throw new RuntimeException(e);
        }
    }
}
