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
package io.openliberty.elph.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public enum OS {
    LINUX("eclipse"),
    MAC("Contents/MacOS/eclipse") {
        Stream<Object> getArgStream(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
            var exec = eclipseHome;
            return projects.length == 0 ?
                    Stream.of("open", "-a", exec, "--args", "-data", eclipseWorkspace) :
                    Stream.concat(Stream.of("open", "-a", exec), Stream.of(projects));
        }
    },
    WINDOWS("eclipse.exe");
    public final Path pathToExecutable;
    OS(String exec) {this.pathToExecutable = Paths.get(exec);}
    public static OS current() {return valueOf(System.getProperty("os.name").toUpperCase().split(" ")[0]);}
    public static boolean is(OS os) { return current() == os; }
    public List<String> getEclipseCmd(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
        return getArgStream(eclipseHome, eclipseWorkspace, projects)
                .map(Objects::toString)
                .toList();
    }
    Stream<Object> getArgStream(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
        var exec = eclipseHome.resolve(pathToExecutable);
        return projects.length == 0 ?
                Stream.of(exec, "-data", eclipseWorkspace) :
                Stream.concat(Stream.of(exec), Stream.of(projects));
    }
}
