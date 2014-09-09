var transformer = require("./transformer");
var night = require("./night");

// Move infobox to the bottom of the lead section
transformer.register( "leadSection", function( leadContent ) {
    var infobox = leadContent.querySelector( "table.infobox" );
    var pTags;
    if ( infobox ) {

        /*
        If the infobox table itself sits within a table or series of tables,
        move the most distant ancestor table instead of just moving the
        infobox. Otherwise you end up with table(s) with a hole where the
        infobox had been. World War II article on enWiki has this issue.
        Note that we need to stop checking ancestor tables when we hit
        content_block_0.
        */
        var infoboxParentTable = null;
        var el = infobox;
        while (el.parentNode) {
            el = el.parentNode;
            if (el.id === 'content_block_0') {
                break;
            }
            if (el.tagName === 'TABLE') {
                infoboxParentTable = el;
            }
        }
        if (infoboxParentTable) {
            infobox = infoboxParentTable;
        }

        infobox.parentNode.removeChild( infobox );
        pTags = leadContent.getElementsByTagName( "p" );
        if ( pTags.length ) {
            pTags[0].appendChild( infobox );
        } else {
            leadContent.appendChild( infobox );
        }
    }
    //also move any thumbnail images to the bottom of the section,
    //since we have a lead image, and we want the content to appear at the very beginning.
    var thumbs = leadContent.querySelectorAll( "div.thumb" );
    for ( var i = 0; i < thumbs.length; i++ ) {
        thumbs[i].parentNode.removeChild( thumbs[i] );
        pTags = leadContent.getElementsByTagName( "p" );
        if ( pTags.length ) {
            pTags[pTags.length - 1].appendChild( thumbs[i] );
        } else {
            leadContent.appendChild( thumbs[i] );
        }
    }
    return leadContent;
} );

transformer.register( "section", function( content ) {
	if ( window.isNightMode ) {
		night.invertElement ( content );
	}
	return content;
} );

transformer.register( "section", function( content ) {
	var redLinks = content.querySelectorAll( 'a.new' );
	for ( var i = 0; i < redLinks.length; i++ ) {
		var redLink = redLinks[i];
		var replacementSpan = document.createElement( 'span' );
		replacementSpan.innerHTML = redLink.innerHTML;
		replacementSpan.setAttribute( 'class', redLink.getAttribute( 'class' ) );
		redLink.parentNode.replaceChild( replacementSpan, redLink );
	}
	return content;
} );
