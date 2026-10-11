package org.gradle.wrapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Small, auditable Gradle bootstrapper used only to make this generated project self-contained.
 * It downloads the distribution declared in gradle-wrapper.properties, verifies its SHA-256,
 * extracts it under GRADLE_USER_HOME, then delegates to the distribution's gradle executable.
 */
public final class GradleWrapperMain {
    private GradleWrapperMain() {}

    public static void main(String[] args) {
        try {
            int exit = run(args);
            if (exit != 0) System.exit(exit);
        } catch (Exception error) {
            System.err.println("Echo Inbox Gradle bootstrap failed: " + error.getMessage());
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int run(String[] args) throws Exception {
        Path projectRoot = locateProjectRoot();
        Path propertiesPath = projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(propertiesPath)) {
            properties.load(input);
        }

        String distributionUrl = require(properties, "distributionUrl");
        String expectedSha = properties.getProperty("distributionSha256Sum", "").trim().toLowerCase();
        int timeoutMillis = Integer.parseInt(properties.getProperty("networkTimeout", "10000"));
        boolean validateUrl = Boolean.parseBoolean(
            properties.getProperty("validateDistributionUrl", "true")
        );
        if (validateUrl && !isAllowedDistributionUrl(distributionUrl)) {
            throw new IOException("Refusing unexpected Gradle distribution URL: " + distributionUrl);
        }

        String fileName = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1);
        if (!fileName.endsWith(".zip")) {
            throw new IOException("Gradle distribution must be a .zip: " + fileName);
        }
        String distributionName = fileName.substring(0, fileName.length() - 4);
        Path gradleUserHome = resolveGradleUserHome();
        Path installRoot = gradleUserHome
            .resolve("wrapper/dists")
            .resolve(distributionName)
            .resolve(shortHash(distributionUrl));
        Path lockPath = installRoot.resolveSibling(installRoot.getFileName() + ".lock");
        Files.createDirectories(lockPath.getParent());

        try (
            FileChannel channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            );
            FileLock ignored = channel.lock()
        ) {
            Path executable = findGradleExecutable(installRoot);
            if (executable == null) {
                installDistribution(
                    distributionUrl,
                    expectedSha,
                    timeoutMillis,
                    installRoot
                );
                executable = findGradleExecutable(installRoot);
            }
            if (executable == null) {
                throw new IOException("Downloaded distribution does not contain bin/gradle");
            }
            executable.toFile().setExecutable(true, false);
            List<String> command = new ArrayList<>();
            command.add(executable.toAbsolutePath().toString());
            for (String arg : args) command.add(arg);
            Process process = new ProcessBuilder(command)
                .directory(projectRoot.toFile())
                .inheritIO()
                .start();
            return process.waitFor();
        }
    }

    private static void installDistribution(
        String distributionUrl,
        String expectedSha,
        int timeoutMillis,
        Path installRoot
    ) throws Exception {
        Path parent = installRoot.getParent();
        Files.createDirectories(parent);
        Path download = Files.createTempFile(parent, "gradle-download-", ".zip");
        Path extract = Files.createTempDirectory(parent, "gradle-extract-");
        try {
            download(distributionUrl, download, timeoutMillis);
            String actualSha = sha256(download);
            if (!expectedSha.isEmpty() && !actualSha.equals(expectedSha)) {
                throw new IOException(
                    "Gradle distribution checksum mismatch. Expected " + expectedSha +
                    " but received " + actualSha
                );
            }
            unzip(download, extract);
            deleteRecursively(installRoot);
            try {
                Files.move(extract, installRoot, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(extract, installRoot, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(download);
            deleteRecursively(extract);
        }
    }

    private static void download(String source, Path destination, int timeoutMillis) throws Exception {
        URLConnection connection = URI.create(source).toURL().openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(Math.max(timeoutMillis, 120_000));
        connection.setRequestProperty("User-Agent", "EchoInbox-Gradle-Bootstrap/1.0");
        if (connection instanceof HttpURLConnection http) {
            http.setInstanceFollowRedirects(true);
            int status = http.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Distribution download returned HTTP " + status);
            }
        }
        try (
            InputStream input = new BufferedInputStream(connection.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(destination))
        ) {
            input.transferTo(output);
        }
    }

    private static void unzip(Path archive, Path destination) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path output = normalizedDestination.resolve(entry.getName()).normalize();
                if (!output.startsWith(normalizedDestination)) {
                    throw new IOException("Unsafe path in Gradle archive: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    try (BufferedOutputStream target = new BufferedOutputStream(Files.newOutputStream(output))) {
                        zip.transferTo(target);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static Path findGradleExecutable(Path installRoot) throws IOException {
        if (!Files.isDirectory(installRoot)) return null;
        try (var children = Files.list(installRoot)) {
            return children
                .filter(Files::isDirectory)
                .map(child -> child.resolve(isWindows() ? "bin/gradle.bat" : "bin/gradle"))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
        }
    }

    private static Path locateProjectRoot() throws Exception {
        URL location = GradleWrapperMain.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation();
        Path jar = Path.of(location.toURI()).toAbsolutePath().normalize();
        Path root = jar.getParent(); // wrapper
        if (root != null) root = root.getParent(); // gradle
        if (root != null) root = root.getParent(); // project
        if (root == null) throw new IOException("Cannot locate project root from " + jar);
        return root;
    }

    private static Path resolveGradleUserHome() {
        String configured = System.getenv("GRADLE_USER_HOME");
        if (configured != null && !configured.isBlank()) return Path.of(configured);
        return Path.of(System.getProperty("user.home"), ".gradle");
    }

    private static boolean isAllowedDistributionUrl(String value) {
        URI uri = URI.create(value);
        if ("file".equalsIgnoreCase(uri.getScheme())) return true;
        return "https".equalsIgnoreCase(uri.getScheme()) &&
            ("services.gradle.org".equalsIgnoreCase(uri.getHost()) ||
             "downloads.gradle.org".equalsIgnoreCase(uri.getHost()));
    }

    private static String require(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IOException("Missing " + key);
        return value.trim();
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String shortHash(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes())).substring(0, 16);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException error) {
                        throw new RuntimeException(error);
                    }
                });
        } catch (RuntimeException wrapped) {
            if (wrapped.getCause() instanceof IOException io) throw io;
            throw wrapped;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
