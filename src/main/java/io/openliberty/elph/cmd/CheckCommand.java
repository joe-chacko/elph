package io.openliberty.elph.cmd;

import io.openliberty.elph.bnd.ProjectPaths;
import io.openliberty.elph.util.IO;
import picocli.CommandLine.Command;

import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.openliberty.elph.bnd.ProjectPaths.asNames;
import static io.openliberty.elph.bnd.ProjectPaths.toNames;
import static io.openliberty.elph.util.IO.Verbosity.DEBUG;
import static io.openliberty.elph.util.IO.Verbosity.INFO;
import static io.openliberty.elph.util.IO.Verbosity.LOG;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

@Command(name = "check", description = "Check the Eclipse workspace for issues. Increase verbosity to see more detail.")
class CheckCommand extends AbstractImportCommand implements Runnable {
    @Override
    public void run() {
        var bndProjects = elph.getBndProjects();
        var eclipseProjects = elph.getEclipseProjects();

        // 0. stats
        io.reportf("Projects in OpenLiberty git repository: %d", bndProjects.size());
        if (io.isEnabled(DEBUG)) asNames(bndProjects).map(this::indent).forEach(io::debugf);
        io.reportf("Projects in Eclipse workspace: %d", eclipseProjects.size());
        if (io.isEnabled(LOG)) asNames(eclipseProjects).map(this::indent).forEach(io::logf);

        // 1. ghost projects
        var ghostProjects = eclipseProjects.stream().filter(not(bndProjects::contains)).collect(toSet());
        io.reportf("Possible ghost projects (in workspace but not in git repository): %d", ghostProjects.size());
        asNames(ghostProjects).map(this::indent).forEach(io::reportf);

        // remove ghosts before continuing
        eclipseProjects.retainAll(bndProjects);

        // 2. workspace gaps
        var missingProjects = new TreeSet<>(eclipseProjects);
        addDeps(missingProjects);
        removeImported(missingProjects);
        io.reportf("Gaps in Eclipse workspace: %d", missingProjects.size());
        if (io.isEnabled(INFO)) asNames(missingProjects).map(this::indent).forEach(io::infof);

        // 2. import history
        var importPatterns = getHistoryList();
        if (importPatterns.isEmpty()) {
            // stop here if history is empty
            io.reportf("No import history found.");
            return;
        }
        io.reportf("Import patterns: %d", importPatterns.size());
        if (io.isEnabled(INFO)) importPatterns.stream().map(this::indent).forEach(io::infof);

        var importProjects = getProjectsFromHistory();
        io.reportf("Projects matching import patterns: %d", importProjects.size());
        if (io.isEnabled(INFO)) asNames(importProjects).map(this::indent).forEach(io::infof);

        // 3. remaining imports
        var remainingImports = new TreeSet<>(importProjects);
        addDeps(remainingImports);
        removeImported(remainingImports);
        io.reportf("Remaining projects to be imported: %d", remainingImports.size());
        if (io.isEnabled(INFO)) asNames(remainingImports).map(this::indent).forEach(io::infof);

        // 4. unrelated projects - fix by removing or telling elph to import
        addDeps(importProjects);

        var unrelatedProjects = new TreeSet<>(eclipseProjects);
        unrelatedProjects.removeAll(importProjects);
        io.reportf("Unrelated projects (in workspace but not in import history): %d", unrelatedProjects.size());
        if (unrelatedProjects.isEmpty()) return;
        if (io.isEnabled(INFO)) asNames(unrelatedProjects).map(this::indent).forEach(io::infof);
    }

    String indent(String s) { return "  " + s; }
}
