var bridge = require("./bridge");
var transformer = require("./transformer");

// Move infobox to the bottom of the lead section
transformer.register( "leadSection", function( leadContent ) {
    var infobox = leadContent.querySelector( "table.infobox" );
    if ( infobox ) {
        infobox.parentNode.removeChild( infobox );
        var pTags = leadContent.getElementsByTagName( "p" );
        if ( pTags.length ) {
            pTags[0].appendChild( infobox );
        } else {
            leadContent.appendChild( infobox );
        }
    }
    return leadContent;
} );

// Use locally cached images as fallback in saved pages
transformer.register( "section", function( content ) {
    var images = content.querySelectorAll( "img" );
    function onError() {
        var img = event.target;
        // Only work on http or https URLs. If we do not have this check, we might go on an infinte loop
        if ( img.src.substring( 0, 4 ) === "http" )  {
            // if it is already not a file URL!
            var resp = bridge.sendMessage( "imageUrlToFilePath", { "imageUrl": img.src } );
            console.log( "new filepath is " + resp.filePath );
            img.src = "file://" + resp.filePath;
        }
    }
    for ( var i = 0; i < images.length; i++ ) {
        images[i].onerror = onError;
    }
    return content;
} );
