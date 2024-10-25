package org.embeddedt.embeddium.gradle;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.gradle.api.Project;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        InputStream is = executeCommand("git", "log", "-1", "--pretty=\"format:%H   %B\"");
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

    public static void publishEmbeddiumJar(Project project, File file) {
        if(URL != null) {
            try(WebhookClient client = WebhookClient.withUrl(URL)) {
                WebhookMessage message = new WebhookMessageBuilder()
                        .setUsername("Embeddium Test Builds") // use this username
                        .setAvatarUrl("https://raw.githubusercontent.com/FiniteReality/embeddium/master/src/main/resources/icon.png") // use this avatar
                        .setContent(getLastCommitInfo())
                        .addFile(file)
                        .addFile("embeddium-" + project.getVersion() + "-sources.zip", getSourceTarball())
                        .build();
                client.onThread(TEST_BUILD_THREAD).send(message);
            }
        }
    }
}
