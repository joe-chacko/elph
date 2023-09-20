package io.openliberty.elph.bnd;

import io.openliberty.elph.util.IO;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static picocli.CommandLine.Help.Ansi.Style.bold;
import static picocli.CommandLine.Help.Ansi.Style.faint;
import static picocli.CommandLine.Help.Ansi.Style.reset;

final class BndProject {
    final Path root;
    final String name;
    final String symbolicName;
    final List<String> initialDeps;
    final FileTime timestamp;
    final boolean isNoBundle;
    final boolean publishWlpJarDisabled;

    BndProject(Path root) {
        this.root = root;
        this.name = root.getFileName().toString();
        Properties props = getBndProps(root);
        this.symbolicName = Optional.of(props)
                .map(p -> p.getProperty(BUNDLE_SYMBOLICNAME))
                .map(val -> val.replaceFirst(";.*", ""))
                .map(String::trim)
                .orElse(null);
        List<String> deps = new ArrayList<>();
        deps.addAll(getPathProp(props, "-buildpath"));
        deps.addAll(getPathProp(props, "-testpath"));
        deps.remove("");
        this.isNoBundle = props.containsKey("-nobundles");
        this.publishWlpJarDisabled = "true".equals(props.getProperty("publish.wlp.jar.disabled"));
        this.initialDeps = unmodifiableList(deps);
        this.timestamp = IO.getLastModified(root.resolve("bnd.bnd"));
    }

    private static Properties getBndProps(Path root) {
        Path bndPath = root.resolve("bnd.bnd");
        Path bndOverridesPath = root.resolve("bnd.overrides");
        Properties bndProps = new Properties();
        try (var bndRdr = Files.newBufferedReader(bndPath)) {
            bndProps.load(bndRdr);
            if (Files.exists(bndOverridesPath)) {
                try (var overrideRdr = Files.newBufferedReader(bndOverridesPath)) {
                    bndProps.load(overrideRdr);
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
        return bndProps;
    }

    private static List<String> getPathProp(Properties props, String key) {
        String val = props.getProperty(key, "");
        return Stream.of(val.split(",\\s*"))
                .map(s -> s.replaceFirst(";.*", "")) // chop off qualifiers
                .map(s -> s.replaceFirst("\\.\\./([^/]+)/.*", "$1")) // parse relative dirs ../*/
                .toList();
    }

    boolean symbolicNameDiffersFromName() {
        return Objects.nonNull(symbolicName) && !Objects.equals(name, symbolicName);
    }

    @Override
    public String toString() { return name; }

    public String details() {
        return ("===%s%s%s===%n" +
                "%s         dir: %s%s%n" +
                "%ssymbolicName: %s%s%n" +
                "%s        deps: %s%s").formatted(
                bold.on(), name, reset.on(),
                faint.on(), faint.off(), root.getFileName(),
                faint.on(), faint.off(), symbolicName,
                faint.on(), faint.off(), join("%n              ", initialDeps));
    }
}
