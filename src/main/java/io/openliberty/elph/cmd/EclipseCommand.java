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

import static io.openliberty.elph.cmd.ElphCommand.TOOL_NAME;

@Command(name = "eclipse", description = "Start Eclipse with the specified workspace. This is a good test of your " + TOOL_NAME + " configuration, but it does not replace other ways of launching Eclipse.")
class EclipseCommand extends AbstractCommand implements Runnable {
    @Override
    public void run() {
        elph.startEclipse();
    }
}
