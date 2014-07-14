var bridge = require('./bridge');

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

document.onclick = function() {
    var sourceNode = null;
    var curNode = event.target;
    // If an element was clicked, check if it or any of its parents are <a>
    // This handles cases like <a>foo</a>, <a><strong>foo</strong></a>, etc.
    while (curNode) {
        if (curNode.tagName === "A") {
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
                var targetId = href.slice(1);
                var target = document.getElementById( targetId );
                if ( target === null ) {
                    console.log( "reference target not found: " + targetId );
                } else if ( href.slice(0, 10) === "#cite_note" ) {
                    try {
                        var refTexts = target.getElementsByClassName( "reference-text" );
                        if ( refTexts.length > 0 ) {
                            target = refTexts[0];
                        }
                        bridge.sendMessage( 'referenceClicked', { "ref": target.innerHTML } );
                    } catch (e) {
                        target.scrollIntoView();
                    }
                } else {
                    // If it is a link to an anchor in the current page, just scroll to it
                    target.scrollIntoView();
                }
            } else {
                bridge.sendMessage( 'linkClicked', { "href": href } );
            }
            event.preventDefault();
        }
    }
};

module.exports = new ActionsHandler();
