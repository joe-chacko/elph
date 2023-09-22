package io.openliberty.elph.cmd;

import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static picocli.CommandLine.Help.Ansi.Style.bold;
import static picocli.CommandLine.Help.Ansi.Style.reset;

class AbstractImportCommand extends AbstractHistoryCommand {

    private int maxBatchSize = Integer.MAX_VALUE;

    @Option(names = {"-b", "--batch-size"}, defaultValue = "20", description = "Limit the maximum number of eclipse projects to import in a single batch.")
    private void setMaxBatchSize(int val) {
        if (val <= 0) throw new TypeConversionException("Cannot set batch size lower than 1");
        if (val > 100) io.warn("Ambitious batch size setting: " + val);
        maxBatchSize = val;
    }

    void importProject(Path path) {
        io.infof("Importing %s", path);
        elph.importProjects(List.of(path));
        if (path.getFileName().toString().equals("cnf")) {
            io.inputf("Ensure you have unchecked the following checkboxes:%n" +
                            "  \u2022 %1$sSearch for nested projects%2$s%n" +
                            "  \u2022 %1$sDetect and configure project natures%2$s%n" +
                            "Press return to continue.",
                    bold.on(), reset.on());
        }
    }

    void importDeps(Set<Path> projects) {
        Set<Path> deps = new TreeSet<>(projects);
        addDeps(deps);
        io.infof("%d related projects discovered.", deps.size());
        Set<Path> removed = removeImported(deps);
        io.infof("%d related projects already imported.", removed.size());
        if (deps.isEmpty()) {
            io.report("Nothing left to import.");
            return;
        }
        io.report("Projects to be imported: " + deps.size());
        int depCount = deps.size();


        // if the list to be imported includes "cnf" then import that first to allow dialog configuration
        deps.stream()
                .filter(p -> "cnf".equals(p.getFileName().toString()))
                .findFirst() // this terminal operation completes the stream's iteration through deps
                .ifPresent(cnf -> {
                    importProject(cnf);
                    deps.remove(cnf); // now the stream is done with, it is safe to remove from the set
                });
        var stack = new LinkedList<Path>();
        elph.getCatalog().inTopologicalOrder(deps.stream()).forEach(p -> {
            if (stack.size() == maxBatchSize) {
                stack.clear(); // we've just processed these, so clear them out
                io.pause(); // there is at least one more project to import, so pause
            }
            stack.push(p);
            if (stack.size() < maxBatchSize) return;
            elph.importProjects(stack);
        });
        // clear up any remainder
        if (stack.size() != maxBatchSize) elph.importProjects(stack);
    }
}
