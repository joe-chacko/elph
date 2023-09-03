package io.openliberty.elph;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.stream.Collectors.joining;

@Command(name = "import", description = "Automatically add project to Eclipse. " +
        "Please ensure both your terminal and eclipse applications are not full screen.")
public class ImportCommand implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

    @ParentCommand
    ElphCommand elph;
    @Spec
    CommandSpec spec;

    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    String pattern;

    @Override
    public void run() { elph.getCatalog().findProjects(pattern).forEach(elph::importProject); }
}
