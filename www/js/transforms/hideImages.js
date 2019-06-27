var transformer = require("../transformer");

transformer.register( "hideImages", function( content ) {
    var minImageSize = 64;
    var images = content.querySelectorAll( 'img:not(.mwe-math-fallback-image-inline)' );
    for (var i = 0; i < images.length; i++) {
        var img = images[i];
        if (img.width < minImageSize && img.height < minImageSize) {
            continue;
        }
        // Just replace the src of the image with a background color
        img.src = "";
    switch(window.theme){
        case 1:
        case 2: img.parentElement.style.backgroundColor = "#27292d";
                break;
        case 3: img.parentElement.style.backgroundColor = "#f0e6d6";
                break;
        default: img.parentElement.style.backgroundColor = "#f8f9fa";
        }
        img.srcset = "";
    }
} );
