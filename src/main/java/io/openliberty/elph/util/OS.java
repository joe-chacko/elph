package io.openliberty.elph.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum OS {
    LINUX("eclipse") {
        Stream<Object> getArgStream(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
            var exec = eclipseHome.resolve("eclipse");
            return projects.length == 0 ?
                    Stream.of(exec, "-data", eclipseWorkspace) :
                    Stream.concat(Stream.of(exec), Stream.of(projects));
        }
    },
    MAC("Contents/MacOS/eclipse") {
        Stream<Object> getArgStream(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
            var exec = eclipseHome;
            return projects.length == 0 ?
                    Stream.of("open", "-a", exec, "--args", "-data", eclipseWorkspace) :
                    Stream.concat(Stream.of("open", "-a", exec), Stream.of(projects));
        }
    },
    WINDOWS("eclipsec.exe") {
        Stream<Object> getArgStream(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
            throw new UnsupportedOperationException("Please contact the maintainer to add support for your OS");
        }
    };
    public final Path pathToExecutable;
    OS(String exec) {this.pathToExecutable = Paths.get(exec);}
    public static OS current() {return valueOf(System.getProperty("os.name").toUpperCase().split(" ")[0]);}
    public static boolean is(OS os) { return current() == os; }
    abstract Stream<Object> getArgStream(Path eclipseHome, Path eclipseWorkspace, Path...projects);
    public List<String> getEclipseCmd(Path eclipseHome, Path eclipseWorkspace, Path... projects) {
        return getArgStream(eclipseHome, eclipseWorkspace, projects)
                .map(Objects::toString)
                .toList();
    }
}
