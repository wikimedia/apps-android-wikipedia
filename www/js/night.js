var bridge = require("./bridge");
var loader = require("./loader");
var util = require("./util");

function setImageBackgroundsForDarkMode( content ) {
	var allImgs = content.querySelectorAll( 'img' );
	for ( var i = 0; i < allImgs.length; i++ ) {
		if ( !util.ancestorHasStyleProperty( allImgs[i], 'background-color' ) ) {
			allImgs[i].style.background = '#fff';
		}
	}
	// and now, look for Math formula images, and invert them
	var mathImgs = content.querySelectorAll( "[class*='math-fallback']" );
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
		setImageBackgroundsForDarkMode( document.querySelector( '.content' ) );
	}
}

bridge.registerListener( 'toggleNightMode', function( payload ) {
	toggle( payload.nightStyleBundle.style_paths[0], payload.hasPageLoaded );
} );

module.exports = {
	setImageBackgroundsForDarkMode: setImageBackgroundsForDarkMode
};
