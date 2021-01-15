package com.networknt.openapi;

import com.github.mustachejava.DefaultMustacheFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MergeProcessor {

    public static final DefaultMustacheFactory DEFAULT_MUSTACHE_FACTORY = new DefaultMustacheFactory();

    private final Yaml yaml = new Yaml();
    private final Map<String, Object> scope = new HashMap<>();

    private String folder;
    private String fileName;
    private String outputDir;
    private String outputFile;


    public MergeProcessor() {
        init(System.getenv());
    }

    public MergeProcessor(String folder, String fileName, String outputDir, String outputFile) {
        this.folder = folder;
        this.fileName = fileName;
        this.outputDir = outputDir;
        this.outputFile = outputFile;
    }

    public MergeProcessor(Map<String, String> env) {
        if (env != null) init(env);
    }

    private void init(Map<String, String> env) {
        for (String varname : env.keySet()) {
            scope.put(varname, env.get(varname));
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> merge(String dir) throws IOException {
        List<String> files = Utils.files(dir);
        Map<String, Object> mergedResult = new LinkedHashMap<>();
        for (String file : files) {
            InputStream in = null;
            try {
                // read the file into a String
                in = new FileInputStream(dir + "/" + file);
                final String entireFile = IOUtils.toString(in);

                // substitute variables
                final StringWriter writer = new StringWriter(entireFile.length() + 10);
                DEFAULT_MUSTACHE_FACTORY.compile(new StringReader(entireFile), "mergeyml_" + System.currentTimeMillis()).execute(writer, scope);

                // load the YML file
                final Map<String, Object> yamlContents = (Map<String, Object>) yaml.load(writer.toString());

                // merge into results map
                merge_internal(mergedResult, yamlContents);
                log.info("loaded YML from " + file + ": " + yamlContents);

            } finally {
                if (in != null) in.close();
            }
        }
        Utils.writeYaml(mergedResult, outputDir, outputFile, true);
        return mergedResult;
    }

    @SuppressWarnings("unchecked")
    private void merge_internal(Map<String, Object> mergedResult, Map<String, Object> yamlContents) {

        if (yamlContents == null) return;

        for (String key : yamlContents.keySet()) {

            Object yamlValue = yamlContents.get(key);
            if (yamlValue == null) {
                addToMergedResult(mergedResult, key, null);
                continue;
            }

            Object existingValue = mergedResult.get(key);
            if (existingValue != null) {
                if (yamlValue instanceof Map) {
                    if (existingValue instanceof Map) {
                        merge_internal((Map<String, Object>) existingValue, (Map<String, Object>) yamlValue);
                    } else if (existingValue instanceof String) {
                        throw new IllegalArgumentException("Cannot merge complex element into a simple element: " + key);
                    } else {
                        throw unknownValueType(key, yamlValue);
                    }
                } else if (yamlValue instanceof String
                        || yamlValue instanceof Boolean
                        || yamlValue instanceof Double
                        || yamlValue instanceof List
                        || yamlValue instanceof Integer) {
                    log.info("overriding value of " + key + " with value " + yamlValue);
                    addToMergedResult(mergedResult, key, yamlValue);
                } else {
                    throw unknownValueType(key, yamlValue);
                }
            } else {
                if (yamlValue instanceof Map
                        || yamlValue instanceof List
                        || yamlValue instanceof String
                        || yamlValue instanceof Boolean
                        || yamlValue instanceof Integer
                        || yamlValue instanceof Double) {
                    log.info("adding new key->value: " + key + "->" + yamlValue);
                    addToMergedResult(mergedResult, key, yamlValue);
                } else {
                    throw unknownValueType(key, yamlValue);
                }
            }
        }
    }

    private IllegalArgumentException unknownValueType(String key, Object yamlValue) {
        final String msg = "Cannot merge element of unknown type: " + key + ": " + yamlValue.getClass().getName();
        log.error(msg);
        return new IllegalArgumentException(msg);
    }

    private Object addToMergedResult(Map<String, Object> mergedResult, String key, Object yamlValue) {
        return mergedResult.put(key, yamlValue);
    }

//    @SuppressWarnings("unchecked")
//    private void mergeLists(Map<String, Object> mergedResult, String key, Object yamlValue) {
//        if (!(yamlValue instanceof List && mergedResult.get(key) instanceof List)) {
//            throw new IllegalArgumentException("Cannot merge a list with a non-list: " + key);
//        }
//
//        List<Object> originalList = (List<Object>) mergedResult.get(key);
//        originalList.addAll((List<Object>) yamlValue);
//    }
//
//    public String mergeToString(String[] files) throws IOException {
//        return toString(merge(files));
//    }
//
//    public String mergeToString(List<String> files) throws IOException {
//        return toString(merge(files.toArray(new String[files.size()])));
//    }
//
//    public String toString(Map<String, Object> merged) throws IOException {
//        return yaml.dump(merged);
//    }
}
