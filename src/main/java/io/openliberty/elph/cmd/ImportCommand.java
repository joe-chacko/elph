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
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = ImportCommand.SUBCOMMAND_NAME, description = "Add project and its dependencies to Eclipse. ")
class ImportCommand extends AbstractImportCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "import";

    @Option(names = {"-u", "--users"}, description = "Include users of the specified projects. Helps spot incompatible code changes.")
    boolean includeUsers;
    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    List<String> patterns;

    @Override
    public void run() {
        addToHistory(patterns, includeUsers);
        var projects = findProjects(patterns.stream(), includeUsers);
        eclipseImportCheckboxCheck();
        importDeps(projects);
    }



}
