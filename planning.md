Mod loader; Fabric
For; Minecraft Java
Language; Java
Primary Objective: Trigger client-side OS commands 
safely based on in-game conditions and events 
(e.g., changing dimensions, health changes, or redstone triggers).

Pythemc IO mod is about;
making a mod that can link in game events to commands
the real OS can execute.

Example;
if player goes into the nether, the OS should run
"python3 lightsnetherenv.py"

Security measures:
1. Must run completely on client side; no server host
should be able to run commands on someone elses PC.
2. The parser strips all non-alphanumeric characters
(quotes, backticks, dollar signs, semicolons, pipes, etc.)
and replaces them with spaces before checking, so bypasses
like sh -c "sudo rm -rf /" are caught.

Blocked commands:
- Destructive: rm, rmdir, del, erase, dd, format, mkfs
- Permission: chmod, chown, chgrp, sudo, su, passwd
- Process: kill, killall, pkill, halt, shutdown, reboot
- System: systemctl, service, crontab, at, mount, umount, fdisk, parted
- Network: curl, wget (prevents downloading arbitrary content)
- Shell: powershell, pwsh (too powerful, .NET access)

Blocked flags:
- --no-preserve-root, -rf, -fr, --force, --recursive

Blocked patterns (substring check on full command):
- rm -, rmdir -, sudo , su -, chmod , chown
- dd if=, dd of=, > /dev/, < /dev/
- /etc/passwd, /etc/shadow

NOT blocked (safe to use):
- bash, sh, zsh, cmd (just shells, the command inside is what matters)
- pip, npm, cargo, yarn (package managers, constantly scanned for malware)
- Pipes and chaining (|, &&, ;) — the individual commands are still checked

If detected, immediately fail the process.
Pythemc IO mod must always run in the same 
user privileges.

3. Pythemc IO mod should be allowed to be completely
disabled through /pythemcio disable (re-enable with /pythemcio enable)

Command syntax:
/pythemcio add -o <event> <command>                    → fires for all occurrences
/pythemcio add -o <event> filter <argument> <command>  → fires only when filter matches
/pythemcio remove <id>                                 → remove a trigger by ID
/pythemcio list                                        → list all triggers
/pythemcio clear                                       → remove all triggers
/pythemcio disable [-i|-o]                             → disable triggers/scripts
/pythemcio enable [-i|-o]                              → enable triggers/scripts
/pythemcio scope <id> global|local                     → change trigger scope
/pythemcio help                                        → show help

Variable substitution in commands:
$CONTEXT — the context string (item/block/entity name, message text)
$EVENT — the event name
$ITEM — alias for $CONTEXT
$BLOCK — alias for $CONTEXT
$ENTITY — alias for $CONTEXT

Filterable events (10):
- using_item [item]          → context: item name (e.g. minecraft:bow)
- item_pickup [item]         → context: item name
- item_drop [item]           → context: item name
- block_break [block]        → context: block name (e.g. minecraft:diamond_ore)
- block_place [block]        → context: block name
- player_attack [entity]     → context: entity type (e.g. minecraft:creeper)
- chat_message [keyword]     → context: message text
- dimension_change [dim]     → context: nether, end, or overworld
- death [cause]              → context: entity type or damage cause (e.g. minecraft:creeper, inFire)
- time_change [time]         → context: day or night

Non-filterable events (15):
player_join, player_leave, health_change,
food_change, armor_change, xp_change, respawn,
sleep, wake_up, on_fire, in_water, sprint,
elytra, sneak, redstone_signal

Cooldown: 3 second debounce per command to prevent spam
from rapid-fire events (health regen, food, etc.)

Design measures;
1. Commands are registered in-game via /pythemcio. 
2. The parser CHECKS the command written using the security
measures above. All non-alphanumeric characters are stripped
before validation to prevent bypass attacks.
3. Triggers persist to config/pythemcio/triggers.json
via Gson, surviving game restarts.
