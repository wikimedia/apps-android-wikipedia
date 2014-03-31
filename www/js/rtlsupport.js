var bridge = require("./bridge");

bridge.registerListener( "setDirectionality", function( payload ) {
    var html = document.getElementsByTagName( "html" )[0];
    html.setAttribute( "dir", payload.contentDirection );
    html.classList.add( "content-" + payload.contentDirection );
    html.classList.add( "ui-" + payload.uiDirection );
} );
