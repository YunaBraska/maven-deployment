package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_CLEAN;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_CLEAN_CACHE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_FAILSAFE_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_GPG_SIGN_ALT_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_GPG_SIGN_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_JAVADOC;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_REPORT;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SKIP_TEST;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SOURCE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SURFIRE_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_TAG_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_UPDATE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_VERSION_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.FILE_MVN_FAILSAFE;
import static berlin.yuna.mavendeploy.config.MavenCommands.FILE_MVN_SURFIRE;
import static berlin.yuna.mavendeploy.config.MavenCommands.MVN_DEPLOY_LAYOUT;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_PLUGIN;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_STAGING_URL;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_URL;
import static berlin.yuna.mavendeploy.config.MavenCommands.XX_CMD_MVN_SNAPSHOT;
import static berlin.yuna.mavendeploy.config.MavenCommands.XX_CMD_MVN_VERSION;
import static java.lang.String.format;

public class Ci {

    private File PROJECT_DIR = new File(System.getProperty("user.dir"));
    final CommandLineReader clr;
    final Model pom;
    final SemanticService semanticService;
    final GitService gitService;

    private String JAVA_VERSION = null;
    private String ENCODING = null;
    private String GPG_PASS = null;
    private String GPG_PASS_ALT = null;
    private String PROJECT_VERSION = null;
    private String MVN_OPTIONS = "";

    private final boolean IS_POM;
    private boolean MVN_CLEAN = false;
    private boolean MVN_CLEAN_CACHE = false;
    private boolean MVN_SKIP_TEST = true;
    private boolean MVN_UPDATE = false;
    private boolean MVN_JAVA_DOC = true;
    private boolean MVN_PROFILES = true;
    private boolean MVN_SOURCE = true;
    private boolean MVN_TAG = false;
    private boolean MVN_TAG_BREAK = false;
    private boolean MVN_REPORT = false;
    private boolean MVN_RELEASE = false;
    private boolean MVN_REMOVE_SNAPSHOT = false;

    private String MVN_DEPLOY_ID = null;
    private String SEMANTIC_FORMAT = null;
//    private String SEMANTIC_FORMAT = "\\.::release::feature::bugfix|hotfix::custom";

    private final Log LOG;

    public Ci(final Log LOG, final String... args) {
        this.LOG = LOG;
        clr = new CommandLineReader(args == null ? new String[]{""} : args);
        //Project
        PROJECT_DIR = getOrElse(clr.getValue("PROJECT_DIR"), PROJECT_DIR);
        ENCODING = getString(clr, "ENCODING", ENCODING);
        PROJECT_VERSION = getString(clr, "PROJECT_VERSION", PROJECT_VERSION);
        MVN_OPTIONS = getString(clr, "OPTIONS", MVN_OPTIONS);
        JAVA_VERSION = getString(clr, "JAVA_VERSION", JAVA_VERSION);
        SEMANTIC_FORMAT = getString(clr, "SEMANTIC_FORMAT", SEMANTIC_FORMAT);

        //Boolean
        MVN_PROFILES = getBoolean(clr, "PROFILES", MVN_PROFILES);
        MVN_CLEAN = getBoolean(clr, "CLEAN", MVN_CLEAN);
        MVN_CLEAN_CACHE = getBoolean(clr, "CLEAN_CACHE", MVN_CLEAN_CACHE);
        MVN_UPDATE = getBoolean(clr, "UPDATE", MVN_UPDATE);
        MVN_JAVA_DOC = getBoolean(clr, "JAVA_DOC", MVN_JAVA_DOC);
        MVN_SOURCE = getBoolean(clr, "SOURCE", MVN_SOURCE);
        MVN_TAG = getBoolean(clr, "TAG", MVN_TAG);
        MVN_TAG_BREAK = getBoolean(clr, "TAG_BREAK", MVN_TAG_BREAK);
        MVN_RELEASE = getBoolean(clr, "RELEASE", MVN_RELEASE);
        MVN_SKIP_TEST = getBoolean(clr, "SKIP_TEST", MVN_SKIP_TEST);
        MVN_REPORT = getBoolean(clr, "REPORT", MVN_REPORT);
        MVN_REMOVE_SNAPSHOT = getBoolean(clr, "REMOVE_SNAPSHOT", MVN_REMOVE_SNAPSHOT);

        //DEOPLY (Nexus only currently)
        MVN_DEPLOY_ID = getString(clr, "DEPLOY_ID", MVN_DEPLOY_ID);

        //GPG
        GPG_PASS = getString(clr, "GPG_PASS", GPG_PASS);
        GPG_PASS_ALT = getString(clr, "GPG_PASS_ALT", GPG_PASS_ALT);

        pom = parsePomFile(PROJECT_DIR);
        IS_POM = isPomArtifact(pom);

        semanticService = new SemanticService(isEmpty(SEMANTIC_FORMAT)? "\\.:none" : SEMANTIC_FORMAT, PROJECT_DIR);
        gitService = new GitService(PROJECT_DIR);

        PROJECT_VERSION = isEmpty(SEMANTIC_FORMAT) ?
                PROJECT_VERSION : semanticService.getNextSemanticVersion(pom.getVersion(), gitService, PROJECT_VERSION);
    }

    public String getProjectVersion() {
        return isEmpty(PROJECT_VERSION) ? pom.getVersion() : PROJECT_VERSION;
    }

    public String getBranchName() {
        final String branchName = semanticService.getBranchName();
        return branchName == null ? gitService.findOriginalBranchName(1) : branchName;
    }

    public String prepareMaven() {
        final StringBuilder mvnCommand = new StringBuilder();
        mvnCommand.append("mvn").append(" ");
        mvnCommand.append(ifDo(MVN_CLEAN_CACHE, CMD_MVN_CLEAN_CACHE, "CLEAN_CACHE"));
        mvnCommand.append(ifDo(MVN_CLEAN, "clean", "CLEAN"));
        mvnCommand.append(isEmpty(MVN_DEPLOY_ID) ? "verify" : "deploy").append(" ");
        mvnCommand.append(ifDo(MVN_SKIP_TEST, CMD_MVN_SKIP_TEST, "SKIP_TEST"));
        mvnCommand.append(ifDo(MVN_CLEAN, CMD_MVN_CLEAN));
        mvnCommand.append(ifDo(MVN_UPDATE, CMD_MVN_UPDATE, "UPDATE"));
        mvnCommand.append(ifDo((!isEmpty(PROJECT_VERSION) || MVN_REMOVE_SNAPSHOT), CMD_MVN_VERSION_XX));
        mvnCommand.append(ifDo(PROJECT_VERSION, XX_CMD_MVN_VERSION + PROJECT_VERSION, "PROJECT_VERSION"));
        mvnCommand.append(ifDo(MVN_REMOVE_SNAPSHOT, XX_CMD_MVN_SNAPSHOT, "REMOVE_SNAPSHOT"));
        mvnCommand.append(ifDo(!IS_POM && MVN_JAVA_DOC, CMD_MVN_JAVADOC, "JAVA_DOC"));
        mvnCommand.append(ifDo(!IS_POM && MVN_SOURCE, CMD_MVN_SOURCE, "SOURCE"));
        mvnCommand.append(ifDo(hasNewTag(), CMD_MVN_TAG_XX + PROJECT_VERSION, "TAG"));
        mvnCommand.append(ifDo(GPG_PASS, CMD_MVN_GPG_SIGN_XX + GPG_PASS, "GPG_PASSPHRASE"));
        mvnCommand.append(ifDo(GPG_PASS_ALT, CMD_MVN_GPG_SIGN_ALT_XX + GPG_PASS_ALT, "GPG_PASSPHRASE_ALT"));
        mvnCommand.append(ifDo(MVN_DEPLOY_ID, prepareNexusDeployUrl(), "DEPLOY_ID"));
        mvnCommand.append(ifDo(MVN_OPTIONS, MVN_OPTIONS, "OPTIONS"));
        mvnCommand.append(ifDo(ENCODING, "-Dproject.build.sourceEncoding=" + ENCODING, "ENCODING"));
        mvnCommand.append(ifDo(ENCODING, "-Dproject.reporting.outputEncoding=" + ENCODING));
        mvnCommand.append(ifDo(ENCODING, "-Dproject.encoding=" + ENCODING));
        mvnCommand.append(ifDo(JAVA_VERSION, "-Dmaven.compiler.source=" + JAVA_VERSION, "JAVA_VERSION"));
        mvnCommand.append(ifDo(JAVA_VERSION, "-Dmaven.compiler.target=" + JAVA_VERSION));
        mvnCommand.append(ifDo(MVN_PROFILES, prepareMavenProfileParam(), "PROFILES"));
        mvnCommand.append(ifDo(!MVN_SKIP_TEST, prepareSurFire(), "SKIP_TEST"));
        mvnCommand.append(ifDo(!MVN_SKIP_TEST, prepareFailSafe()));
        mvnCommand.append(ifDo(MVN_REPORT, CMD_MVN_REPORT, "REPORT"));
        mvnCommand.append(ifDo((!isEmpty(PROJECT_VERSION) || MVN_REMOVE_SNAPSHOT || MVN_REPORT), "-DgenerateBackupPoms=false "));

        if (!isEmpty(GPG_PASS_ALT)) {
            new GpgUtil(LOG).downloadMavenGpgIfNotExists(PROJECT_DIR);
        }
        return mvnCommand.toString().trim();
    }

    public CommandLineReader getCommandLineReader() {
        return clr;
    }

    private Model parsePomFile(final File projectDir) {
        try {
            return new MavenXpp3Reader().read(new FileReader(new File(projectDir, "pom.xml")));
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("could not read pom.xml \n ", e);
        }
    }

    private String prepareSurFire() {
        final File failSafeConf = createTmpConf("mvnSurFireExcludes_");
        writeFile(failSafeConf, FILE_MVN_SURFIRE);
        return CMD_MVN_SURFIRE_XX + failSafeConf.getAbsolutePath();

    }

    private String prepareFailSafe() {
        final File failSafeConf = createTmpConf("mvnFailSafeIncludes_");
        writeFile(failSafeConf, FILE_MVN_FAILSAFE);
        return CMD_MVN_FAILSAFE_XX + failSafeConf.getAbsolutePath();
    }

    private String prepareNexusDeployUrl() {
        return SONATYPE_PLUGIN + " -DaltDeploymentRepository=" + MVN_DEPLOY_ID + "::" + MVN_DEPLOY_LAYOUT + "::" + SONATYPE_STAGING_URL + " -DnexusUrl=" + SONATYPE_URL + " -DserverId=" + MVN_DEPLOY_ID + " -DautoReleaseAfterClose=false";
    }

    private boolean hasNewTag() {
        if ((MVN_TAG || MVN_TAG_BREAK) && !isEmpty(PROJECT_VERSION)) {
            final String lastGitTag = gitService.getLastGitTag();
            printTagMessage(lastGitTag);
            return !PROJECT_VERSION.equalsIgnoreCase(lastGitTag);
        }
        return false;
    }

    private void printTagMessage(final String lastGitTag) {
        if (MVN_TAG_BREAK && PROJECT_VERSION.equalsIgnoreCase(lastGitTag)) {
            throw new RuntimeException(format("GIT_TAG [%s] already exists", PROJECT_VERSION));
        } else {
            LOG.info(format("New GIT_TAG [%s]", PROJECT_VERSION));
        }
    }

    private boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    private String prepareMavenProfileParam() {
        if (MVN_PROFILES) {
            LOG.debug("Read maven profiles");
            final String command = "mvn help:all-profiles | grep \"Profile Id\" | cut -d' ' -f 5 | xargs | tr ' ' ','";
            final String mvnProfiles = newTerminal().timeoutMs(-1).execute(command).consoleInfo();
            LOG.info(format("Found maven profiles [%s]", mvnProfiles.trim()));
            return isEmpty(mvnProfiles) ? "" : "--activate-profiles=" + mvnProfiles.trim();
        }
        return "";
    }

    private Boolean isPomArtifact(final Model pom) {
        final String packaging = pom.getPackaging();
        return isEmpty(packaging) || packaging.trim().equals("pom");
    }

    private String ifDo(final String trigger, final String arg) {
        return ifDo(!isEmpty(trigger), arg);
    }

    private String ifDo(final boolean trigger, final String arg) {
        return trigger ? arg + " " : "";
    }

    private String ifDo(final String trigger, final String arg, final String description) {
        return ifDo(!isEmpty(trigger), arg, description);
    }

    private String ifDo(final boolean trigger, final String arg, final String description) {
        LOG.debug(format("[%s] [%s]", trigger, description));
        return trigger ? arg + " " : "";
    }

    private String getString(final CommandLineReader clr, final String key, final String fallback) {
        return getOrElse(clr.getValue(key), fallback);
    }

    private boolean getBoolean(final CommandLineReader clr, final String key, final boolean fallback) {
        final boolean present = clr.isPresent(key);
        final String value = clr.getValue(key);
        if (present && isEmpty(value)) {
            return true;
        }
        return getOrElse(value, fallback);
    }

    private boolean getOrElse(final String test, final boolean fallback) {
        return !isEmpty(test) ? Boolean.valueOf(test) : fallback;
    }

    private String getOrElse(final String test, final String fallback) {
        return !isEmpty(test) ? test : fallback;
    }

    private File getOrElse(final String test, final File fallback) {
        final File file = !isEmpty(test) ? new File(test) : fallback;
        if (!file.exists()) {
            throw new RuntimeException(format("Path [%s] does not exist", file));
        }
        return file;
    }

    private Terminal newTerminal() {
        return new Terminal()
                .breakOnError(true)
                .consumerError(LOG::error)
                .dir(PROJECT_DIR).timeoutMs(32000);
    }

    private File createTmpConf(final String prefix) {
        try {
            return File.createTempFile(prefix, ".conf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFile(final File file, final String content) {
        try {
            Files.write(file.toPath(), content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
