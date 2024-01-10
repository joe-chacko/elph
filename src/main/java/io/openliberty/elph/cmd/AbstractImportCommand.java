/*
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package io.openliberty.elph.cmd;

import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static io.openliberty.elph.cmd.ElphCommand.TOOL_NAME;
import static io.openliberty.elph.util.IO.Verbosity.INFO;
import static java.util.stream.Collectors.toCollection;
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
        var queue = elph.getCatalog().inTopologicalOrder(deps.stream()).collect(toCollection(LinkedList::new));
        final int total = queue.size();
        var stack = new LinkedList<Path>();


        boolean firstInstructionsNeeded = noImportHistory();
        boolean instructionsNeeded = true;

        for (Path p = queue.poll(); null != p; p = queue.poll()) {
            stack.push(p);
            boolean batchComplete = stack.size() == maxBatchSize;
            boolean lastBatch = queue.isEmpty();
            // import the first project "cnf" on its own, with extra instructions
            // otherwise import in batches, allowing for an under-sized final batch
            if (!(firstInstructionsNeeded || batchComplete || lastBatch)) continue;
            if (firstInstructionsNeeded) {
                displayFirstDialogInstructions();
                firstInstructionsNeeded = false;
            } else if (instructionsNeeded) {
                displayGeneralInstructions();
                instructionsNeeded = false;
            }
            if (stack.size() == 1) io.reportf("Importing project %d of %d", total - queue.size(), total);
            else io.reportf("Importing projects %d..%d of %d", 1 + total - queue.size() - stack.size(), total - queue.size(), total);
            importProjects(stack);
            if (stack.isEmpty()) continue;
            // to ensure unimported projects still get imported, put them back at the head of the queue
            io.infof("Re-inserting %d project(s) at the head of the current import queue.", stack.size());
            while (null != (p = stack.poll())) queue.addFirst(p);
        }
    }

    private void displayGeneralInstructions() {
        io.banner(
                "Several import dialogs are about to open in Eclipse.",
                " - Deal with these in Eclipse as follows:",
                bold.on() + "   - EITHER import all the projects (hold down ENTER)",
                bold.on() + "   - OR import some/none of the projects and cancel the rest",
                " - Wait for Eclipse to finish building.",
                " - Ensure there are no errors in Eclipse's Markers view pane.",
                " - Finally, return to the " + TOOL_NAME + " window to continue.");
    }

    private void displayFirstDialogInstructions() {
        io.banner(
                "This looks like the first import into this Eclipse workspace.",
                "Just a single project will be imported.",
                "Follow these steps to configure the import dialog:",
                bold.on() + " [ ] Uncheck \"CLose newly imported projects upon deletion\"",
                bold.on() + " [ ] Uncheck \"Search for nested projects\"",
                bold.on() + " [ ] Uncheck \"Detect and configure project natures\"",
                bold.on() + " [*] Check \"Hide already open projects\"");
        io.pause();
    }

    private void importProjects(LinkedList<Path> stack) {
        elph.importProjects(stack);
        io.pause();
        int count = stack.size();
        boolean anyImportingHappened = stack.removeAll(elph.getEclipseProjects());
        if (stack.isEmpty()) {
            io.infof("Successfully imported %d project(s).", count);
        } else {
            // some or all of the projects were not imported
            // give the user a nudge to figure out what is going wrong
            if (anyImportingHappened) {
                boolean info = io.isEnabled(INFO);
                io.warn("Failed to import " + stack.size() + " project(s)" + (info ? ":" : "."));
                if (info) stack.stream().map(Objects::toString).map("\t"::concat).forEach(io::reportf);
            } else {
                io.warn("No projects were imported.");
            }
            io.banner("Please finish or cancel all the import dialogs before continuing.");
            io.pause();
            stack.removeAll(elph.getEclipseProjects()); // rule out any additional imports that have been processed
        }
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
