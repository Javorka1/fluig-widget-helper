package com.fluiggers.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;

import com.fluiggers.dto.WidgetDto;
import com.fluiggers.repository.WidgetRepository;

public class WidgetService {

    public List<WidgetDto> findAll() throws Exception {
        return new WidgetRepository().findAll();
    }

    public FileInputStream getWidgetFileInputStream(
        ServletContext servletContext,
        String filename
    ) throws FileNotFoundException {
        Set<String> candidates = getCandidatePaths(servletContext, filename);

        for (String path : candidates) {
            File widgetFile = new File(path);
            if (widgetFile.exists() && widgetFile.isFile()) {
                return new FileInputStream(widgetFile);
            }
        }

        File domainContentWar = findInWildFlyContentRepository(filename);
        if (domainContentWar != null) {
            return new FileInputStream(domainContentWar);
        }

        throw new FileNotFoundException(
            "Arquivo não encontrado: " + filename
            + " | realPath=" + servletContext.getRealPath("/")
            + " | jboss.home.dir=" + System.getProperty("jboss.home.dir")
            + " | jboss.server.base.dir=" + System.getProperty("jboss.server.base.dir")
            + " | testados=" + String.join(", ", candidates)
        );
    }

    private File findInWildFlyContentRepository(String filename) {
        String baseName = filename.replaceAll("(?i)\\.war$", "");
        Set<String> targetCodes = new LinkedHashSet<>();
        targetCodes.add(baseName.toLowerCase());

        try {
            for (WidgetDto dto : findAll()) {
                if (filename.equalsIgnoreCase(dto.getFilename()) && dto.getCode() != null) {
                    targetCodes.add(dto.getCode().toLowerCase());
                }
            }
        } catch (Exception ignore) {
        }

        Set<File> contentDirs = new LinkedHashSet<>();
        String serverBase = System.getProperty("jboss.server.base.dir");
        if (serverBase != null && !serverBase.isBlank()) {
            contentDirs.add(new File(serverBase, "data" + File.separator + "content"));
        }
        String domainBase = System.getProperty("jboss.domain.base.dir");
        if (domainBase != null && !domainBase.isBlank()) {
            contentDirs.add(new File(domainBase, "data" + File.separator + "content"));
        }
        String jbossHome = System.getProperty("jboss.home.dir");
        if (jbossHome != null && !jbossHome.isBlank()) {
            contentDirs.add(new File(jbossHome, "domain" + File.separator + "data" + File.separator + "content"));
        }

        for (File dir : contentDirs) {
            if (!dir.exists() || !dir.isDirectory()) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir.toPath(), 4)) {
                List<File> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> "content".equalsIgnoreCase(p.getFileName().toString()))
                    .map(Path::toFile)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed())
                    .collect(Collectors.toList());

                for (File f : files) {
                    if (matchesWidgetWar(f, targetCodes)) {
                        return f;
                    }
                }
            } catch (Exception ignore) {
            }
        }

        return null;
    }

    private boolean matchesWidgetWar(File file, Set<String> targetCodes) {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry("WEB-INF/classes/application.info");
            if (entry == null) {
                entry = zip.getEntry("application.info");
            }
            if (entry == null) {
                return false;
            }
            Properties props = new Properties();
            try (InputStream is = zip.getInputStream(entry)) {
                props.load(is);
            }
            String appCode = props.getProperty("application.code");
            return appCode != null && targetCodes.contains(appCode.trim().toLowerCase());
        } catch (Exception ignore) {
            return false;
        }
    }

    private Set<String> getCandidatePaths(ServletContext servletContext, String filename) {
        Set<String> paths = new LinkedHashSet<>();

        String realPath = servletContext.getRealPath("/");
        if (realPath != null && !realPath.isBlank()) {
            String appserverBase = realPath.replaceAll("(?i)^(.+?appserver).*", "$1");
            paths.add(appserverBase + File.separator + "apps" + File.separator + filename);
        }

        String jbossHome = System.getProperty("jboss.home.dir");
        if (jbossHome != null && !jbossHome.isBlank()) {
            paths.add(jbossHome + File.separator + "apps" + File.separator + filename);
            paths.add(jbossHome + File.separator + "standalone" + File.separator + "deployments" + File.separator + filename);
            File parent = new File(jbossHome).getParentFile();
            if (parent != null) {
                paths.add(parent.getAbsolutePath() + File.separator + "apps" + File.separator + filename);
                paths.add(parent.getAbsolutePath() + File.separator + "repository" + File.separator + "wcmdir" + File.separator + "apps" + File.separator + filename);
            }
        }

        String serverBase = System.getProperty("jboss.server.base.dir");
        if (serverBase != null && !serverBase.isBlank()) {
            paths.add(serverBase + File.separator + "deployments" + File.separator + filename);
            paths.add(serverBase + File.separator + "apps" + File.separator + filename);
        }

        String domainBase = System.getProperty("jboss.domain.base.dir");
        if (domainBase != null && !domainBase.isBlank()) {
            paths.add(domainBase + File.separator + "apps" + File.separator + filename);
            File parent = new File(domainBase).getParentFile();
            if (parent != null) {
                paths.add(parent.getAbsolutePath() + File.separator + "apps" + File.separator + filename);
            }
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            paths.add(userDir + File.separator + "apps" + File.separator + filename);
        }

        return paths;
    }
}
