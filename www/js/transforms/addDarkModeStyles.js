var transformer = require("../transformer");
var dark = require("../dark");

transformer.register( "addDarkModeStyles", function( content ) {
	if ( window.isDarkMode ) {
		dark.setImageBackgroundsForDarkMode ( content );
	}
} );