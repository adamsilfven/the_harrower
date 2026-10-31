# Hollow Fragment 1.0.1 — rendering repair

## Finding and correction

The user's Minecraft screenshot of 1.0.0 shows the missing-model cube in the hand and hotbar. The model contains 38 elements whose `faces` maps are empty. Minecraft 1.21.1's actual BlockModel parser rejects the original file with `Expected between 1 and 6 unique faces, got 0`. The earlier build/archive checks did not detect this client-format restriction.

Removed only those 38 empty elements: all 155 visible elements, 276 faces, their positions, texture references, and seven display transforms remain intact. Empty elements supplied no rendered surfaces. Both original PNGs remain byte-for-byte unchanged.

The original face UVs were expressed as coordinates on the 32×32 image. Minecraft model UVs use a 0–16 range regardless of image resolution; the `texture_size` metadata does not change that. Dividing UV values by two correctly selects the intended original palette swatches. Every one of the 276 face UV centers now samples an opaque swatch. This repairs the existing design's resource encoding without redesigning it.

Version is now 1.0.1 in the build and mod metadata. Item registration, maximum stack of 16, name, lore, Java implementation, display transforms, and gameplay behavior are unchanged. LC Core remains untouched. No summoning or catalyst consumption was added.

## Validation

- Clean build with Java 21.0.13+9, Gradle 8.10.2, ModDevGradle 2.0.148, and NeoForge 21.1.259: PASS. Dependencies came from the local cache.
- Actual Minecraft/NeoForge BlockModel parser regression test: PASS. It reproduces the original parse exception, then accepts the repaired file with 155 elements and 276 faces. This runs automatically through Gradle `check` and `build`; the Java harness and original failing fixture are included in source.
- Packaged JAR ZIP integrity, mod metadata/version, Java 21 class files, registration resources and translated name/lore: PASS.
- Packaged model equals the validated source model; texture references resolve inside the JAR: PASS.
- Both PNGs and all display transforms compared against the supplied original standalone source: unchanged. All visible positions/faces remain, with UVs divided by two: PASS.

The 1.0.0 addon previously passed a production dedicated-server registration smoke test; that test did not exercise client models and is not presented as a new 1.0.1 runtime test. No graphical Minecraft client was launched for 1.0.1. Actual rendering, tooltip appearance, and combined LC Core runtime behavior still require the user's final visual check. The new parser test verifies the specific rejection shown by the screenshot; it is not a claim of a full client render test.

## Install and final check

Remove `hollow-fragment-addon-1.0.0.jar` and install `hollow-fragment-addon-1.0.1.jar` in its place. Keep the existing LC Core and other mods. Restart Minecraft, then run:

```mcfunction
/give @s hollow_fragment_addon:hollow_fragment
```

Check hotbar/inventory, first-person and third-person hands, and the dropped item together. Confirm the name **Hollow Fragment** and lore **It weighs nothing. The ground beneath it disagrees.** No Cairn behavior is expected.
