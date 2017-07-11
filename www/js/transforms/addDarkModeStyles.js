var transformer = require("../transformer");
var dark = require("../dark");

transformer.register( "addDarkModeStyles", function( content ) {
    var html = document.getElementsByTagName( 'html' )[0];
    html.classList.remove( 'dark' );
    if ( window.isDarkMode ) {
        html.classList.add( 'dark' );
        dark.setImageBackgroundsForDarkMode ( content );
    }
} );