package io.openliberty.elph.cmd;

import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import static picocli.CommandLine.Help.Ansi.Style.bold;
import static picocli.CommandLine.Help.Ansi.Style.italic;
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
            elph.getBunchedEclipseCmds(stack).forEach(elph::runExternal);
        });

        // clear up any remainder
        if (stack.size() != maxBatchSize) elph.getBunchedEclipseCmds(stack).forEach(elph::runExternal);
    }

     void eclipseImportCheckboxCheck() {
        System.out.println( bold.on() +
                "\nIMPORTANT - Please perform the following instructions within the import window that will appear in " +
                "Eclipse after you press the 'return' key in this command line shell." +
                bold.off() + reset.on());

        System.out.println("\nEnsure the following boxes are" + bold.on() + " UNTICKED \u274C" + bold.off() + reset.on());
        System.out.println(italic.on() + "\t - Close newly imported projects upon deletion");
        System.out.println("\t - Search for nested projects");
        System.out.println("\t - Detect and configure project natures" + italic.off());

        System.out.println("\nEnsure the following boxes are" + bold.on() + " TICKED \u2705" + bold.off() + reset.on());
        System.out.println(italic.on() + "\t - Hide already open projects" + italic.off());

        io.inputf("\nPress return to continue\n");
    }
}
