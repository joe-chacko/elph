package io.openliberty.elph.bnd;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * Outside this package, projects are referred to using their (absolute) path objects.
 * This is a collection of convenience functions for translation.
 */
public enum Projects {
    ;

    public static Stream<String> asNames(Collection<Path> projects) {
        return projects.stream().map(Projects::toName);
    }
    public static Set<String> toNames(Collection<Path> projects) { return asNames(projects).collect(toCollection(TreeSet::new)); }
    public static String toName(Path project) {
        return project.getFileName().toString();
    }
    public static String toMultilineString(Collection<Path> projects) { return projects.stream().map(Projects::toName).collect(joining("\n\t", "[\t", "\n]"));}
    public static String toInlineString(Collection<Path> projects) { return projects.stream().map(Projects::toName).collect(joining(" ", "[ ", " ]"));}
}
