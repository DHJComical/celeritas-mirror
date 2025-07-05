// This class exists because of the kotlin dsl not being able to find anything outside of the default package in the plugins block.
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class E {
    public static final Properties p;
    static {
        Properties properties = new Properties();
        try {
            Path path = Path.of(System.getProperty("rootDir")).resolve("gradle.properties");
            properties.load(Files.newInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        p = properties;
    }
}