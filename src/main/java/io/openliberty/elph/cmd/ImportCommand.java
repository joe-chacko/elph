package io.openliberty.elph.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = ImportCommand.SUBCOMMAND_NAME, description = "Add project and its dependencies to Eclipse. ")
class ImportCommand extends AbstractImportCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "import";

    @Option(names = {"-u", "--users"}, description = "Include users of the specified projects. Helps spot incompatible code changes.")
    boolean includeUsers;
    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    List<String> patterns;

    @Override
    public void run() {
        addToHistory(patterns, includeUsers);
        var projects = findProjects(patterns.stream(), includeUsers);
        eclipseImportCheckboxCheck();
        importDeps(projects);
    }



}
