package berlin.yuna.mavendeploy.logic;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticService {

    private final File WORK_DIR;
    private final String[] SEMANTIC_FORMAT;
    private String branchName;

    public String getBranchName() {
        return branchName;
    }

    SemanticService(final String semanticFormat, final File workDir) {
        WORK_DIR = workDir;
        SEMANTIC_FORMAT = semanticFormat.split("::");
    }

    String getNextSemanticVersion(final String currentVersion, final GitService gitService, final String fallback) {
        for (int commitNumber = 1; commitNumber < 32; commitNumber++) {
            final String branchName = gitService.findOriginalBranchName(commitNumber);
            final int semanticPosition = getSemanticPosition(branchName);
            if (branchName != null && !branchName.trim().isEmpty() && semanticPosition != -1) {
                return getNextSemanticVersion(currentVersion, semanticPosition);
            }
        }
        return fallback;
    }

    String getNextSemanticVersion(final String versionOrg, final int semanticPosition) {
        final String separator = getSemanticSeparator(versionOrg);
        final StringBuilder nextVersion = new StringBuilder();
        for (String digit : prepareNextSemanticVersion(versionOrg, semanticPosition)) {
            nextVersion.append(digit).append(separator);
        }
        return nextVersion.delete((nextVersion.length() - 1), nextVersion.length()).toString();
    }

    private String getSemanticSeparator(final String versionOrg) {
        final Matcher matcher = Pattern.compile(SEMANTIC_FORMAT[0]).matcher(versionOrg);
        return matcher.find() ? matcher.group(0) : ".";
    }

    private String[] prepareNextSemanticVersion(final String versionOrg, final int semanticPosition) {
        final String[] version = versionOrg.split(SEMANTIC_FORMAT[0]);
        version[semanticPosition] = ((Integer) (Integer.valueOf(version[semanticPosition]) + 1)).toString();
        for (int i = (semanticPosition + 1); i < version.length; i++) {
            version[i] = "0";
        }
        return version;
    }

    private int getSemanticPosition(final String branchName) {
        for (int i = 1; i < SEMANTIC_FORMAT.length; i++) {
            if (Pattern.compile(SEMANTIC_FORMAT[i]).matcher(branchName).find()) {
                return i - 1;
            }
        }
        return -1;
    }
}
