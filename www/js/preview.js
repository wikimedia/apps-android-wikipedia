var bridge = require("./bridge");

bridge.registerListener( "displayPreviewHTML", function( payload ) {
    var content = document.getElementById( "content" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.html;
} );
