var bridge = require("./bridge");
bridge.registerListener( "displayAttribution", function( payload ) {
    var lastUpdatedA = document.getElementById( "lastupdated" );
    lastUpdatedA.innerText = payload.historyText;
    lastUpdatedA.href = payload.historyTarget;
    var licenseText = document.getElementById( "licensetext" );
    licenseText.innerHTML = payload.licenseHTML;
});

bridge.registerListener( "requestImagesList", function () {
    var imageURLs = [];
    var images = document.querySelectorAll( "img" );
    for ( var i = 0; i < images.length; i++ ) {
        imageURLs.push( images[i].src );
    }
    bridge.sendMessage( "imagesListResponse", { "images": imageURLs });
} );