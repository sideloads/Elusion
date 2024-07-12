package dev.sideloaded.elusion.command.impl;

import dev.sideloaded.elusion.Config;
import dev.sideloaded.elusion.command.SlashCommand;
import dev.sideloaded.elusion.database.DatabaseManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bson.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class WhitelistCommand implements SlashCommand {

    @Override
    public String getName() {
        return "whitelist";
    }

    @Override
    public boolean isAuthorized(String userId) {
        return Config.getInstance().getAuthorizedUsers().contains(userId);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("whitelist", "Manage the whitelist")
                .addOptions(
                        new OptionData(OptionType.STRING, "action", "Action to perform")
                                .setRequired(true)
                                .addChoice("Add", "add")
                                .addChoice("Remove", "remove")
                                .addChoice("Check", "check"),
                        new OptionData(OptionType.USER, "user", "User to manage")
                                .setRequired(true),
                        new OptionData(OptionType.STRING, "type", "Whitelist type")
                                .setRequired(false)
                                .addChoice("Private", "private")
                                .addChoice("Partner", "partner")
                                .addChoice("Media", "media")
                                .addChoice("Access", "access"),
                        new OptionData(OptionType.INTEGER, "days", "Number of days to whitelist (optional)")
                                .setRequired(false)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!isAuthorized(event.getUser().getId())) {
            event.reply("You are not authorized to use this command.").setEphemeral(true).queue();
            return;
        }

        String action = Objects.requireNonNull(event.getOption("action")).getAsString();
        User targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
        String type = event.getOption("type") != null ? Objects.requireNonNull(event.getOption("type")).getAsString() : "access";
        Integer days = event.getOption("days") != null ? Objects.requireNonNull(event.getOption("days")).getAsInt() : null;

        switch (action) {
            case "add":
                Instant expirationTime = days != null ? Instant.now().plus(Duration.ofDays(days)) : null;
                DatabaseManager.updateUserWhitelist(targetUser.getId(), true, type, expirationTime);
                String response = "User " + targetUser.getAsTag() + " has been added to the whitelist with type: " + type;
                if (days != null) {
                    response += " for " + days + " days";
                }
                event.reply(response).setEphemeral(true).queue();
                break;
            case "remove":
                DatabaseManager.updateUserWhitelist(targetUser.getId(), false, null, null);
                event.reply("User " + targetUser.getAsTag() + " has been removed from the whitelist").setEphemeral(true).queue();
                break;
            case "check":
                Document user = DatabaseManager.getUser(targetUser.getId());
                boolean isWhitelisted = user != null && user.getBoolean("whitelisted", false);
                String whitelistType = user != null ? user.getString("whitelistType") : null;
                Instant expiration = user != null ? user.getDate("whitelistExpiration") != null ? user.getDate("whitelistExpiration").toInstant() : null : null;
                if (isWhitelisted) {
                    String expirationString = expiration != null ? " until " + expiration : " permanently";
                    event.reply("User " + targetUser.getAsTag() + " is whitelisted with type: " + whitelistType + expirationString).setEphemeral(true).queue();
                } else {
                    event.reply("User " + targetUser.getAsTag() + " is not whitelisted").setEphemeral(true).queue();
                }
                break;
            default:
                event.reply("Invalid action specified").setEphemeral(true).queue();
        }
    }
}