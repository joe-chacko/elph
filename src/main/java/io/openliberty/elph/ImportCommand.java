package io.openliberty.elph;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
    @Mixin
    IO io;

    static class Args {
        @Option(names = {"-j", "--just"}, required = true, description = "Import just the matching projects. Do NOT import any dependencies.")
        boolean noDeps;
        @Option(names = {"-u", "--users"}, required = true, description = "Import the matching projects and all the projects that use them.")
        boolean includeUsers;
    }

    @ArgGroup(exclusive = true)
    Args args = new Args();

    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    List<String> patterns;


    @Override
    public void run() {
        if (args.noDeps) {
            patterns.stream().flatMap(elph.getCatalog()::findProjects).forEach(elph::importProject);
        } else {
            throw io.error("Not implemented yet.");
        }
    }
}
