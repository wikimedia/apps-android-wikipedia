var bridge = require('./bridge');

var actionHandlers = {
    "edit_section": function( el, event ) {
        bridge.sendMessage( 'editSectionClicked', { sectionID: el.getAttribute( 'data-id' ) } );
        event.preventDefault();
    }
};

document.onclick = function() {
    if ( event.target.tagName === "A" ) {
        if ( event.target.hasAttribute( "data-action" ) ) {
            var action = event.target.getAttribute( "data-action" );
            actionHandlers[ action ]( event.target, event );
        } else {
            bridge.sendMessage( 'linkClicked', { href: event.target.getAttribute( "href" ) });
            event.preventDefault();
        }
    }
};