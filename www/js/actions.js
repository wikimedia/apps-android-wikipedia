var bridge = require('./bridge');
var pagelib = require("wikimedia-page-library");

function ActionsHandler() {
}

var actionHandlers = {};

ActionsHandler.prototype.register = function( action, fun ) {
    if ( action in actionHandlers ) {
        actionHandlers[action].push( fun );
    } else {
        actionHandlers[action] = [ fun ];
    }
};

bridge.registerListener( 'handleReference', function( payload ) {
    handleReference( "#" + payload.anchor, null, payload.text );
} );

function handleReference( href, linkNode, linkText ) {
    var targetElem = document.getElementById(href.slice(1));
    if (linkNode && pagelib.ReferenceCollection.isCitation(href)){
        var adjacentReferences = pagelib.ReferenceCollection.collectNearbyReferences(document, linkNode);
        bridge.sendMessage( 'referenceClicked', adjacentReferences );
    } else if ( href.slice(1, 5).toLowerCase() === "cite" ) {
        try {
            var refTexts = targetElem.getElementsByClassName( "reference-text" );
            if ( refTexts.length > 0 ) {
                targetElem = refTexts[0];
            }
            bridge.sendMessage( 'referenceClicked', { "selectedIndex": 0, "referencesGroup": [ { "html": targetElem.innerHTML, "text": linkText } ] });
        } catch (e) {
            targetElem.scrollIntoView();
        }
    } else {
        if ( targetElem === null ) {
            console.log( "reference target not found: " + href );
        } else {
            // If it is a link to another anchor in the current page, just scroll to it
            targetElem.scrollIntoView();
        }
    }
}

document.onclick = function() {
    var sourceNode = null;
    var curNode = event.target;
    // If an element was clicked, check if it or any of its parents are <a>
    // This handles cases like <a>foo</a>, <a><strong>foo</strong></a>, etc.
    while (curNode) {
        if (curNode.tagName === "A" || curNode.tagName === "AREA") {
            sourceNode = curNode;
            break;
        }
        curNode = curNode.parentNode;
    }

    if (sourceNode) {
        if ( sourceNode.hasAttribute( "data-action" ) ) {
            var action = sourceNode.getAttribute( "data-action" );
            var handlers = actionHandlers[ action ];
            for ( var i = 0; i < handlers.length; i++ ) {
                handlers[i]( sourceNode, event );
            }
        } else {
            var href = sourceNode.getAttribute( "href" );
            if ( href[0] === "#" ) {
                handleReference(href, event.target, null);
            } else if (sourceNode.classList.contains( 'app_media' )) {
                bridge.sendMessage( 'mediaClicked', { "href": href } );
            } else if (sourceNode.classList.contains( 'image' )) {
                bridge.sendMessage( 'imageClicked', { "href": href } );
            } else {
                var response = { "href": href, "text": sourceNode.textContent };
                if (sourceNode.hasAttribute( "title" )) {
                    response.title = sourceNode.getAttribute( "title" );
                }
                bridge.sendMessage( 'linkClicked', response );
            }
            event.preventDefault();
        }
    }
};

module.exports = new ActionsHandler();
