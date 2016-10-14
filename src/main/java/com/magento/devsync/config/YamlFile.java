package com.magento.devsync.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlFile {

    @JsonProperty
    public List<Mount> mounts;

    public String contents;

    public static YamlFile parseYaml(String yamlFile) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        try {
            YamlFile yf = mapper.readValue(yamlFile, YamlFile.class);
            yf.contents = yamlFile;
            validateConfig(yf);
            return yf;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read configuration file.", e);
        }
    }

    private static void validateConfig(YamlFile config) {
        // Do some basic validation of the config file.
        if (config.mounts == null) {
            config.mounts = new ArrayList<>();
        }
        for (Mount m : config.mounts) {
            if (m.local == null) {
                throw new RuntimeException("'local' missing from mount definition");
            }
            m.local = cleanPath(m.local);
            if (m.remote == null) {
                throw new RuntimeException("'remote' missing from mount definition");
            }
            m.remote = cleanPath(m.remote);
            if (m.once == null) {
                m.once = new ArrayList<>();
            }
            checkSyncRules(m.once);
            if (m.watch == null) {
                m.watch = new ArrayList<>();
            }
            checkSyncRules(m.watch);
        }

    }

    private static void checkSyncRules(List<SyncRule> syncRules) {
        for (SyncRule sr : syncRules) {
            if (sr.mode == null) {
                throw new RuntimeException("'mode' missing from config file");
            }
            if (sr.mode.equals("push") || sr.mode.equals("pull") || sr.mode.equals("sync")) {
                // Good
            } else {
                throw new RuntimeException("'mode' must be push/pull/sync");
            }
            if (sr.path == null) {
                sr.path = ".";
            }
            if (sr.exclude == null) {
                sr.exclude = new ArrayList<>();
            }
        }
    }

    private static String cleanPath(String path) {
        if (path.length() == 0) {
            return ".";
        }
        while (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}