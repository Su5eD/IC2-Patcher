# IC2 Patcher
Fixes several bugs in IndustrialCraft2 using [binary patches](http://javaxdelta.sourceforge.net).

Note that you **may NOT distribute** any decompiled code generated by this tool.
For more information, see [IC2's license](https://forum.industrial-craft.net/thread/9843-mc-1-7-ic%C2%B2-v-2-1-x-2-2-x-experimental/).

### List of patches
You can find lists of patches for each supported IC2 Version [here]().

#### Currently supported versions:
- IC2-2.8.221 and above
- IC2-2.8.164

# Contribution

## Initial Setup
1. Clone the repository.
2. Run the `Setup IC2 Source` gradle task found in `ic2 patcher`.   
   This will set up both `IC2-Base` and `IC2-Patched` projects, which contain clean and patched code respectively.  
   Note that while this code will compile, it's *highly unstable* and might not work on your OS / IDE.

## Updating the project
To apply changes pulled from the GitHub, simply execute `Setup IC2 Source` task again.

_Important Note:_ If the latest pull contains changes to patches for the `Base` project, 
before running the `Setup IC2 Source` task, you need to execute `Setup Source` of the `Base` project itself. 

<hr>

## Editing IC2 code base
<i>Important, changes to the IC2 source code are not automatically saved!</i>

Both `Base` and `Patched` project have `Generate Patches` tasks, 
that are used to generate patches based on the current changes.

Those patches will later be used for the IC2 Source setup and generation of the binary patches.

### Modifying Base Project
Base project is meant to be pure "decompilation" of the IC2. The code should be as close to the original as possible.

Acceptable changes: Code Style changes and fixes of errors from the Decompilation process.

### Modifying Patched Project
Patched Project should only contain the actual fixes, that are going to be applied to the IC2 on runtime. For cosmetic changes to the code base, edit the Base project instead.

Acceptable changes: Patches meant to be applied on runtime.

### Working on an unsupported version of the IC2
To start working on a new version of the IC2,
you need to create folders for the patches in both `Base` and `Patched` projects.

The folder name is required to contain the version range, for which those patches are applicable.<br>
Example: 
- `[2.8.164]` -> Those patches support only version 2.8.164
- `[2.8.221,+]` -> Those patches support version 2.8.221 and above.
- `[2.8.170,2.8.180]` -> Those patches support versions from 2.8.170 to 2.8.180.

It is recommended to copy the patches from the closets,
already supported version and start from there. 
Do note that most likely those patches _will_ fail to apply correctly 
and will need to be fixed / reimplemented.


### Multi version support
IC2 Patcher is created to support multiple versions of the IC2. 
It is recommended to work on the patch on the oldest supported version of the IC2,
that the patch is applicable to, and then port the changes to the newer versions — if applicable.
<hr>

## Building & Exporting
1. Run the `Generate Binary Patches` task of the `Patched` project for each version you have done changes to.
2. Run `Release Jar` of the `Patcher` project to build the mod.<br>
   The resulting jar will be located in the `build/libs` folder.  
   Remember that the jar built by the `jar` task **will NOT work** outside the dev environment.

<div>
  <img src="https://upload.wikimedia.org/wikipedia/commons/e/eb/PD-icon-black.svg" align="right" width="50" alt="The Unlicense Logo">
</div>
<h2 align="left">Licensing</h2>

All code is licensed under **The Unlicense**, except for `mods.su5ed.ic2patcher.asm.BinPatchManager`, which is a modified version
of MinecraftForge's [ClassPatchManager](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/src/main/java/net/minecraftforge/fml/common/patcher/ClassPatchManager.java) class, and is licensed under the **GNU Lesser General Public License version 2.1**
