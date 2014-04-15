var bridge = require("./bridge");
bridge.registerListener( "displayAttribution", function( payload ) {
    var directionality = document.getElementsByTagName( "html" )[0].classList.contains( "ui-rtl" ) ? "rtl" : "ltr";

    var lastUpdatedDiv = document.getElementById( "lastupdated" );
    lastUpdatedDiv.setAttribute( "dir", directionality );
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

bridge.registerListener( "replaceImageSrc", function( payload ) {
    var images = document.querySelectorAll( "img[src='" + payload.originalURL + "']" );
    for ( var i = 0; i < images.length; i++ ) {
        var img = images[i];
        img.setAttribute( "src", payload.newURL );
        img.setAttribute( "data-old-src", payload.originalURL );
    }
} );

bridge.registerListener( "hideEditButtons", function() {
    document.getElementsByTagName( "html" )[0].classList.add( "no-editing" );
} );
