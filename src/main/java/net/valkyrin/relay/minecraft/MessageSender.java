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

package net.valkyrin.relay.minecraft;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.core.entities.Message;
import net.kronos.rkon.core.Rcon;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class MessageSender
    implements AutoCloseable {
    AtomicReference<Rcon> rcon;
    EventBus bus;

    public MessageSender(final @NonNull ExecutorService service, final @NonNull EventBus bus,
                         final @NonNull String host, final int port, final @NonNull String password) {
        this.bus = bus;
        rcon = new AtomicReference<>();
        service.submit(() -> {
            while (rcon.get() == null) {
                try {
                    rcon.set(new Rcon(host, port, password.getBytes()));
                } catch (Exception ignored) {
                    try {
                        System.out.println("Connection to RCON failed, retrying...");
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
    }

    @Subscribe
    public void onDiscordForwardedMessage(final @NonNull Message message) {
        TextComponent.Builder builder = TextComponent.builder();
        builder.content(message.getMember().getEffectiveName());
        builder.color(KnownColor.getClosest(message.getMember().getColor()).getTextColor());
        builder.append(TextComponent.of(": " + message.getContentStripped(), TextColor.WHITE));
        rcon.getAndUpdate(executableRcon -> {
            if (executableRcon != null) {
                try {
                    executableRcon.command("/tellraw @a " + ComponentSerializers.JSON.serialize(builder.build()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return executableRcon;
        });
    }

    @Subscribe
    public void onCommand(final @NonNull Command command) {
        rcon.getAndUpdate(executableRcon -> {
            if (executableRcon != null) {
                try {
                    bus.post(new LogEntry(executableRcon.command(command.getPayload()),
                                          executableRcon.getSocket().getInetAddress()
                    ));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return executableRcon;
        });
    }

    @Subscribe
    public void onShutdown(final @NonNull ShutdownEvent event) {
        close();
    }

    @Override
    public void close() {
        rcon.getAndUpdate(executableRcon -> {
            if (executableRcon != null) {
                try {
                    executableRcon.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        });
    }
}
