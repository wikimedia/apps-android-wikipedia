var transformer = require("../transformer");
var utilities = require("../utilities");

var maxStretchRatioAllowedBeforeRequestingHigherResolution = 1.3;

function widenAncestors (el) {
    while ((el = el.parentElement) && !el.classList.contains('content_block')) {
        // Only widen if there was a width setting. Keeps changes minimal.
        if (el.style.width) {
            el.style.width = '100%';
        }
        if (el.style.maxWidth) {
            el.style.maxWidth = '100%';
        }
        if (el.style.float) {
            el.style.float = 'none';
        }
    }
}

function shouldWidenImage(image) {
    if (
        image.width >= 64 &&
        image.hasAttribute('srcset') &&
        !image.hasAttribute('hasOverflowXContainer') &&
        image.parentNode.className === "image" &&
        !utilities.isNestedInTable(image)
        ) {
        return true;
    } else {
        return false;
    }
}

function makeRoomForImageWidening(image) {
    // Expand containment so css wideImageOverride width percentages can take effect.
    widenAncestors (image);

    // Remove width and height attributes so wideImageOverride width percentages can take effect.
    image.removeAttribute("width");
    image.removeAttribute("height");
}

function getStretchRatio(image) {
    var widthControllingDiv = utilities.firstDivAncestor(image);
    if (widthControllingDiv) {
        return (widthControllingDiv.offsetWidth / image.naturalWidth);
    }
    return 1.0;
}

function useHigherResolutionImageSrcFromSrcsetIfNecessary(image) {
    if (image.getAttribute('srcset')) {
        var stretchRatio = getStretchRatio(image);
        if (stretchRatio > maxStretchRatioAllowedBeforeRequestingHigherResolution) {
            var srcsetDict = utilities.getDictionaryFromSrcset(image.getAttribute('srcset'));
            /*
            Grab the highest res url from srcset - avoids the complexity of parsing urls
            to retrieve variants - which can get tricky - canonicals have different paths
            than size variants
            */
            var largestSrcsetDictKey = Object.keys(srcsetDict).reduce(function(a, b) {
              return a > b ? a : b;
            });

            image.src = srcsetDict[largestSrcsetDictKey];
        }
    }
}

function widenImage(image) {
    makeRoomForImageWidening (image);
    image.classList.add("wideImageOverride");
    useHigherResolutionImageSrcFromSrcsetIfNecessary(image);
}

function maybeWidenImage() {
    var image = this;
    image.removeEventListener('load', maybeWidenImage, false);
    if (shouldWidenImage(image)) {
        widenImage(image);
    }
}

transformer.register( "widenImages", function( content ) {
    var images = content.querySelectorAll( 'img' );
    for ( var i = 0; i < images.length; i++ ) {
        // Load event used so images w/o style or inline width/height
        // attributes can still have their size determined reliably.
        images[i].addEventListener('load', maybeWidenImage, false);
    }
} );