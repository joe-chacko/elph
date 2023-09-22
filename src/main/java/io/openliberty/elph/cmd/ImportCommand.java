package io.openliberty.elph.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

import static picocli.CommandLine.Help.Ansi.Style.bold;
import static picocli.CommandLine.Help.Ansi.Style.italic;
import static picocli.CommandLine.Help.Ansi.Style.reset;

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

    private void eclipseImportCheckboxCheck() {
        System.out.println( bold.on() + "\nPlease press return after reading the following" + bold.off() + reset.on());

        System.out.println("\nEnsure the following boxes are" + bold.on() + " UNTICKED \u274C" + bold.off() + reset.on());
        System.out.println(italic.on() + "\t - Close newly imported projects upon deletion");
        System.out.println("\t - Search for nested projects");
        System.out.println("\t - Detect and configure project natures" + italic.off());

        System.out.println("\nEnsure the following boxes are" + bold.on() + " TICKED \u2705" + bold.off() + reset.on());
        System.out.println(italic.on() + "\t - Hide already open projects" + italic.off());
    }

}
