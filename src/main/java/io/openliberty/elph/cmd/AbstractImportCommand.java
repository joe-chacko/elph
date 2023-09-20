package io.openliberty.elph.cmd;

import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static io.openliberty.elph.bnd.ProjectPaths.toInlineString;
import static picocli.CommandLine.Help.Ansi.Style.bold;
import static picocli.CommandLine.Help.Ansi.Style.reset;

class AbstractImportCommand extends AbstractHistoryCommand {

    @Option(names = {"-b", "--batch"}, description = "Import in batches.")
    boolean batching;

    private int maxBatchSize = Integer.MAX_VALUE;
    @Option(names = {"-m", "--max-batch-size"}, description = "Limit the maximum number of eclipse projects to import in a single batch.")
    private void setMaxBatchSize(int val) {
        if (val <= 0) throw new TypeConversionException("Cannot set batch size lower than 1");
        maxBatchSize = val;
    }

    void importProject(Path path) {
        io.infof("Importing %s", path);
        // invoke eclipse
        elph.runExternal(elph.getEclipseCmd(path));
        if (path.getFileName().toString().equals("cnf")) {
            io.inputf("Ensure you have unchecked the following checkboxes:%n" +
                            "  \u2022 %1$sSearch for nested projects%2$s%n" +
                            "  \u2022 %1$sDetect and configure project natures%2$s%n" +
                            "Press return to continue.",
                    bold.on(), reset.on());
        }
    }

    void importDeps(Set<Path> projects) {
        Set<Path> deps, removed, leaves;
        deps = new TreeSet<>(projects);
        addDeps(deps);
        io.infof("%d related projects discovered.", deps.size());
        removed = removeImported(deps);
        io.infof("%d related projects already imported.", removed.size());
        if (deps.isEmpty()) {
            io.report("Nothing left to import.");
            return;
        }
        io.report("Projects to be imported: " + deps.size());
        int depCount = deps.size();
        if (batching) {
            while (depCount > 0) {
                leaves = removeLeaves(deps, maxBatchSize);
                if (deps.size() == depCount) {
                    throw io.error("Project import seems to be stuck in a loop - Open Liberty or this tool needs fixing",
                            "leaves = " + toInlineString(leaves),
                            "deps = " + toInlineString(deps));
                }
                depCount = deps.size();
                io.reportf("Importing batch of %d projects: %d remaining", leaves.size(), deps.size());
                // Import one project at a time if this tool is going to click finish
                if (false) leaves.forEach(this::importProject);
                    // Otherwise open all the projects in as few commands as possible
                else elph.getBunchedEclipseCmds(leaves).forEach(elph::runExternal);
                if (deps.isEmpty()) break;
                io.pause();
            }
        } else {
            // if the list to be imported includes "cnf" then import that first to allow dialog configuration
            deps.stream()
                    .filter(p -> "cnf".equals(p.getFileName().toString()))
                    .findFirst() // this terminal operation completes the stream's iteration through deps
                    .ifPresent(cnf -> {
                        importProject(cnf);
                        deps.remove(cnf); // now the stream is done with, it is safe to remove from the set
                    });

            // invert the order of imports so that the very last dialog is the first dependency
            var reversed = elph.getCatalog()
                    .reverseDependencyOrder(deps.stream())
                    .toList();
            elph.getBunchedEclipseCmds(reversed).forEach(elph::runExternal);
        }
    }
}
