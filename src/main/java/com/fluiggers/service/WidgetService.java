package com.fluiggers.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        throw new FileNotFoundException(
            "Arquivo não encontrado: " + filename
            + " | realPath=" + servletContext.getRealPath("/")
            + " | jboss.home.dir=" + System.getProperty("jboss.home.dir")
            + " | jboss.server.base.dir=" + System.getProperty("jboss.server.base.dir")
            + " | testados=" + String.join(", ", candidates)
        );
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
