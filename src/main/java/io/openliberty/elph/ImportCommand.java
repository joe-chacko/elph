package io.openliberty.elph;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.openliberty.elph.ElphCommand.TOOL_NAME;

@Command(name = ImportCommand.SUBCOMMAND_NAME, description = "Add project and its dependencies to Eclipse. " +
        "It is recommended to have your terminal and your Eclipse window both visible at the same time. ")
public class ImportCommand extends AbstractImportCommand implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final String SUBCOMMAND_NAME = "import";
    static final String SETTINGS_LIST_FILE = SUBCOMMAND_NAME + ".hist";
    static final String SETTINGS_LIST_FILE_DESC = TOOL_NAME + " " + SUBCOMMAND_NAME + " history file";

    static class Args {
        @Option(names = {"-j", "--just"}, required = true, description = "Import just the matching projects. Do NOT import any dependencies.")
        boolean noDeps;
        @Option(names = {"-u", "--users"}, required = true, description = "Include users of the specified projects. Helps spot incompatible code changes.")
        boolean includeUsers;
        @Option(names = {"-f", "--force"}, required = true, description = "Don't pause between batches. Use with caution.")
        boolean force;
    }

    @ArgGroup
    final Args args = new Args();

    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    List<String> patterns;


    @Override
    public void run() {
        if (args.noDeps) {
            patterns.stream()
                    .flatMap(elph.getCatalog()::findProjects)
                    .forEach(this::importProject);
        } else {
            addToHistory(patterns, args.includeUsers);
            var projects = findProjects(patterns.stream(), args.includeUsers);
            if (args.force) importDepsAllAtOnce(projects);
            else importDepsInBatches(projects);
        }
    }
}
