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

import picocli.CommandLine.Command;

import java.util.TreeSet;

import static io.openliberty.elph.bnd.ProjectPaths.asNames;
import static io.openliberty.elph.util.IO.Verbosity.DEBUG;
import static io.openliberty.elph.util.IO.Verbosity.INFO;
import static io.openliberty.elph.util.IO.Verbosity.LOG;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

@Command(name = "check", description = "Check the Eclipse workspace for issues. Increase verbosity to see more detail.")
class CheckCommand extends AbstractHistoryCommand implements Runnable {
    private static final String GOOD = "\u2705 ";
    private static final String BAD = "\u2753 ";
    @Override
    public void run() {
        var bndProjects = elph.getBndProjects();
        var eclipseProjects = elph.getEclipseProjects();

        // 0. stats
        io.reportf("%sProjects in OpenLiberty git repository: %d", GOOD, bndProjects.size());
        if (io.isEnabled(DEBUG)) asNames(bndProjects).map(this::indent).forEach(io::debugf);
        io.reportf("%sProjects in Eclipse workspace: %d", GOOD, eclipseProjects.size());
        if (io.isEnabled(LOG)) asNames(eclipseProjects).map(this::indent).forEach(io::logf);

        // 1. ghost projects
        var ghostProjects = eclipseProjects.stream().filter(not(bndProjects::contains)).collect(toSet());
        io.reportf("%sPossible ghost projects (in workspace but not in git repository): %d", ghostProjects.isEmpty() ? GOOD: BAD, ghostProjects.size());
        asNames(ghostProjects).map(this::indent).forEach(io::reportf);

        // remove ghosts before continuing
        eclipseProjects.retainAll(bndProjects);

        // 2. workspace gaps
        var missingProjects = new TreeSet<>(eclipseProjects);
        addDeps(missingProjects);
        removeImported(missingProjects);
        io.reportf("%sGaps in Eclipse workspace: %d", missingProjects.isEmpty() ? GOOD: BAD, missingProjects.size());
        if (io.isEnabled(INFO)) asNames(missingProjects).map(this::indent).forEach(io::infof);

        // 2. import history
        var importPatterns = getHistoryList();
        if (importPatterns.isEmpty()) {
            // stop here if history is empty
            io.reportf( "No import history found.");
            return;
        }
        io.reportf("%sImport patterns: %d", GOOD, importPatterns.size());
        if (io.isEnabled(INFO)) importPatterns.stream().map(this::indent).forEach(io::infof);

        var importProjects = getProjectsFromHistory();
        io.reportf("%sProjects matching import patterns: %d", importProjects.isEmpty() ? BAD: GOOD, importProjects.size());
        if (io.isEnabled(INFO)) asNames(importProjects).map(this::indent).forEach(io::infof);

        // 3. remaining imports
        var remainingImports = new TreeSet<>(importProjects);
        addDeps(remainingImports);
        removeImported(remainingImports);
        io.reportf("%sRemaining projects to be imported: %d", remainingImports.isEmpty() ? GOOD: BAD, remainingImports.size());
        if (io.isEnabled(INFO)) asNames(remainingImports).map(this::indent).forEach(io::infof);

        // 4. unrelated projects - fix by removing or telling elph to import
        addDeps(importProjects);

        var unrelatedProjects = new TreeSet<>(eclipseProjects);
        unrelatedProjects.removeAll(importProjects);
        io.reportf("%sUnrelated projects (in workspace but not in import history): %d", unrelatedProjects.isEmpty() ? GOOD: BAD, unrelatedProjects.size());
        if (unrelatedProjects.isEmpty()) return;
        if (io.isEnabled(INFO)) asNames(unrelatedProjects).map(this::indent).forEach(io::infof);
    }

    String indent(String s) { return "  " + s; }
}
