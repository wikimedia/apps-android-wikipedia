var parseCSSColor = require("../lib/js/css-color-parser");
var bridge = require("./bridge");
var loader = require("./loader");
var util = require("./util");

function parseRgbValues( el, propertyName ) {
	var property = el.style[propertyName];
	var bits = parseCSSColor( property );
	if ( bits === null ) {
		return;
	}
	return [ parseInt( bits[0] ), parseInt( bits[1] ), parseInt( bits[2] )];
}

function invertColorProperty( el, propertyName ) {
	var rgb = parseRgbValues( el, propertyName );
	if ( rgb ) {
		el.style[propertyName] = 'rgb(' + (255 - rgb[0]) + ', ' + (255 - rgb[1]) + ', ' + (255 - rgb[2]) + ')';
	}
}

function halveRgbValues( el, propertyName ) {
	var rgb = parseRgbValues( el, propertyName );
	if ( rgb ) {
		el.style[propertyName] = 'rgb(' + Math.floor(rgb[0] / 2) + ', ' + Math.floor(rgb[1] / 2) + ', ' + Math.floor(rgb[2] / 2) + ')';
	}
}

var invertProperties = [ 'color', 'background-color', 'border-color' ];
function invertOneElement( el ) {
	var isInTable = util.hasAncestor( el, 'TABLE' );
	for ( var i = 0; i < invertProperties.length; i++ ) {
		if ( el.style[invertProperties[i]] ) {
			if ( isInTable ) {
				halveRgbValues( el, invertProperties[i] );
			} else {
				invertColorProperty( el, invertProperties[i] );
			}
		}
	}
}

function invertElement( el ) {
    // first, invert the colors of tables and other elements
	var allElements = el.querySelectorAll( '*[style]' );
	for ( var i = 0; i < allElements.length; i++ ) {
		invertOneElement( allElements[i] );
	}
    // and now, look for Math formula images, and invert them
    var mathImgs = el.querySelectorAll( "[class*='math-fallback']" );
    for ( i = 0; i < mathImgs.length; i++ ) {
        var mathImg = mathImgs[i];
        // KitKat and higher can use webkit to invert colors
        if (window.apiLevel >= 19) {
            mathImg.style.cssText = mathImg.style.cssText + ";-webkit-filter: invert(100%);";
        } else {
            // otherwise, just give it a mild background color
            mathImg.style.backgroundColor = "#ccc";
            // and give it a little padding, since the text is right up against the edge.
            mathImg.style.padding = "2px";
        }
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
