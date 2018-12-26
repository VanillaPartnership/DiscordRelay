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
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.valkyrin.relay.minecraft.Command;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
@EqualsAndHashCode(callSuper = true)
@Value
public class MessageReceiver
    extends ListenerAdapter {
    EventBus bus;
    JDA jda;
    String consoleChannel;
    String chatChannel;

    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
        if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
            if (Objects.equals(event.getChannel().getId(), chatChannel) && !event.getAuthor().isFake()) {
                bus.post(event.getMessage());
            } else if (Objects.equals(event.getChannel().getId(), consoleChannel) && !event.getAuthor().isFake()) {
                bus.post(new Command(event.getMessage().getContentRaw()));
            }
        }
    }
}
