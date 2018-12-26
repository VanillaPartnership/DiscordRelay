/*
 * discord-relay: A Minecraft <=> Discord relay, but for Vanilla servers.
 * Copyright (C) 2018 VanillaPartnership
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.valkyrin.relay.discord;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.valkyrin.relay.minecraft.LogEntry;
import net.valkyrin.relay.minecraft.ShutdownEvent;
import org.intellij.lang.annotations.Language;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LogSender
    implements AutoCloseable {
    EventBus bus;
    TextChannel console;
    BlockingQueue<String> consoleQueue;
    AtomicReference<Thread> consoleQueueDumpThread;
    Pattern userParser;
    WebhookClient client;
    private static Pattern messagePattern = Pattern.compile("^<(.*)> (.*$)");

    public LogSender(final @NonNull ExecutorService service, final @NonNull EventBus bus, final @NonNull JDA jda,
                     final @NonNull String consoleChannel, final @NonNull String chatChannel,
                     final @NonNull @Language("Regexp") String userParseRegex) {
        this.bus = bus;
        this.console = jda.getTextChannelById(consoleChannel);
        final TextChannel chat = jda.getTextChannelById(chatChannel);
        this.consoleQueue = new LinkedBlockingQueue<>();
        this.consoleQueueDumpThread = new AtomicReference<>();
        this.userParser = Pattern.compile(userParseRegex);
        this.client = getWebhookClient(chat);
        service.submit(() -> {
            consoleQueueDumpThread.set(Thread.currentThread());
            StringBuilder builder = new StringBuilder();
            while (!service.isShutdown()) {
                try {
                    builder.append(consoleQueue.take());
                    while (consoleQueue.peek() != null && builder.length() + consoleQueue.peek().length() < 2000) {
                        builder.append(consoleQueue.poll()).append('\n');
                    }
                    console.sendMessage(builder.toString()).complete();
                    builder.setLength(0);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    private static WebhookClient getWebhookClient(final @NonNull TextChannel chat) {
        final Optional<Webhook> result = chat.getWebhooks().complete().stream().filter(webhook -> webhook.getName()
            .equals("minecraft-relay")).findFirst();
        final Webhook webhook = result.orElseGet(() -> chat.createWebhook("minecraft-relay").complete());
        WebhookClientBuilder builder = webhook.newClient();
        return builder.build();
    }

    @Subscribe
    public void onLogEntry(final @NonNull LogEntry entry) {
        String consoleMessage = String.format("%s/%s: %s", entry.getThread(), entry.getLevel(), entry.getMessage());
        consoleQueue.add(consoleMessage);
        if (entry.getMessage().equals("Stopping server")) {
            bus.post(new ShutdownEvent());
            if (consoleQueueDumpThread.get() != null) {
                consoleQueueDumpThread.get().interrupt();
            }
        } else {
            Matcher matcher = messagePattern.matcher(entry.getMessage());
            if (matcher.matches()) {
                Matcher playerNameMatcher = userParser.matcher(matcher.group(1));
                if (playerNameMatcher.matches()) {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder();
                    builder.setContent(matcher.group(2));
                    builder.setAvatarUrl("https://minotar.net/avatar/" + playerNameMatcher.group());
                    builder.setUsername(playerNameMatcher.group());
                    client.send(builder.build());
                }
            }
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
