package dev.sideloaded.elusion.command.impl;

import dev.sideloaded.elusion.command.SlashCommand;
import dev.sideloaded.elusion.database.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SyncUsersCommand implements SlashCommand {

    private static final String AUTHORIZED_USER_ID = "000000";

    @Override
    public String getName() {
        return "sync";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("sync", "Sync all users to the database");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getUser().getId().equals(AUTHORIZED_USER_ID)) {
            event.reply("You are not authorized to use this command.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().sendMessage("This command can only be used in a server.").queue();
            return;
        }

        guild.loadMembers().onSuccess(members -> {
            int syncedCount = 0;
            for (Member member : members) {
                if (!member.getUser().isBot()) {
                    String userId = member.getId();
                    String userName = member.getUser().getName();
                    DatabaseManager.addOrUpdateUser(userId, userName, null);
                    syncedCount++;
                }
            }

            event.getHook().sendMessage("Synced " + syncedCount + " users to the database.").queue();
        }).onError(error -> {
            event.getHook().sendMessage("An error occurred while syncing users.").queue();
            error.printStackTrace();
        });
    }
}