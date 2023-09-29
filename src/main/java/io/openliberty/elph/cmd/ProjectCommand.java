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

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;

@Command(name = "project", hidden = true)
public class ProjectCommand extends AbstractCommand implements Runnable {
    @CommandLine.Parameters(paramLabel = "PATTERN", arity = "1..*", description = "projects to be imported")
    List<String> patterns;

    @Override
    public void run() {
        findProjects(patterns.stream()).stream().map(elph.getCatalog()::getProjectDetails).forEach(io::reportf);
    }
}
