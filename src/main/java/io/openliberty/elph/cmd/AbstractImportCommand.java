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

    void importDepsInBatches(Set<Path> projects) { importDeps(projects, true); }
    void importDepsAllAtOnce(Set<Path> projects) { importDeps(projects, false); }

    private void importDeps(Set<Path> projects, boolean pauseBetweenBatches) {
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
            if (pauseBetweenBatches) io.pause();
        }
    }
}
