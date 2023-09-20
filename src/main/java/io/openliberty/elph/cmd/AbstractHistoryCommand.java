package io.openliberty.elph.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class AbstractHistoryCommand extends AbstractCommand {
    private static final String HIST_FILE = "import.hist";
    private static final String HIST_FILE_DESC = "import history file";
    protected boolean noHistory;

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
            throw io.error("Could not open the " + HIST_FILE_DESC + " for reading: " + historyFile);
        }
    }

    Set<Path> getProjectsFromHistory() {
        var imports = getHistoryList();
        var importsWithUsers = imports.stream()
                .filter(s -> s.startsWith("--users "))
                .map(s -> s.substring("--users ".length()));
        var importsWithoutUsers = imports.stream()
                .filter(not(s -> s.startsWith("--users ")));
        var projects = findProjects(importsWithUsers);
        addUsers(projects);
        projects.addAll(findProjects(importsWithoutUsers));
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

        normalize(patterns.stream())
                .peek(history::remove) // remove exact matches
                .map(s -> "--users " + s).forEach(history::remove); // remove matches with "--users"
        // compute the changes
        changes.removeAll(history);
        changes.forEach(h -> io.infof("Deleted: %s", h));
        rewriteHistory(history.stream());
        return changes.size() > 0;
    }

    private Path getHistoryFile() {
        String desc = HIST_FILE_DESC;
        Path path = elph.getWorkspaceSettingsDir().resolve(AbstractHistoryCommand.HIST_FILE);
        if (!Files.exists(path)) noHistory = true;
        return io.verifyOrCreateFile(desc, path);
    }

    private void rewriteHistory(Stream<String> newNarrative) {
        // write this out to file
        Path settingsFile = getHistoryFile();
        try (FileWriter fw = new FileWriter(settingsFile.toFile()); PrintWriter pw = new PrintWriter(fw)) {
            normalize(newNarrative).distinct().forEach(pw::println);
        } catch (IOException e) {
            throw io.error("Failed to open history for re-writing: " + settingsFile);
        }
    }
}
