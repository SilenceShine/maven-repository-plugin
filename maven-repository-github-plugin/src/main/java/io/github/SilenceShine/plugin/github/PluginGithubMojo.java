package io.github.SilenceShine.plugin.github;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.kohsuke.github.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author SilenceShine
 * @since 1.0
 */
@Mojo(name = "github",
        threadSafe = true,
        defaultPhase = LifecyclePhase.DEPLOY,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class PluginGithubMojo extends AbstractMojo {

    private static final String LOG_PREFIX = "Github ";
    private static final String SNAPSHOT = "SNAPSHOT";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", required = true, readonly = true)
    private Settings settings;

    @Parameter(defaultValue = "${id}")
    private String id;

    @Parameter(defaultValue = "${token}")
    private String token;

    @Parameter(defaultValue = "${owner}", required = true)
    private String owner;

    @Parameter(defaultValue = "${repository}", required = true)
    private String repository;

    @Parameter(defaultValue = "${message}")
    private String message;

    @Parameter(defaultValue = "${branchRelease}", required = true)
    private String branchRelease;

    @Parameter(defaultValue = "${branchSnapshot}", required = true)
    private String branchSnapshot;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        GitHub gitHub = connectGitHub();
        GHRepository ghRepository = getRepository(gitHub);
        String branch = getBranchName();
        String deployDir = getDeployDir();
        getLog().info(LOG_PREFIX + "deploy dir : " + deployDir);
        uploadFile(branch, ghRepository, deployDir);
    }

    private void uploadFile(String branch, GHRepository ghRepository, String deployDir) throws MojoExecutionException {
        try {
            GHRef ghRef = ghRepository.getRef("heads/" + branch);
            String mainTreeSha = ghRepository.getTreeRecursive(branch, 1).getSha();
            GHTreeBuilder treeBuilder = ghRepository.createTree().baseTree(mainTreeSha);
            int length = deployDir.length();
            for (File file : getFiles(deployDir)) {
                byte[] bytes = getBytes(file);
                String path = file.getAbsolutePath().substring(length + 1).replaceAll("\\\\", "/");
                getLog().info(LOG_PREFIX + "file : " + path);
                treeBuilder.add(path, bytes, false);
            }
            String treeSha = treeBuilder.create().getSha();
            GHCommit commit = ghRepository.createCommit()
                    .message(StringUtils.isBlank(message) ? "deploy file to github" : message)
                    .tree(treeSha)
                    .parent(ghRef.getObject().getSha())
                    .create();
            String commitSha = commit.getSHA1();
            ghRef.updateTo(commitSha);
        } catch (IOException e) {
            throw new MojoExecutionException(LOG_PREFIX + "upload file error : " + e.getMessage(), e);
        }
    }

    private byte[] getBytes(File file) throws MojoExecutionException {
        try (FileInputStream stream = new FileInputStream(file);) {
            byte[] bytes = new byte[(int) file.length()];
            stream.read(bytes);
            return bytes;
        } catch (Exception e) {
            throw new MojoExecutionException(LOG_PREFIX + "file read error : " + e.getMessage(), e);
        }
    }

    private String getBranchName() throws MojoExecutionException {
        String branch = project.getVersion().endsWith(SNAPSHOT) ? branchSnapshot : branchRelease;
        if (StringUtils.isBlank(branch)) {
            throw new MojoExecutionException(LOG_PREFIX + "branch not blank:");
        }
        getLog().info(LOG_PREFIX + "branch name : " + branch);
        return branch;
    }

    private String getDeployDir() {
        ArtifactRepository artifactRepository = project.getDistributionManagementArtifactRepository();
        return artifactRepository.getBasedir();
    }

    private List<File> getFiles(String deployDir) {
        File dirFile = new File(deployDir);
        return getFiles(dirFile);
    }

    private List<File> getFiles(File file) {
        List<File> files = new ArrayList<>();
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            assert listFiles != null;
            for (File f : listFiles) {
                files.addAll(getFiles(f));
            }
        } else {
            files.add(file);
        }
        return files;
    }

    private GHRepository getRepository(GitHub gitHub) throws MojoExecutionException {
        GHRepository ghRepository;
        try {
            ghRepository = gitHub.getRepository(owner + "/" + repository);
        } catch (IOException e) {
            throw new MojoExecutionException(LOG_PREFIX + " getRepository error: " + e.getMessage(), e);
        }
        getLog().info(LOG_PREFIX + "get repository : success");
        return ghRepository;
    }

    private GitHub connectGitHub() throws MojoExecutionException {
        GitHub gitHub;
        try {
            gitHub = GitHub.connectUsingOAuth(getToken());
        } catch (IOException e) {
            throw new MojoExecutionException(LOG_PREFIX + " connectUsingOAuth error: " + e.getMessage(), e);
        }
        getLog().info(LOG_PREFIX + "connect status : " + gitHub.isCredentialValid());
        return gitHub;
    }

    private String getToken() throws MojoExecutionException {
        if (null != token) {
            return token;
        }
        return settings.getServers()
                .stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new MojoExecutionException("id server not exist."))
                .getPassword();
    }

}
