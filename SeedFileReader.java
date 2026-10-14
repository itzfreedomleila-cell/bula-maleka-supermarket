package net.codejava.common.seed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Reads starter data from pipe-separated files in src/main/resources/seed,
 * so sample data lives in data files instead of being hard-coded in Java.
 * Lines starting with # are comments.
 */
@Component
public class SeedFileReader {

    /**
     * @param fileName e.g. "products.psv"
     * @return each data row split into its columns
     */
    public List<String[]> read(String fileName) {
        ClassPathResource resource = new ClassPathResource("seed/" + fileName);
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\\|", -1);
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = columns[i].trim();
                }
                rows.add(columns);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read seed file " + fileName, ex);
        }
        return rows;
    }
}
