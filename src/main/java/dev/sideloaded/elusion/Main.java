package dev.sideloaded.elusion;

import dev.sideloaded.elusion.command.CommandManager;
import dev.sideloaded.elusion.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bson.Document;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends ListenerAdapter {
    private static final Map<String, Invite> inviteCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        Config config = Config.getInstance();
        String token = config.getToken();
        long guildId = config.getGuildId();
        String mongoConnectionString = config.getMongoConnectionString();

        
        if (token == null || mongoConnectionString == null) {
            throw new RuntimeException("Missing required configuration values: token or mongoConnectionString");
        }

        DatabaseManager.connect(mongoConnectionString);

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_INVITES)
                .addEventListeners(new Main())
                .build();

        try {
            jda.awaitReady();
            CommandManager commandManager = new CommandManager();
            commandManager.registerCommands(jda, guildId);

            jda.getGuilds().forEach(guild ->
                    guild.retrieveInvites().queue(invites ->
                            invites.forEach(invite -> inviteCache.put(invite.getCode(), invite))
                    )
            );

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    DatabaseManager.removeExpiredWhitelists();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::close));
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        String discordId = event.getMember().getId();
        String discordName = event.getMember().getUser().getName();
        DatabaseManager.addOrUpdateUser(discordId, discordName, null);

        event.getGuild().retrieveInvites().queue(currentInvites -> {
            for (Invite invite : currentInvites) {
                Invite cachedInvite = inviteCache.get(invite.getCode());
                if (cachedInvite != null && invite.getUses() > cachedInvite.getUses()) {
                    String inviterId = Objects.requireNonNull(invite.getInviter()).getId();
                    DatabaseManager.addCredit(inviterId);
                    DatabaseManager.updateUser(discordId, "invitedBy", inviterId);
                    inviteCache.put(invite.getCode(), invite);
                    break;
                }
            }
        });
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        String discordId = Objects.requireNonNull(event.getMember()).getId();
        Document user = DatabaseManager.getUser(discordId);
        if (user != null) {
            String inviterId = user.getString("invitedBy");
            if (inviterId != null) {
                DatabaseManager.removeCredit(inviterId);
                DatabaseManager.decrementField(inviterId, "invites");
            }
        }
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        inviteCache.put(event.getCode(), event.getInvite());
    }

    @Override
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        inviteCache.remove(event.getCode());
    }


}