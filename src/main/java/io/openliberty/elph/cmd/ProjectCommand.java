package io.openliberty.elph.cmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;

@Command(name = "project", hidden = true)
public class ProjectCommand extends AbstractCommand implements Runnable {
    @CommandLine.Parameters(paramLabel = "PATTERN", arity = "1..*", description = "projects to be imported")
    List<String> patterns;

    @Override
    public void run() {
        findProjects(patterns.stream()).stream().map(elph.getCatalog()::getProjectDetails).forEach(io::reportf);
    }
}
