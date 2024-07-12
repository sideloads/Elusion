package dev.sideloaded.elusion.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommand {
    String getName();

    CommandData getCommandData();

    void execute(SlashCommandInteractionEvent event);

    boolean isAuthorized(String userId);

}