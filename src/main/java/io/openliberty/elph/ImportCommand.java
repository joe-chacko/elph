package io.openliberty.elph;

import io.openliberty.elph.io.IO;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.openliberty.elph.ElphCommand.TOOL_NAME;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Command(name = ImportCommand.SUBCOMMAND_NAME, description = "Add project and its dependencies to Eclipse. " +
        "It is recommended to have your terminal and your Eclipse window both visible at the same time. ")
public class ImportCommand implements Runnable {
    static final Path HOME_DIR = Paths.get(System.getProperty("user.home"));
    static final String SUBCOMMAND_NAME = "import";
    static final String SETTINGS_LIST_FILE = SUBCOMMAND_NAME + ".hist";
    static final String SETTINGS_LIST_FILE_DESC = TOOL_NAME + " " + SUBCOMMAND_NAME + " history file";

    @ParentCommand
    ElphCommand elph;
    @Spec
    CommandSpec spec;
    @Mixin
    IO io;
    private final List<String> projects = new ArrayList<>();
    private boolean noHistory;

    static class Args {
        @Option(names = {"-j", "--just"}, required = true, description = "Import just the matching projects. Do NOT import any dependencies.")
        boolean noDeps;
        @Option(names = {"-u", "--users"}, required = true, description = "Include users of the specified projects. Helps spot incompatible code changes.")
        boolean includeUsers;
    }

    @ArgGroup(exclusive = true)
    Args args = new Args();

    @Parameters(paramLabel = "PATTERN", arity = "1", description = "projects to be imported")
    List<String> patterns;

    @Override
    public void run() {
        if (args.noDeps) {
            patterns.stream()
                    .flatMap(elph.getCatalog()::findProjects)
                    .forEach(this::importProject);
        } else {
            saveImportToHistory();
            findProjects(this.patterns, args.includeUsers);
            importInBatches();
        }
    }

    private void importProject(Path path) {
        io.infof("Importing %s", path);
        // invoke eclipse
        elph.runExternal(elph.getEclipseCmd(path));
        // optionally click finish
        if (noHistory) return;
        elph.pressFinish();
    }

    void importInBatches() {
        int depCount = getDepCount(projects);
        if (depCount == 0) {
            io.report("Nothing left to import.");
            return;
        }
        io.report("Projects to be imported: " + depCount);
        var leaves = getNextBatch(projects);
        while (leaves.size() > 0) {
            io.reportf("Importing batch of projects: %d of %d remaining", leaves.size(), getDepCount(projects));
            leaves.forEach(this::importProject);
            leaves = getNextBatch(projects);
            io.pause();
        }
    }

    void initFromHistory() {
        var imports = getRawImportList();
        var importsWithUsers = imports.stream()
                .filter(s -> s.endsWith(" +users"))
                .map(s -> s.substring(0, s.length() - " +users".length()))
                .collect(toList());
        var importsWithoutUsers = imports.stream()
                .filter(not(s -> s.endsWith(" +users")))
                .collect(toList());
        // must findProjects with users first
        findProjects(importsWithUsers, true);
        findProjects(importsWithoutUsers, false);
    }

    private void findProjects(List<String> patterns, boolean includeUsers) {
        patterns.stream()
                .flatMap(elph.getCatalog()::findProjects)
                .map(Path::getFileName)
                .map(Object::toString)
                .forEach(projects::add);
        if (includeUsers) {
            elph.getCatalog().getDependentProjectPaths(projects)
                    .map(Path::getFileName)
                    .map(Object::toString)
                    .collect(toList())
                    .forEach(projects::add);
        }
    }

    private int getDepCount(List<String> projects) {
        return elph.getCatalog().getRequiredProjectPaths(projects)
                .map(Path::getFileName)
                .map(Object::toString)
                .filter(not(elph.getProjectsInEclipse()::contains))
                .collect(toList())
                .size();
    }

    private List<Path> getNextBatch(List<String> projects) {
        return elph.getCatalog().getLeafProjects(projects, elph.getProjectsInEclipse()).collect(toList());
    }

    private Path getHistoryFile() {
        String desc = SETTINGS_LIST_FILE_DESC;
        Path path = elph.getWorkspaceSettingsDir().resolve(SETTINGS_LIST_FILE);
        if (!Files.exists(path)) noHistory = true;
        return io.verifyOrCreateFile(desc, path);
    }

    private List<String> getRawImportList() {
        Path historyFile = getHistoryFile();
        try {
            return Files.readAllLines(historyFile);
        } catch (IOException e) {
            throw io.error("Could not open the " + SETTINGS_LIST_FILE_DESC + " for reading: " + historyFile);
        }
    }

    private void saveImportToHistory() {
        writeSettingsList(Stream.concat(
                getRawImportList().stream(),
                patterns.stream().map(s -> args.includeUsers ? s + " +users" : s)));
    }

    private void writeSettingsList(Stream<String> settingsList) {
        // write this out to file
        Path settingsFile = getHistoryFile();
        try (FileWriter fw = new FileWriter(settingsFile.toFile()); PrintWriter pw = new PrintWriter(fw)) {
            settingsList.distinct().forEach(pw::println);
        } catch (IOException e) {
            throw io.error("Failed to open settings list file for writing: " + settingsFile);
        }
    }
}
