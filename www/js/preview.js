var bridge = require("./bridge");
var pagelib = require("wikimedia-page-library");

bridge.registerListener( "displayPreviewHTML", function( payload ) {
    var content = document.getElementById( "content" );
    document.head.getElementsByTagName("base")[0].setAttribute("href", payload.siteBaseUrl);
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.html;

    // todo: remove this when editing page preview uses the same bundle as reading.
    if ( content ) {
        pagelib.ThemeTransform.classifyElements( content );
    }
} );