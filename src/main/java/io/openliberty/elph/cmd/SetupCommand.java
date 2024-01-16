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

import static io.openliberty.elph.cmd.ElphCommand.TOOL_NAME;
import static io.openliberty.elph.util.OS.MAC;
import static io.openliberty.elph.util.Objects.stringEquals;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Consumer;

import io.openliberty.elph.util.OS;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "setup", description = "Review/configure the directories used by " + TOOL_NAME + ".")
class SetupCommand extends AbstractCommand implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final File PROPS_FILE = HOME_DIR.resolve(".elph.properties").toFile();

    static class NonInteractiveOptions {
        @Option(names = {"--ol-repo"}, paramLabel = "DIR", description = "the local open-liberty git repository")
        Path newRepo;
        @Option(names = {"--eclipse"}, paramLabel = "DIR", description = "the app / directory containing Eclipse")
        Path newEclipseHome;
        @Option(names = {"--workspace"}, paramLabel = "DIR", description = "the Eclipse workspace directory")
        Path newEclipseWorkspace;
    }

    static class InteractiveOptions {
        @Option(names = {"-i", "--interactive"}, description = "interactively configure all the settings")
        boolean enabled;
    }

    static class Args {
        @ArgGroup
        NonInteractiveOptions params = new NonInteractiveOptions();
        @ArgGroup
        InteractiveOptions interactive = new InteractiveOptions();
    }

    @ArgGroup
    Args args = new Args();

    private final Properties prefs;
    private boolean updated;

    SetupCommand() {
        prefs = new Properties();
        try {
            if (PROPS_FILE.exists()) prefs.load(new FileReader(PROPS_FILE));
        } catch (Throwable ignored) {}
    }


    @Override
    public void run() {
        elph.allowNullPaths();
        io.reportf("%n");
        updatePath(elph.getOpenLibertyRepo(), args.params.newRepo, "elph.ol-repo", "Open Liberty git repository", elph::setOpenLibertyRepo);
        io.reportf("%n");
        updatePath(elph.getEclipseHome(), args.params.newEclipseHome, "elph.eclipse", OS.is(MAC) ? "Eclipse home (Eclipse.app)": "Eclipse home directory", elph::setEclipseHome);
        io.reportf("%n");
        updatePath(elph.getEclipseWorkspace(), args.params.newEclipseWorkspace, "elph.workspace", "Eclipse workspace", elph::setEclipseWorkspace);
        io.reportf("%n");

        if (updated) {
            try {
                prefs.store(new FileWriter(PROPS_FILE), "Written programmatically using: " + spec.root().name() + " " + String.join(" ", spec.commandLine().getParseResult().originalArgs()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updatePath(Path elphPath, Path newPath, String pathTypeName, String uiMsg, Consumer<Path> validator) {
        Object oldPath = prefs.get(pathTypeName);
        // if no path was provided directly, use the path from elph if it differs from the entry in prefs
        if (newPath == null && !stringEquals(oldPath, elphPath)) newPath = elphPath;
        io.debugf("updatePath(%s, %s, \"%s\", \"%s\")%n", elphPath, newPath, pathTypeName, uiMsg);
        if (null == newPath && args.interactive.enabled) newPath = io.chooseDirectory(uiMsg, oldPath);
        else io.reportDirectory(uiMsg, oldPath, newPath);
        if (null != newPath) {
            save(pathTypeName, newPath);
            validator.accept(newPath);
        }
    }

    private void save(String name, Path dir) {
        dir = dir.toAbsolutePath().normalize();
        Object oldVal = prefs.put(name, dir.toString());
        if (stringEquals(dir, oldVal)) return;
        io.logf("Updating '%s' property%n\told val: '%s'%n\tnew val: '%s'", name, oldVal, dir);
        updated = true;
    }
}
