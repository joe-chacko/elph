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

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = ForgetCommand.SUBCOMMAND_NAME, description = "Remove items from import history.")
class ForgetCommand extends AbstractHistoryCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "forget";

    static class Args {
        @Option(names = {"-a", "--all"}, required = true, description = "Remove import history completely.")
        boolean all;
        @Parameters(paramLabel = "Pattern to delete", description = "Remove pattern from import history")
        List<String> patterns;
    }

    @ArgGroup
    Args args;

    @Override
    public void run() {
        if (null == args) {
            var history = getHistoryList();
            if (history.isEmpty()) {
                io.report("No import history recorded.");
                return;
            }
            io.reportf("What would you like to forget? Here is the import history:");
            history.stream().map(s -> "  " + s).forEach(io::report);
        } else if (args.all) {
            deleteHistory();
        } else if (null != args.patterns) {
            if (deleteHistory(args.patterns)) return;
            throw io.error("Nothing deleted.");
        }
    }
}
