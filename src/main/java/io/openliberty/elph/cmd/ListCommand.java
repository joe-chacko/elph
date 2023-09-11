package io.openliberty.elph.cmd;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.openliberty.elph.bnd.ProjectPaths.asNames;
import static java.util.function.Predicate.not;

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
        var projects = findProjects(patterns.stream());
        if (showUsers) addUsers(projects);
        if (showDeps) addDeps(projects);
        var names = asNames(projects);
        if (hiding.imported) names = names.filter(not(elph.getEclipseProjectNames()::contains));
        if (hiding.unimported) names = names.filter(elph.getEclipseProjectNames()::contains);
        names.sorted().forEach(io::report);
    }
}
