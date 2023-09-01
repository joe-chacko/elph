package io.openliberty.elph;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

@Command(name = "import", description = "Automatically add project to Eclipse. " +
        "Please ensure both your terminal and eclipse applications are not full screen.")
public class Import implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

    @ParentCommand
    Main elph;
    @Spec
    CommandSpec spec;

    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    String pattern;

    @Override
    public void run() {
        elph.getCatalog().findProjects(pattern).forEach(this::importProject);
    }

    void importProject(Path path) {
        elph.verbose("Invoking eclipse to import %s", path);
        var args = elph.isMacOS() ?
                Stream.of("open", "-a", elph.eclipseHome, path, "--args", "-data", elph.eclipseWorkspace) :
                Stream.of(elph.eclipseHome.resolve("eclipse"), "-data", elph.eclipseWorkspace, path);
        var cmd = args
                .map(Object::toString)
                .collect(Collectors.toList());
        // invoke eclipse
        elph.runExternalCmd(cmd);
        // optionally click finish
        Optional.ofNullable(getFinishCommand()).filter(not(List::isEmpty)).ifPresent(elph::runExternalCmd);
    }


    private List<String> getFinishCommand() { return  null != elph.finishCommand ? elph.finishCommand : defaultFinishCommand(); }
    private List<String> defaultFinishCommand() { return elph.isMacOS() ? asList("osascript", "-e", "tell app \"System Events\" to tell process \"Eclipse\" to click button \"Finish\" of window 1") : null; }
}
