var transformer = require("../transformer");

transformer.register( "hideImages", function( content ) {
    var minImageSize = 64;
    var images = content.querySelectorAll( 'img:not(.mwe-math-fallback-image-inline)' );
    for (var i = 0; i < images.length; i++) {
        var img = images[i];
        if (img.width < minImageSize && img.height < minImageSize) {
            continue;
        }
        // Just replace the src of the image with a placeholder image from our assets.
        img.src = "file:///android_asset/checkerboard.png";
        img.srcset = "";
    }
} );
