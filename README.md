# Elusion
Discord Bot &amp; API for Elusion.

### Planned Updates

- [ ] Make a config file so that the bot does not need to be rebuilt every time you want to change certain values.
- [ ] Allow the alt folder to be detected automatically depending on the folder the jar file is run from.
- [ ] Allow un-whitelisted users to generate an unbanned alt with ten invites.
- [ ] Add Javadocs (maybe).


I initially went with a modular approach to the bot but it got a bit out of hand, I intend to do a refactor sometime to make the code easier to understand.

## Installation

*Assumes you have a basic mongoDB server running on localhost:27017*

You MUST change these hardcoded values in the bot's code for it to work.

`Main.java`:
```java
String token = "TOKEN_HERE";
long guildId = 000L;
String mongoConnectionString = "mongodb://localhost:27017";
```

`CommandManager.java`
```java
allowedChannels.add(00000L);
```

`WhitelistCommand.java`
```java
private static final List<String> AUTHORIZED_USERS = List.of("00000");)
```
`SyncUsersCommand.java`
```java
private static final String AUTHORIZED_USER_ID = "000000";
```
*The sync users command only allows one user ID as it is generally a one time command.*

`GenerateCommand.java`
```java
private static final String MAIN_FOLDER = "C:\\Full\\Path\\To\\Your\\Main\\Folder";
private static final String BANNED_FOLDER = MAIN_FOLDER + "/banned";
private static final String UNBANNED_FOLDER = MAIN_FOLDER + "/unbanned";
private static final String USED_FOLDER = MAIN_FOLDER + "/used";
```
The folder hierarchy for the alts folder is important for the bot to work properly.

```
MAIN-FOLDER/
├── banned
├── unbanned
├── used/
├── ├── banned
├── ├── unbaned
```
The main banned and unbanned folders should contain all the text files.

## How to actually build and run the bot:

1. Clone the repository (or fork it to pull new changes easily).
2. In the main folder, depending on your system, run `gradlew.bat shadowJar` or `./gradlew shadowJar`.
3. Run the produced jar file that *should* be in the `build/libs` folder.
4. Run the jar with Java 21. You can download it [here](https://adoptopenjdk.net/).