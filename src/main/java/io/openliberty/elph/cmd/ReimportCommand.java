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

@Command(name = ReimportCommand.SUBCOMMAND_NAME, description = "Complete previous import actions, or re-interpret them after changes to OpenLiberty. ")
class ReimportCommand extends AbstractImportCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "reimport";

    @Override
    public void run() {
        var projects = getProjectsFromHistory();
        importDeps(projects);
    }
}
