var bridge = require( "./bridge" );
bridge.registerListener( "displayAttribution", function( payload ) {
    var attributionDiv = document.getElementById( "attribution" );
    attributionDiv.setAttribute( "dir", window.directionality );
    var lastUpdatedA = document.getElementById( "lastupdated" );
    lastUpdatedA.innerText = payload.historyText;
    lastUpdatedA.href = payload.historyTarget;
    var licenseText = document.getElementById( "licensetext" );
    licenseText.innerHTML = payload.licenseHTML;
});

bridge.registerListener( "requestImagesList", function() {
    var imageURLs = [];
    var images = document.querySelectorAll( "img" );
    for ( var i = 0; i < images.length; i++ ) {
        imageURLs.push( images[i].src );
    }
    bridge.sendMessage( "imagesListResponse", { "images": imageURLs });
} );

// reusing this function
function replaceImageSrc( payload ) {
    var images = document.querySelectorAll( "img[src='" + payload.originalURL + "']" );
    for ( var i = 0; i < images.length; i++ ) {
        var img = images[i];
        img.setAttribute( "src", payload.newURL );
        img.setAttribute( "data-old-src", payload.originalURL );
    }
}
bridge.registerListener( "replaceImageSrc", replaceImageSrc );

bridge.registerListener( "replaceImageSources", function( payload ) {
    for ( var i = 0; i < payload.img_map.length; i++ ) {
        replaceImageSrc( payload.img_map[i] );
    }
} );

bridge.registerListener( "hideEditButtons", function() {
    document.getElementsByTagName( "html" )[0].classList.add( "no-editing" );
} );

bridge.registerListener( "setPageProtected", function() {
    document.getElementsByTagName( "html" )[0].classList.add( "page-protected" );
} );

/**
 * Message sent when the current page is determined to be the main page of a wiki.
 *
 * Should remove all edit icons, and in the future also other changes.
 *
 * No payload.
 */
bridge.registerListener( "setMainPage", function() {
    // Wrap .content in #mainpage. Differs from MF which wraps #mainpage in .content
    var content = document.getElementById( "content" );
    var mainpage = document.createElement( "div" );
    mainpage.setAttribute( "id", "mainpage" );

    document.body.insertBefore( mainpage, content.nextSibling );

    mainpage.appendChild( content );

} );
