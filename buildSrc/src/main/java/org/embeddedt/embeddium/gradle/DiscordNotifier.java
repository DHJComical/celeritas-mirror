package org.embeddedt.embeddium.gradle;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.common.collect.Lists;
import org.gradle.api.Project;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DiscordNotifier {
    private static final String URL;
    private static final long TEST_BUILD_THREAD = 1299434943704862791L;

    static {
        String url = System.getenv("DISCORD_WEBHOOK");
        if(url == null || url.length() == 0) {
            URL = null;
        } else {
            URL = url;
        }
    }

    private static InputStream executeCommand(String... args) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(args);
            return process.getInputStream();
        } catch(Exception e) {
            e.printStackTrace();
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private static String getLastCommitInfo() {
        InputStream is = executeCommand("git", "log", "-1", "--pretty=format:%H   %B");
        try {
            byte[] bs = is.readAllBytes();
            is.close();
            return "```\n" + new String(bs, StandardCharsets.UTF_8) + "\n```";
        } catch(IOException e) {
            e.printStackTrace();
            return "[unable to fetch commit information]";
        }
    }

    private static InputStream getSourceTarball() {
        return executeCommand("git", "archive", "--format=zip", "HEAD");
    }

    private static WebhookMessageBuilder makeBuilder() {
        return new WebhookMessageBuilder()
                .setUsername("Celeritas Test Builds") // use this username
                .setAvatarUrl("https://raw.githubusercontent.com/FiniteReality/embeddium/master/src/main/resources/icon.png"); // use this avatar
    }

    public static void publishEmbeddiumJar(Project project) {
        if(URL != null) {
            List<File> jars;
            File libsDir = new File(project.getRootDir(), "build/libs/" + project.getVersion().toString());
            if (libsDir.isDirectory()) {
                jars = Arrays.stream(libsDir.listFiles()).filter(File::isFile).collect(Collectors.toCollection(ArrayList::new));
                jars.sort(Comparator.comparing(File::getName));
            } else {
                throw new IllegalStateException("Cannot find " + libsDir.toString());
            }
            try(WebhookClient client = WebhookClient.withUrl(URL)) {
                CompletableFuture<?> future = client.onThread(TEST_BUILD_THREAD).send(makeBuilder()
                        .setContent(getLastCommitInfo()).build());
                var partitionList = Lists.partition(jars, 10);
                for(var partition : partitionList) {
                    future = future.thenCompose(v -> {
                        var builder = makeBuilder();
                        partition.forEach(builder::addFile);
                        return client.onThread(TEST_BUILD_THREAD).send(builder.build());
                    });
                }
                future = future.thenCompose(v -> {
                    var builder = makeBuilder();
                    return client.onThread(TEST_BUILD_THREAD)
                            .send(builder.addFile("celeritas-" + project.getVersion() + "-sources.zip", getSourceTarball()).build());
                });
                future.join();
            }
        } else {
            throw new IllegalStateException("No webhook found");
        }
    }
}
