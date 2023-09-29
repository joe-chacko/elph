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
import picocli.CommandLine.ParentCommand;

@Command(name = "analyze", description = "Force re-analysis of dependencies using bnd. Slow!")
public class AnalyzeCommand implements Runnable {
    @ParentCommand
    ElphCommand elph;
    @Override
    public void run() {
        elph.getCatalog().reanalyze();
    }
}
