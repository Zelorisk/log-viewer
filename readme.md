# log viewer

a minecraft fabric mod for 1.21.11 that displays real-time logs in a sleek, terminal-style gui overlay.

## features

- **real-time log capture** - intercepts all log4j2 messages from the game
- **terminal-style gui** - dark, semi-transparent overlay with colored log levels
- **filter by level** - toggle info/warn/error/debug logs with buttons
- **text search** - filter logs by searching for text
- **auto-scroll** - automatically scrolls to newest logs (toggleable)
- **draggable window** - click and drag the header to move
- **resizable** - drag the bottom-right corner to resize
- **export logs** - save filtered logs to a file

## controls

- `f7` - toggle log viewer gui
- `ctrl+l` - clear all logs
- `ctrl+f` - focus search box
- `ctrl+s` - toggle auto-scroll
- `esc` - close gui

## log level colors

- info - light gray
- warn - orange
- error - red
- debug - cyan

## installation

1. install fabric loader 0.18.1+ for minecraft 1.21.11
2. install fabric api 0.141.2+
3. drop `log-viewer-1.0.0.jar` into your mods folder
4. launch the game and press f7

## building

```bash
./gradlew build
```

the built jar will be in `build/libs/`

## requirements

- minecraft 1.21.11
- fabric loader 0.18.1+
- fabric api 0.141.2+1.21.11
- java 21
