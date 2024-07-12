package dev.sideloaded.elusion.command.impl;

import dev.sideloaded.elusion.Config;
import dev.sideloaded.elusion.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.io.File;
import java.util.Objects;

public class StockCommand implements SlashCommand {

    private static final String MAIN_FOLDER = Config.getInstance().getMainFolderPath();
    private static final String BANNED_FOLDER = MAIN_FOLDER + "/banned";
    private static final String UNBANNED_FOLDER = MAIN_FOLDER + "/unbanned";

    @Override
    public String getName() {
        return "stock";
    }

    @Override
    public boolean isAuthorized(String userId) {
        return true;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("stock", "Check the current stock of alts");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int bannedStock = countFiles(BANNED_FOLDER);
        int unbannedStock = countFiles(UNBANNED_FOLDER);

        MessageEmbed embed = createStockEmbed(bannedStock, unbannedStock);
        event.replyEmbeds(embed).queue();
    }

    private int countFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        return files != null ? files.length : 0;
    }

    private MessageEmbed createStockEmbed(int bannedStock, int unbannedStock) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Current Alt Stock");
        embedBuilder.setColor(Color.BLUE);
        embedBuilder.addField("Banned Alts", String.valueOf(bannedStock), true);
        embedBuilder.addField("Unbanned Alts", String.valueOf(unbannedStock), true);
        embedBuilder.addField("Total Stock", String.valueOf(bannedStock + unbannedStock), false);
        embedBuilder.setFooter("Last updated: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return embedBuilder.build();
    }
}