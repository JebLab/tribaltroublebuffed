# Releasing

Tribal Trouble Buffed is released from `revamp` as GitHub releases on [JebLab/tribaltroublebuffed](https://github.com/JebLab/tribaltroublebuffed/releases). Pushing a tag `v<major>.<minor>.<patch>` runs [.github/workflows/build.yml](../.github/workflows/build.yml): it builds and tests on Windows and Linux, builds the Windows package (`tt:packageWindows`, a self-contained jpackage app image) and publishes a release with the package attached as `TribalTroubleBuffed-<tag>-windows.zip`. Until 1.0 every release is marked as a pre-release. itch.io, Flathub, winget and macOS/Linux packages come with M22 and are Josh's to approve ([DECISIONS.md](DECISIONS.md), D-02).

Resurrected's pipeline (Steam, itch.io, its website, a `release` branch and a promote step) was removed with its workflows in M3; it is documented in upstream's `docs/releasing.md`.

## Cutting a release

1. Check that CI is green on `revamp`.
2. Write the notes in `docs/release-notes/v<version>.md` (the workflow uses them; without the file GitHub generates notes from the merged pull requests) and commit them.
3. Set `version = "<version>"` in the root `build.gradle.kts` if it does not already name this release, and commit.
4. Tag and push:

   ```
   git tag -a v0.1.0 -m "Tribal Trouble Buffed 0.1.0"
   git push origin v0.1.0
   ```

5. Watch the run (`gh run watch`). When it finishes, the release page has the zip.
6. Set `version` in `build.gradle.kts` to the next planned release.

To rebuild the package of an existing tag, re-run the workflow for that tag: the release job uploads the zip again instead of creating a second release.

## Versioning

- **Releases** are `v<major>.<minor>.<patch>` tags. Before 1.0, a minor version is a milestone drop with new content or features and a patch is fixes only.
- **The in-game version** (About screen, log files) is `v<version>-<API_VERSION>.<SIM_VERSION>`. `<version>` comes from `git describe`: on a tagged commit it is the tag (`0.1.0`), later it says how far past the tag the build is (`0.1.0-3-gabc1234`), and without a reachable tag it is the `version` in `build.gradle.kts` plus `-dev`. `common/build.gradle.kts` generates it into `BuildInfo.java`.
- **`SIM_VERSION`** (`common/src/main/java/com/oddlabs/util/Compatibility.java`) changes whenever the simulation does: model, pathfinding, behaviours, landscape generation, AI, or ruleset numbers. Replays, save games (M13) and lockstep peers are only valid between builds with the same value. The headless golden traces record it, and their test fails until the traces are regenerated after a bump ([testing.md](testing.md)).
- **`API_VERSION`** is the inherited matchmaking wire version. Multiplayer is parked, so it does not change here.
