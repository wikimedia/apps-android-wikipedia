var transformer = require("../transformer");
var night = require("../night");

transformer.register( "addDarkModeStyles", function( content ) {
	if ( window.isNightMode ) {
		night.setImageBackgroundsForDarkMode ( content );
	}
} );