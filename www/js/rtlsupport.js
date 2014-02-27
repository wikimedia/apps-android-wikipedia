var bridge = require("./bridge");

bridge.registerListener( "setDirectionality", function( payload ) {
    document.getElementsByTagName( "html" )[0].setAttribute( "dir", payload.dir );
} );