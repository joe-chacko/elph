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

import static io.openliberty.elph.bnd.ProjectPaths.asNames;
import static java.util.function.Predicate.not;

import java.util.List;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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
