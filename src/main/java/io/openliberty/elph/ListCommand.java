package io.openliberty.elph;

import io.openliberty.elph.io.IO;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Command(name = "list", description = "List projects matching specified patterns.")
public class ListCommand implements Runnable {
    @ParentCommand
    ElphCommand elph;
    @Spec
    CommandSpec spec;
    @Mixin
    IO io;
    @Option(names = {"-d", "--show-deps"}, description = "Show all dependencies of matching projects")
    boolean showDeps;
    @Option(names = {"-u", "--show-users"}, description = "Show all users of matching projects")
    boolean showUsers;
    @Option(names = {"-h", "--hide-imported"}, description = "Hide already imported projects")
    boolean hideImported;

    @Parameters(paramLabel = "PATTERN", arity = "1..*", description = "projects to be imported")
    List<String> pattern;

    @Override
    public void run() {
        var projects = pattern.stream()
                .flatMap(elph.getCatalog()::findProjects)
                .map(Path::getFileName)
                .distinct();
        if (showUsers) {
            var paths = projects.collect(toList());
            var names = paths.stream().map(Path::getFileName).map(Path::toString).collect(toList());
            var users = elph.getCatalog().getDependentProjectPaths(names);
            projects = Stream.concat(paths.stream(), users).distinct();
        }
        if (showDeps) {
            var names = projects.map(Path::getFileName).map(Path::toString).collect(toList());
            projects = elph.getCatalog().getRequiredProjectPaths(names);
        }
        if (hideImported) {
            projects = projects.filter(elph.getProjectsInEclipse()::contains);
        }
        projects.sorted().forEach(this::displayProject);
    }

    private void displayProject(Path path) {
        io.report(path.getFileName());
    }
}
