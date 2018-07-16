var transformer = require("../transformer");

/* TODO: remove once implemented in content service. */

transformer.register( "fixAudio", function( content ) {
    var videoTags = content.querySelectorAll( 'video' );
    // Find audio files masquerading as <video> tags, and fix them by
    // replacing them with <audio> tags.
    for ( var i = 0; i < videoTags.length; i++ ) {
        var videoTag = videoTags[i];
        var resource = videoTag.getAttribute( 'resource' );
        // If the 'resource' attribute is an .ogg file, then it's audio.
        if ( resource && resource.indexOf( '.ogg' ) > 0 ) {
            videoTag.removeAttribute( 'poster' );

            // Make a proper <audio> tag, and replace the <video> tag with it.
            var audioTag = document.createElement( 'audio' );
            // Copy the children
            while (videoTag.firstChild) {
                audioTag.appendChild( videoTag.firstChild ); // Increments the child
            }
            // Copy the attributes
            for (var index = videoTag.attributes.length - 1; index >= 0; --index) {
                audioTag.attributes.setNamedItem(videoTag.attributes[index].cloneNode());
            }
            // Replace it
            videoTag.parentNode.replaceChild(audioTag, videoTag);
        }
    }
} );
