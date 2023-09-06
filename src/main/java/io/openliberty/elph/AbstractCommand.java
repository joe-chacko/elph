package io.openliberty.elph;

import io.openliberty.elph.io.IO;
import picocli.CommandLine;

abstract class AbstractCommand {
    @CommandLine.ParentCommand
    ElphCommand elph;
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    IO io;
}
