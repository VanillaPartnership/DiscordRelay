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
import com.google.gson.Gson;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

@SuppressWarnings("UnstableApiUsage")
public final class LogReceiver
    implements AutoCloseable {
    @NonNull
    private final ServerSocket server;

    public LogReceiver(final @NonNull Gson gson, final @NonNull ExecutorService service, final @NonNull EventBus bus,
                       final @NonNull SocketAddress serverAddress, final int backlog) throws
                                                                                      IOException {
        this.server = new ServerSocket();
        server.bind(serverAddress, backlog);
        service.submit(() -> {
            while (!service.isShutdown()) {
                try {
                    Socket socket = server.accept();
                    service.submit(() -> {
                        try (
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                        ) {
                            reader.lines().map(line -> gson.fromJson(line, LogEntry.class)).forEach(entry -> {
                                entry.setSender(socket.getInetAddress());
                                bus.post(entry);
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    if (!e.getMessage().contains("closed")) {
                        e.printStackTrace();
                    } else {
                        break;
                    }
                }
            }
        });
    }

    @Subscribe
    public void onShutdown(ShutdownEvent event) throws
                                                Exception {
        this.close();
    }

    @Override
    public void close() throws
                        Exception {
        server.close();
    }
}
