package dev.sideloaded.elusion.command.impl;

import dev.sideloaded.elusion.Config;
import dev.sideloaded.elusion.command.SlashCommand;
import dev.sideloaded.elusion.database.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bson.Document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class GenerateCommand implements SlashCommand {
    private static final String MAIN_FOLDER = Config.getInstance().getMainFolderPath();
    private static final String BANNED_FOLDER = MAIN_FOLDER + "/banned";
    private static final String UNBANNED_FOLDER = MAIN_FOLDER + "/unbanned";
    private static final String USED_FOLDER = MAIN_FOLDER + "/used";
    private static final Map<String, Long> bannedAltCooldowns = new HashMap<>();
    private static final Map<String, Long> unbannedAltCooldowns = new HashMap<>();
    private static final Map<String, Integer> unbannedAltGenerationCount = new HashMap<>();
    private static final Map<String, Long> firstUnbannedAltGenerationTime = new HashMap<>();

    @Override
    public String getName() {
        return "generate";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("generate", "Generate an alt.")
                .addOptions(
                        new OptionData(OptionType.STRING, "type", "Type of alt to generate")
                                .setRequired(true)
                                .addChoice("Unbanned Alt", "unbanned")
                                .addChoice("Banned Alt", "banned")
                );
    }

    @Override
    public boolean isAuthorized(String userId) {
        return true;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String userId = event.getUser().getId();
        Document user = DatabaseManager.getUser(userId);
        String type = Objects.requireNonNull(event.getOption("type")).getAsString();

        boolean isWhitelisted = user.getBoolean("whitelisted", false);
        String whitelistType = user.getString("whitelistType");
        int invites = user.getInteger("invites", 0);
        int credits = user.getInteger("credits", 0);

        if (isWhitelisted) {
            if ("banned".equals(type)) {
                handleBannedAltGeneration(event, userId);
            } else if ("unbanned".equals(type)) {
                handleUnbannedAltGeneration(event, userId, whitelistType);
            }
        } else if ("banned".equals(type)) {
            if (invites >= 3 && credits >= 1) {
                if (generateAlt(event, type)) {
                    DatabaseManager.decrementField(userId, "credits");
                }
            } else if (invites < 3) {
                event.getHook().sendMessage("You need at least 3 invites to generate a banned alt. You currently have " + invites + " invites.").queue();
            } else {
                event.getHook().sendMessage("You don't have enough credits to generate a banned alt. You need 1 credit, but you have " + credits + " credits.").queue();
            }
        } else if ("unbanned".equals(type)) {
            if (credits >= 10) {
                if (generateAlt(event, type)) {
                    DatabaseManager.decrementField(userId, "credits", 10);
                    event.getHook().sendMessage("You have used 10 credits to generate an unbanned alt.").queue();
                }
            } else {
                event.getHook().sendMessage("You need at least 10 credits to generate an unbanned alt. You currently have " + credits + " credits.").queue();
            }
        } else {
            event.getHook().sendMessage("Invalid alt type specified.").queue();
        }
    }

    private void handleBannedAltGeneration(SlashCommandInteractionEvent event, String userId) {
        long currentTime = System.currentTimeMillis();
        Long lastGenerationTime = bannedAltCooldowns.get(userId);
        if (lastGenerationTime != null && currentTime - lastGenerationTime < 300000) { // 5 minutes
            long remainingTime = 300000 - (currentTime - lastGenerationTime);
            event.getHook().sendMessage("You need to wait " + formatTime(remainingTime) + " before generating another banned alt.").queue();
            return;
        }
        if (generateAlt(event, "banned")) {
            bannedAltCooldowns.put(userId, currentTime);
        }
    }

    private void handleUnbannedAltGeneration(SlashCommandInteractionEvent event, String userId, String whitelistType) {
        long currentTime = System.currentTimeMillis();
        Long lastGenerationTime = unbannedAltCooldowns.get(userId);
        Long firstGenerationTime = firstUnbannedAltGenerationTime.get(userId);
        int generationCount = unbannedAltGenerationCount.getOrDefault(userId, 0);

        long cooldownTime = getCooldownTime(whitelistType, generationCount);

        if (firstGenerationTime == null || currentTime - firstGenerationTime > 86400000) { // 24 hours
            firstUnbannedAltGenerationTime.put(userId, currentTime);
            unbannedAltGenerationCount.put(userId, 0);
            generationCount = 0;
        }

        if (lastGenerationTime != null && currentTime - lastGenerationTime < cooldownTime) {
            long remainingTime = cooldownTime - (currentTime - lastGenerationTime);
            event.getHook().sendMessage("You need to wait " + formatTime(remainingTime) + " before generating another unbanned alt.").queue();
            return;
        }

        if (generateAlt(event, "unbanned")) {
            unbannedAltCooldowns.put(userId, currentTime);
            unbannedAltGenerationCount.put(userId, generationCount + 1);
        }
    }

    private long getCooldownTime(String whitelistType, int generationCount) {
        switch (whitelistType) {
            case "private":
                return 0; 
            case "partner":
                return 900000; 
            case "media":
                return 900000 + (generationCount * 900000L);
            case "access":
            default:
                return 1800000 + (generationCount * 1800000L); 
        }
    }

    private boolean generateAlt(SlashCommandInteractionEvent event, String type) {
        try {
            String sourceFolder = "banned".equals(type) ? BANNED_FOLDER : UNBANNED_FOLDER;
            String destinationFolder = USED_FOLDER + "/" + type;

            File folder = new File(sourceFolder);
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (files == null || files.length == 0) {
                event.getHook().sendMessage("No " + type + " alts available.").queue();
                return false;
            }

            Random random = new Random();
            File randomFile = files[random.nextInt(files.length)];

            event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Here's your " + type + " alt!")
                    .addFiles(FileUpload.fromData(randomFile))
                    .queue(success -> {
                        moveFile(randomFile, destinationFolder);
                        DatabaseManager.incrementField(event.getUser().getId(), "altsGenerated");
                        event.getHook().sendMessage("Check your direct messages for your " + type + " alt.").queue();
                    }, failure -> event.getHook().sendMessage("Failed to send the alt via DM. Please make sure you have DMs enabled for this server.").queue()),
                       failure -> event.getHook().sendMessage("Failed to open a private channel. Please make sure you have DMs enabled for this server.").queue());

            return true;
        } catch (Exception e) {
            event.getHook().sendMessage("An error occurred while generating the " + type + " alt.").queue();
            e.printStackTrace();
            return false;
        }
    }

    private void moveFile(File file, String destinationFolder) {
        try {
            Path source = file.toPath();
            Path destination = getUniqueDestinationPath(destinationFolder, file.getName());
            Files.createDirectories(destination.getParent());
            Files.move(source, destination);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Path getUniqueDestinationPath(String destinationFolder, String fileName) {
        Path destinationPath = Paths.get(destinationFolder, fileName);
        if (!Files.exists(destinationPath)) {
            return destinationPath;
        }

        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        int counter = 1;

        while (true) {
            String newFileName = String.format("%s_%d%s", nameWithoutExtension, counter, extension);
            destinationPath = Paths.get(destinationFolder, newFileName);
            if (!Files.exists(destinationPath)) {
                return destinationPath;
            }
            counter++;
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        StringBuilder formattedTime = new StringBuilder();
        if (hours > 0) {
            formattedTime.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        }
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            formattedTime.append(seconds).append(" second").append(seconds != 1 ? "s" : "");
        }

        return formattedTime.toString().trim();
    }
}