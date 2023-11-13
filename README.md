# poyo

A fun Discord-Bot written in Java ☕️

## Features

### 🎶 Music Commands
- Play your favorite tunes by simply providing the bot with links!
- Asynchronously loads and plays music for a seamless experience.

### 🚀 Intro Feature
- The bot can automatically join a voice channel when it detects a specified user.
- Enjoy faster playback by playing a track from a cache.
- Make the Bot use a fun username automatically

### 🔒 Blacklist
- Configure a blacklist in `config.yaml` to prevent specific users from calling certain commands.
- Keep control and ensure a peaceful server environment.

## Commands

- **Play Music:**
  ```
  /play <music_link>
  ```

- **Get the current Playlist:**
  ```
  /playlist
  ```

- **Skip a Song:**
  ```
  /skip
  ```

- **Jump to a specific Song in the Playlist:**
  ```
  /jump <song_index>
  ```

- **Change the Volume:**
  ```
  /volume <value 0-100>
  ```
- **Stop the Bot:**
  ```
  /stop
  ```

## Configuration

Edit the `config.yaml` file to customize your bot's behavior and set up blacklists.

```yaml
BLACK_LIST:
  <command_name>: <List of blacklisted Users>
DISCORD_TOKEN: <your discord Token>
PLAY_NICKNAME: <a funny Nickname>
VIP_TRACKS: {<UserID>: <a youtube link>}
```

## Support & Issues

If you encounter any issues or have questions, feel free to [create an issue](https://github.com/pvlov/poyo.git/issues).

Happy botting! 🤖🎉