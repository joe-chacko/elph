package io.openliberty.elph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static io.openliberty.elph.bnd.Projects.toInlineString;
import static java.util.function.Predicate.not;

class AbstractImportCommand extends AbstractCommand {
    protected boolean noHistory;

    void importProject(Path path) {
        io.infof("Importing %s", path);
        // invoke eclipse
        elph.runExternal(elph.getEclipseCmd(path));
        // optionally click finish
        if (noHistory) return;
        elph.pressFinish();
    }

    void importDepsInBatches(Set<Path> projects) { importDeps(projects, true); }
    void importDepsAllAtOnce(Set<Path> projects) { importDeps(projects, false); }

    private void importDeps(Set<Path> projects, boolean pauseBetweenBatches) {
        Set<Path> imported, deps, removed, leaves;
        deps = new TreeSet<>(projects);
        addDeps(deps);
        io.infof("%d related projects discovered.", deps.size());
        removed = removeImported(deps);
        io.infof("%d related projects already imported.", removed.size());
        if (deps.isEmpty()) {
            io.report("Nothing left to import.");
            return;
        }
        io.report("Projects to be imported: " + deps.size());
        int depCount = deps.size();
        while (depCount > 0) {
            leaves = removeLeaves(deps);
            if (deps.size() == depCount) {
                throw io.error("Project import seems to be stuck in a loop — Open Liberty or this tool needs fixing",
                        "leaves = " + toInlineString(leaves),
                        "deps = " + toInlineString(deps));
            }
            depCount = deps.size();
            io.reportf("Importing batch of %d projects: %d remaining", leaves.size(), deps.size());
            leaves.forEach(this::importProject);
            if (deps.isEmpty()) break;
            if (pauseBetweenBatches) io.pause();
        }
    }

    void addToHistory(List<String> patterns, boolean includeUsers) {
        rewriteHistory(Stream.concat(
                getHistoryList().stream(),
                patterns.stream().map(s -> includeUsers ? "--users " + s : s)));
    }

    List<String> getHistoryList() {
        Path historyFile = getHistoryFile();
        try {
            return Files.readAllLines(historyFile);
        } catch (IOException e) {
            throw io.error("Could not open the " + ImportCommand.SETTINGS_LIST_FILE_DESC + " for reading: " + historyFile);
        }
    }

    Set<Path> getProjectsFromHistory() {
        var imports = getHistoryList();
        var importsWithUsers = imports.stream()
                .filter(s -> s.startsWith("--users "))
                .map(s -> s.substring("--users ".length()));
        var importsWithoutUsers = imports.stream()
                .filter(not(s -> s.startsWith("--users ")));
        var projects = findProjects(importsWithUsers, true);
        projects.addAll(findProjects(importsWithoutUsers, false));
        return projects;
    }

    void deleteHistory() {
        File file = getHistoryFile().toFile();
        if (!file.exists()) {
            io.report("No history to delete.");
            return;
        }
        try {
            io.logf("Deleting %s", file);
            if (file.delete())
                io.report("History deleted.");
            else
                io.warn("Deletion failed for file: " + file);
        } catch (Exception e) {
            throw io.error("Could not delete file: " + file, e);
        }
    }

    boolean deleteHistory(List<String> patterns) {
        var history = getHistoryList();
        var changes = new ArrayList<>(history);
        // remove exact matches
        history.removeAll(patterns);
        // remove matches that match except for options (like "--users")
        patterns.stream().map(s -> "--users " + s).forEach(history::remove);
        // compute the changes
        changes.removeAll(history);
        changes.forEach(h -> io.infof("Deleted: %s", h));
        rewriteHistory(history.stream());
        return changes.size() > 0;
    }

    private Path getHistoryFile() {
        String desc = ImportCommand.SETTINGS_LIST_FILE_DESC;
        Path path = elph.getWorkspaceSettingsDir().resolve(ImportCommand.SETTINGS_LIST_FILE);
        if (!Files.exists(path)) noHistory = true;
        return io.verifyOrCreateFile(desc, path);
    }

    private void rewriteHistory(Stream<String> newNarrative) {
        // write this out to file
        Path settingsFile = getHistoryFile();
        try (FileWriter fw = new FileWriter(settingsFile.toFile()); PrintWriter pw = new PrintWriter(fw)) {
            newNarrative.distinct().forEach(pw::println);
        } catch (IOException e) {
            throw io.error("Failed to open history for re-writing: " + settingsFile);
        }
    }
}
