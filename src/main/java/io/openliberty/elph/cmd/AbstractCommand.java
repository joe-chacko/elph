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

import io.openliberty.elph.util.IO;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static io.openliberty.elph.bnd.ProjectPaths.toNames;
import static java.util.stream.Collectors.toCollection;

abstract class AbstractCommand {
    @CommandLine.ParentCommand
    ElphCommand elph;
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    IO io;

    Stream<String> normalize(Stream<String> patterns) {
        return patterns.map(s -> s.replace('%', '*'));
    }

    Set<Path> findProjects(Stream<String> patterns, boolean includeUsers) {
        Set<Path> result = findProjects(patterns);
        if (includeUsers) addUsers(result);
        return result;
    }

    Set<Path> findProjects(Stream<String> patterns) {
        return elph.getCatalog().findProjects(normalize(patterns)).collect(toCollection(TreeSet::new));
    }

    void addDeps(Collection<Path> projects) {
        elph.getCatalog().getRequiredProjectPaths(toNames(projects)).forEach(projects::add);
    }

    void addUsers(Collection<Path> projects) {
        var names = toNames(projects);
        elph.getCatalog().getDependentProjectPaths(names).forEach(projects::add);
    }

    /**
     * Removes (and returns) the projects already imported to Eclipse from the supplied collection.
     * @return the set of projects that were removed
     */
    Set<Path> removeImported(Collection<Path> projects) {
        var imported = elph.getEclipseProjects();
        var removed = projects.stream().filter(imported::contains).collect(toCollection(TreeSet::new));
        projects.removeAll(removed);
        return removed;
    }

    /**
     * Removes (and returns) the subset of projects that do not depend on any other projects in the supplied collection.
     * @param max the maximum number of leaf projects to remove
     * @return the set of projects that were removed
     */
    Set<Path> removeLeaves(Collection<Path> projects, int max) {
        assert max > 0;
        var leaves = elph.getCatalog().getLeavesOfSubset(projects, max);
        projects.removeAll(leaves);
        return leaves;
    }
}
