package org.embeddedt.embeddium.gradle.fabric.remapper;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import bs.ModLoader;
import com.google.common.io.ByteStreams;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Based on code from Fabric Loom, used under the terms of the MIT License.
 */
public abstract class FabricApiModuleFinder {
    @Inject
    public abstract Project getProject();

    private record FabricApiVersion(ModLoader loader, String fabricApiVersion) {
        public String getGroup() {
            return switch (loader) {
                case FABRIC -> "net.fabricmc.fabric-api";
                case FORGE -> "dev.su5ed.sinytra.fabric-api";
                case NEOFORGE -> "org.sinytra.forgified-fabric-api";
            };
        }

        public String getMavenUrl() {
            if (loader == ModLoader.FABRIC) {
                return "https://maven.fabricmc.net";
            } else {
                return "https://maven.su5ed.dev/releases";
            }
        }
    }

    private static final ConcurrentHashMap<FabricApiVersion, Map<String, String>> moduleVersionCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<FabricApiVersion, Map<String, String>> deprecatedModuleVersionCache = new ConcurrentHashMap<>();

    public Dependency module(ModLoader loader, String moduleName, String fabricApiVersion) {
        return getProject().getDependencies()
                .create(getDependencyNotation(moduleName, new FabricApiVersion(loader, fabricApiVersion)));
    }

    private String moduleVersion(String moduleName, FabricApiVersion version) {
        String moduleVersion = moduleVersionCache
                .computeIfAbsent(version, this::getApiModuleVersions)
                .get(moduleName);

        if (moduleVersion == null) {
            moduleVersion = deprecatedModuleVersionCache
                    .computeIfAbsent(version, this::getDeprecatedApiModuleVersions)
                    .get(moduleName);
        }

        if (moduleVersion == null) {
            throw new RuntimeException("Failed to find module version for module: " + moduleName);
        }

        return moduleVersion;
    }

    private String getDependencyNotation(String moduleName, FabricApiVersion version) {
        return String.format("%s:%s:%s", version.getGroup(), moduleName, moduleVersion(moduleName, version));
    }

    private Map<String, String> getApiModuleVersions(FabricApiVersion fabricApiVersion) {
        try {
            return populateModuleVersionMap(getApiMavenPom(fabricApiVersion));
        } catch (PomNotFoundException e) {
            throw new RuntimeException("Could not find fabric-api version: " + fabricApiVersion);
        }
    }

    private Map<String, String> getDeprecatedApiModuleVersions(FabricApiVersion fabricApiVersion) {
        try {
            return populateModuleVersionMap(getDeprecatedApiMavenPom(fabricApiVersion));
        } catch (PomNotFoundException e) {
            // Not all fabric-api versions have deprecated modules, return an empty map to cache this fact.
            return Collections.emptyMap();
        }
    }

    private Map<String, String> populateModuleVersionMap(File pomFile) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document pom = docBuilder.parse(pomFile);

            Map<String, String> versionMap = new ConcurrentHashMap<>();

            NodeList dependencies = ((Element) pom.getElementsByTagName("dependencies").item(0)).getElementsByTagName("dependency");

            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                Element artifact = (Element) dep.getElementsByTagName("artifactId").item(0);
                Element version = (Element) dep.getElementsByTagName("version").item(0);

                if (artifact == null || version == null) {
                    throw new RuntimeException("Failed to find artifact or version");
                }

                versionMap.put(artifact.getTextContent(), version.getTextContent());
            }

            return versionMap;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + pomFile.getName(), e);
        }
    }

    private File getApiMavenPom(FabricApiVersion fabricApiVersion) throws PomNotFoundException {
        String artifact = fabricApiVersion.loader == ModLoader.NEOFORGE ? "forgified-fabric-api" : "fabric-api";
        return getPom(artifact, fabricApiVersion);
    }

    private File getDeprecatedApiMavenPom(FabricApiVersion fabricApiVersion) throws PomNotFoundException {
        return getPom("fabric-api-deprecated", fabricApiVersion);
    }

    private File getPom(String name, FabricApiVersion version) throws PomNotFoundException {
        final var mavenPom = new File(getProject().getLayout().getBuildDirectory().getAsFile().get(), "fabric-api-%s/%s-%s.pom".formatted(version.loader.friendlyName, name, version.fabricApiVersion()));

        if(!mavenPom.exists()) {
            mavenPom.getParentFile().mkdirs();
            try(FileOutputStream fos = new FileOutputStream(mavenPom)) {
                String urlStr = String.format("%3$s/%4$s/%2$s/%1$s/%2$s-%1$s.pom", version.fabricApiVersion(), name, version.getMavenUrl(), version.getGroup().replace('.', '/'));
                URL url = new URL(urlStr);
                try(InputStream stream = url.openStream()) {
                    ByteStreams.copy(stream, fos);
                }
            } catch (IOException e) {
                mavenPom.delete();
                throw new UncheckedIOException("Failed to download maven info to " + mavenPom.getName(), e);
            }
        }

        return mavenPom;
    }

    private static class PomNotFoundException extends Exception {
        PomNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
