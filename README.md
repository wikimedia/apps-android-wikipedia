See also https://git.wikimedia.org/summary/apps%2Fandroid%2Fjava-mwapi.git


== Updating icons from SVG ==

Many of our icons are maintained as SVG originals, rasterized to PNG at the
various output resolutions via a script. This rasterization is not part of
the main build process, so needs to be re-run when adding new icons.

Ensure you have librsvg and the 'rsvg-convert' command:

* On Ubuntu, run "sudo apt-get install librsvg2-bin"
* On Mac OS X using Homebrew, run "brew install librsvg"

In "wikipedia" project subdirectory, run:
* "./convertify.bash"

Original files from icon-sources/*.svg are rendered and copied into the res/
subdirectories. Note that they are not automatically added to git!

