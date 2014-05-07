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
    var linkTarget = null;
    var curNode = event.target;
    // If an element was clicked, check if it or any of its parents are <a>
    // This handles cases like <a>foo</a>, <a><strong>foo</strong></a>, etc.
    while (curNode) {
        if (curNode.tagName === "A") {
            linkTarget = curNode;
            break;
        }
        curNode = curNode.parentNode;
    }
    if (linkTarget) {
        if ( linkTarget.hasAttribute( "data-action" ) ) {
            var action = linkTarget.getAttribute( "data-action" );
            var handlers = actionHandlers[ action ];
            for ( var i = 0; i < handlers.length; i++ ) {
                handlers[i]( linkTarget, event );
            }
        } else {
            var href = linkTarget.getAttribute( "href" );
            if ( href[0] === "#" ) {
                // If it is a link to an anchor in the current page, just scroll to it
                document.getElementById( href.substring( 1 ) ).scrollIntoView();
            } else {
                bridge.sendMessage( 'linkClicked', { "href": href } );
            }
            event.preventDefault();
        }
    }
};

module.exports = new ActionsHandler();
