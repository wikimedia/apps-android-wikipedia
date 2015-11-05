var transformer = require("../../transformer");

transformer.register( "hideRedLinks", function( content ) {
	var redLinks = content.querySelectorAll( 'a.new' );
	for ( var i = 0; i < redLinks.length; i++ ) {
		var redLink = redLinks[i];
		var replacementSpan = document.createElement( 'span' );
		replacementSpan.innerHTML = redLink.innerHTML;
		replacementSpan.setAttribute( 'class', redLink.getAttribute( 'class' ) );
		redLink.parentNode.replaceChild( replacementSpan, redLink );
	}
} );