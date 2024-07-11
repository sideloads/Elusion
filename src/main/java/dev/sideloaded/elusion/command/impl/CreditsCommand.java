package dev.sideloaded.elusion.command.impl;

import dev.sideloaded.elusion.command.SlashCommand;
import dev.sideloaded.elusion.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bson.Document;

public class CreditsCommand implements SlashCommand {

    @Override
    public String getName() {
        return "credits";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("credits", "Check your credit balance");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        Document user = DatabaseManager.getUser(userId);

        if (user != null) {
            int credits = user.getInteger("credits", 0);
            int invites = user.getInteger("invites", 0);
            event.reply("Your current balance:\n" +
                    "Credits: " + credits + "\n" +
                    "Total Invites: " + invites)
                    .setEphemeral(true)
                    .queue();
        } else {
            event.reply("Sorry, I couldn't find your user data. Please try again later.")
                    .setEphemeral(true)
                    .queue();
        }
    }
}