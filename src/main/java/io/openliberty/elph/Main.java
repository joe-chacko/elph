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
package io.openliberty.elph;

import io.openliberty.elph.cmd.ElphCommand;
import picocli.CommandLine;

public enum Main {
    ;
    public static void main(String... args) {
        System.exit(new CommandLine(new ElphCommand()).setAbbreviatedSubcommandsAllowed(true).execute(args));
    }
}
