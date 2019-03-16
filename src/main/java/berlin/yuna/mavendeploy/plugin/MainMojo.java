package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.logic.Ci;
import berlin.yuna.mavendeploy.logic.GitService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

import static java.lang.String.format;

@Mojo(name = "run")
public class MainMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    @Parameter(property = "args", defaultValue = "")
    private List<String> args;

    public void execute() {
        final Log log = getLog();
        final GitService gitService = new GitService(basedir);
        final boolean gitStash = gitService.gitHasChanges();

        if (gitStash) {
            gitService.gitStash();
        }

        log.info("Preparing information");
        final Ci ci = new Ci(log, args.toArray(new String[0]));
        final String mavenCommand = ci.prepareMaven();
        final int status = new Terminal()
                .dir(basedir)
                .consumerInfo(log::info)
                .consumerError(log::error)
                .execute(mavenCommand)
                .status();

        if (gitService.gitHasChanges()) {
            gitService.commitAndPush(prepareCommitMessage(ci));
        }

        if (gitStash) {
            gitService.gitStashPop();
        }
        if (status != 0) {
            throw new RuntimeException(format("Status [%s]", status));
        }
    }

    private String prepareCommitMessage(final Ci ci) {
        String message = ci.getCommandLineReader().getValue("COMMIT");
        if (message == null || message.isEmpty()) {
            message = format("[%s] [%s] [%s]", ci.getProjectVersion(), ci.getBranchName(), "update");
        }
        return message;
    }

    public void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    public void setArgs(final List<String> args) {
        this.args = args;
    }
}
