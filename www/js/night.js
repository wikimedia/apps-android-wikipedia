var parseCSSColor = require("../lib/js/css-color-parser");
var bridge = require("./bridge");
var loader = require("./loader");

function invertColorProperty( el, propertyName ) {
	var property = el.style[propertyName];
	console.log( JSON.stringify( parseCSSColor ) );
	var bits = parseCSSColor( property );
	if ( bits === null ) {
		// We couldn't parse the color, nevermind
		return;
	}
	var r = parseInt( bits[0] ), g = parseInt( bits[1] ), b = parseInt( bits[2] );
	el.style[propertyName] = 'rgb(' + (255 - r) + ', ' + (255 - g) + ', ' + (255 - b ) + ')';
}

var invertProperties = [ 'color', 'background-color', 'border-color' ];
function invertOneElement( el ) {
	for ( var i = 0; i < invertProperties.length; i++ ) {
		if ( el.style[invertProperties[i]] ) {
			invertColorProperty( el, invertProperties[i] );
		}
	}
}

function invertElement( el ) {
	var allElements = el.querySelectorAll( '*[style]' );
	console.log( 'rewriting ' + allElements.length + ' elements' );
	for ( var i = 0; i < allElements.length; i++ ) {
		invertOneElement( allElements[i] );
	}
}

function toggle( nightCSSURL, hasPageLoaded ) {
	window.isNightMode = !window.isNightMode;

	// Remove the <style> tag if it exists, add it otherwise
	var nightStyle = document.querySelector( "link[href='" + nightCSSURL + "']" );
	console.log( nightCSSURL );
	if ( nightStyle ) {
		nightStyle.parentElement.removeChild( nightStyle );
	} else {
		loader.addStyleLink( nightCSSURL );
	}

	if ( hasPageLoaded ) {
		// If we are doing this before the page has loaded, no need to swap colors ourselves
		// If we are doing this after, that means the transforms in transformers.js won't run
		// And we have to do this ourselves
		invertElement( document.querySelector( '.content' ) );
	}
}

bridge.registerListener( 'toggleNightMode', function( payload ) {
	toggle( payload.nightStyleBundle.style_paths[0], payload.hasPageLoaded );
} );

module.exports = {
	invertElement: invertElement
};
