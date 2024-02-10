# Inertia Anti Cheat

![Stop people from using unwanted mods on your server!](https://cdn.modrinth.com/data/cached_images/1028d552bad40c52fd94cd12eefbddc17cbe4826.png)

![Require players to use your modpack on your server!](https://cdn.modrinth.com/data/cached_images/0487061e47d8ba4f102d479f853a0242315a86fe.png)

*This was heavily inspired and named after https://www.curseforge.com/minecraft/mc-mods/kinetic-anti-cheat*

## How does it work?

When someone joins your server, the server mod will ask for the client's mod list.

If the client does not respond to this request (most likely because the client mod is not installed), the server will kick the player.          

If the client mod responds, the server mod will check the mod list against a configurable blacklist/whitelist, or a hash for mod packs! It will kick the player if the mod list is not acceptable.

## Is this foolproof?

*Probably not.* This mod was only designed to combat script kiddies, and does not use any special method to check the verity of the mod list. In short, it would take a bit of effort to bypass this, but it will stop most regular cheaters from joining.

At the moment, resource packs are not checked. This allows people to use advantageous resource packs like Xray resource packs on the server. You will need to install an anti-xray mod yourself.

## Installation

Download the mod from [Modrinth](https://modrinth.com/mod/inertiaanticheat).

Check [here](https://github.com/DiffuseHyperion/InertiaAntiCheat/wiki) for more details after downloading the mod.

## Permissions

You are free to use this in modpacks or wherever you like!

I would appreciate it if you could give a link back to this page or the project's github.
