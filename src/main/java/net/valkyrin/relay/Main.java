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

package net.valkyrin.relay;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.valkyrin.relay.discord.LogSender;
import net.valkyrin.relay.discord.MessageReceiver;
import net.valkyrin.relay.minecraft.LogReceiver;
import net.valkyrin.relay.minecraft.MessageSender;
import net.valkyrin.relay.minecraft.ShutdownEvent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("UnstableApiUsage")
public class Main {
    public static void main(String[] args) throws
                                           Exception {
        Properties properties = loadProperties();
        Gson gson = new Gson();
        ExecutorService service = Executors.newWorkStealingPool();
        EventBus bus = new EventBus();
        JDA jda = new JDABuilder(AccountType.BOT).setToken(properties.getProperty("token")).setEnableShutdownHook(false)
            .build();
        jda.awaitReady();
        LogSender logSender = new LogSender(service,
                                            bus,
                                            jda,
                                            properties.getProperty("console"),
                                            properties.getProperty("chat"),
                                            properties.getProperty("player.regexp")
        );
        bus.register(logSender);
        MessageReceiver messageReceiver = new MessageReceiver(bus,
                                                              jda,
                                                              properties.getProperty("console"),
                                                              properties.getProperty("chat")
        );
        jda.addEventListener(messageReceiver);
        MessageSender messageSender = new MessageSender(service,
                                                        bus,
                                                        properties.getProperty("rcon.host"),
                                                        Integer.parseInt(properties.getProperty("rcon.port")),
                                                        properties.getProperty("rcon.password")
        );
        bus.register(messageSender);
        LogReceiver logReceiver = new LogReceiver(gson,
                                                  service,
                                                  bus,
                                                  new InetSocketAddress(properties.getProperty("server.host"),
                                                                        Integer.parseInt(properties
                                                                                             .getProperty("server.port"))
                                                  ),
                                                  Integer.parseInt(properties.getProperty("server.backlog"))
        );
        bus.register(logReceiver);
        ShutdownLatch latch = new ShutdownLatch();
        bus.register(latch);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> latch.onShutdown(new ShutdownEvent())));
        latch.awaitShutdown();
        jda.shutdown();
        service.shutdown();
    }

    private static Properties loadProperties() throws
                                               IOException {
        Properties properties = new Properties();
        File propFile = new File("discord-relay.properties");
        FileReader reader = new FileReader(propFile);
        properties.load(reader);
        return properties;
    }

    public static class ShutdownLatch {
        private final CountDownLatch latch;

        public ShutdownLatch() {
            latch = new CountDownLatch(1);
        }

        public void awaitShutdown() throws
                                    InterruptedException {
            latch.await();
        }

        @Subscribe
        public void onShutdown(ShutdownEvent event) {
            latch.countDown();
        }
    }
}
