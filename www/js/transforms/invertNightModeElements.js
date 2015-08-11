var transformer = require("../transformer");
var night = require("../night");

transformer.register( "invertNightModeElements", function( content ) {
	if ( window.isNightMode ) {
		night.setImageBackgroundsForDarkMode ( content );
	}
} );