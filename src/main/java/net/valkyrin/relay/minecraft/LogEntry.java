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

import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.slf4j.event.Level;

import java.net.InetAddress;

@Value
public class LogEntry {
    long timeMillis;
    String thread;
    Level level;
    String loggerName;
    String message;
    boolean endOfBatch;
    String loggerFqcn;
    int threadId;
    int threadPriority;
    @NonFinal
    @Setter
    InetAddress sender;

    public LogEntry(final @NonNull String message, final @NonNull InetAddress sender) {
        this.timeMillis = System.currentTimeMillis();
        this.thread = this.loggerName = this.loggerFqcn = "RCON";
        this.level = Level.INFO;
        this.message = message;
        this.endOfBatch = false;
        this.threadId = -1;
        this.threadPriority = -1;
        this.sender = sender;
    }
}
