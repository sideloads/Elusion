package dev.sideloaded.elusion.command;

import dev.sideloaded.elusion.command.impl.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.*;

public class CommandManager extends ListenerAdapter {
    private final Map<String, SlashCommand> commands = new HashMap<>();
    private final List<Long> allowedChannels = new ArrayList<>();

    public CommandManager() {
        registerCommand(new PingCommand());
        registerCommand(new GenerateCommand());
        registerCommand(new SyncUsersCommand());
        registerCommand(new CreditsCommand());
        registerCommand(new WhitelistCommand());
        registerCommand(new StockCommand());
        allowedChannels.add(1260571476927447246L);
    }

    private void registerCommand(SlashCommand command) {
        commands.put(command.getName(), command);
    }

    public void registerCommands(JDA jda, long guildId) {
        List<CommandData> commandData = new ArrayList<>();
        for (SlashCommand command : commands.values()) {
            commandData.add(command.getCommandData());
        }
        Objects.requireNonNull(jda.getGuildById(guildId)).updateCommands().addCommands(commandData).queue();
        jda.addEventListener(this);
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!allowedChannels.contains(event.getChannel().getIdLong())) {
            event.reply("This command cannot be used in this channel.").setEphemeral(true).queue();
            return;
        }

        String commandName = event.getName();
        SlashCommand command = commands.get(commandName);
        if (command != null) {
            if (command.isAuthorized(event.getUser().getId())) {
                command.execute(event);
            } else {
                event.reply("You are not authorized to use this command.").setEphemeral(true).queue();
            }
        }
    }

}