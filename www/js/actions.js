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

bridge.registerListener( "handleReference", function( payload ) {
    handleReference( payload.anchor );
});

function handleReference( targetId ) {
    var targetElem = document.getElementById( targetId );
    if ( targetElem === null ) {
        console.log( "reference target not found: " + targetId );
    } else if ( targetId.slice(0, 4).toLowerCase() === "cite" ) { // treat "CITEREF"s the same as "cite_note"s
        try {
            var refTexts = targetElem.getElementsByClassName( "reference-text" );
            if ( refTexts.length > 0 ) {
                targetElem = refTexts[0];
            }
            bridge.sendMessage( 'referenceClicked', { "ref": targetElem.innerHTML } );
        } catch (e) {
            targetElem.scrollIntoView();
        }
    } else {
        // If it is a link to another anchor in the current page, just scroll to it
        targetElem.scrollIntoView();
    }
}

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
/*
    function collectIssues( sourceNode ) {
        var res = [];
        var issues = sourceNode.parentNode.querySelectorAll( 'table.ambox' );
        var i = 0,
            len = issues.length;
        for (; i < len; i++) {
            // .ambox- is used e.g. on eswiki
            res.push( issues[i].querySelector( '.mbox-text, .ambox-text' ).innerHTML );
        }

        bridge.sendMessage( 'issuesClicked', { "issues": res } );
    }

    function handleDisambig( sourceNode ) {
        var title = sourceNode.getAttribute("title");
        bridge.sendMessage( 'disambigClicked', { "title": title } );
    }
*/
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
                if ("issues" === targetId) {
                    //collectIssues(sourceNode);
                } else if ("disambig" === targetId) {
                    //handleDisambig(sourceNode);
                } else {
                    handleReference( targetId );
                }
            } else {
                bridge.sendMessage( 'linkClicked', { "href": href } );
            }
            event.preventDefault();
        }
    }
};

module.exports = new ActionsHandler();
