package io.openliberty.elph.cmd;

import io.openliberty.elph.bnd.ProjectPaths;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static io.openliberty.elph.bnd.ProjectPaths.asNames;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

@Command(name = "list", description = "List projects matching specified patterns.")
class ListCommand extends AbstractCommand implements Runnable {
    @Option(names = {"-d", "--show-deps"}, description = "Show all dependencies of matching projects")
    boolean showDeps;
    @Option(names = {"-u", "--show-users"}, description = "Show all users of matching projects")
    boolean showUsers;

    static class Hiding {
        @Option(names = {"-h", "--hide-imported"}, description = "Hide already imported projects")
        boolean imported;
        @Option(names = {"-H", "--hide-unimported"}, description = "Hide unimported projects")
        boolean unimported;
    }
    @ArgGroup
    final Hiding hiding = new Hiding();

    @Parameters(paramLabel = "PATTERN", arity = "1..*", description = "projects to be imported")
    List<String> patterns;

    @Override
    public void run() {
        List<String> patterns = this.patterns;
        Set<Path> projects = patterns.stream()
                .flatMap(elph.getCatalog()::findProjects)
                .collect(toCollection(TreeSet::new));
        if (showUsers) addUsers(projects);
        if (showDeps) addDeps(projects);

        var names = asNames(projects);
        if (hiding.imported) names = names.filter(not(elph.getEclipseProjectNames()::contains));
        if (hiding.unimported) names = names.filter(elph.getEclipseProjectNames()::contains);
        names.sorted().forEach(this::displayProject);
    }

    private void displayProject(String name) {
        io.report(name);
    }
}
