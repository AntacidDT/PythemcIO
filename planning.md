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
2. Parser may not allow file/directory editing commands
    (touch, nano, etc), no disk formatters (dd, format), 
    no package managers, no recursive actions, no 
    chaining or pipes (&&, | etc). If a script is ran,
    that script must be checked and libraries like
    shutil or os may not be allowed. Powershell may 
    NEVER be used. No permission changing commands should
    be ran. If detected, immediatly fail the process.
    Pythemc IO mod must always run in the same 
    user priviliges and sudo should not be allowed.
    --no-preserve-root must also be blocked and anything
    recursive or destructive.
3. Pythemc IO mod should be allowed to be completely
    disabled through a command or F5 + P

Design measures;
1. we should use commands (in MC). Later we can add GUI 
but early should be ingame commands based. Something like
"/pythemcio run "python3 file.py" if player.dimension = Nether"
that command is specifically done ingame. 
2. The parser CHECKS the command written, and only can allow
execution if the command agrees with Security measure Nr. 2 
