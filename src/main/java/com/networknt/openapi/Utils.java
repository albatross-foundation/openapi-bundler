package com.networknt.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.networknt.oas.OpenApiParser;
import com.networknt.oas.model.OpenApi3;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class Utils {

    private static final String JSON_EXT = "json";
    private static final String YAML_EXT = "yaml";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void writeJson(Map<String, Object> map, String outputDir, String outputFile, boolean outputJSON) throws IOException {
        // Convert the map back to JSON and serialize it.
        if (!outputJSON) {
            return;
        }

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);

        writeData(JSON_EXT, json, outputDir, outputFile);
    }

    /**
     * Convert the map back to YAML and serialize it.
     */
    public static void writeYaml(Map<String, Object> map, String outputDir, String outputFile, boolean outputYaml) throws IOException {

        if (!outputYaml) {
            return;
        }

        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        yamlFactory.disable(YAMLGenerator.Feature.SPLIT_LINES);
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        yamlFactory.disable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
        yamlFactory.disable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE);

        ObjectMapper objMapper = new ObjectMapper(yamlFactory);
        String yamlOutput = objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        writeData(YAML_EXT, yamlOutput, outputDir, outputFile);
    }

    private static void writeData(String suffix, String output, String outputDir, String outputFile) throws IOException {

        Path dir = Paths.get(outputDir);

        Path outPath = dir.resolve(String.format("%s.%s", outputFile, suffix));
        // write the output

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Files.write(outPath, output.getBytes());

        // validate the output file
        validateSpecification(outPath);
    }

    public static boolean validateSpecification(Path p) {
        try {
            @SuppressWarnings("unused") OpenApi3 model = (OpenApi3) new OpenApiParser().parse(new File(p.toString()), true);

            log.info("OpenAPI3 Validation: Definition file: <{}> is valid ....", p);
            return true;
        } catch (Exception e) {
            log.error("OpenAPI3 Validation: Definition file <{}> in directory <{}> failed with exception", p, e);
            return false;
        }
    }

    public static List<String> files(String directory) {
        List<String> textFiles = new ArrayList<>();
        File dir = new File(directory);
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith((".yaml")) || file.getName().endsWith((".yml"))) {
                textFiles.add(file.getName());
            }
        }
        return textFiles;
    }

}
