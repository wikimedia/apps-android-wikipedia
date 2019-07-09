var actions = require('./actions');
var bridge = require('./bridge');

actions.register( "edit_section", function( el, event ) {
    var sectionID = el.getAttribute( 'data-id' );
    if (sectionID === "0") {
        bridge.sendMessage( 'editSectionClicked', { mainPencilClicked: true, 'x': el.parentNode.offsetLeft, 'y': el.parentNode.offsetTop } );
    } else {
        bridge.sendMessage( 'editSectionClicked', { sectionID: sectionID } );
    }
    event.preventDefault();
} );

actions.register( "add_title_description", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { editDescriptionClicked: true } );
    event.preventDefault();
} );

actions.register( "title_pronunciation", function( el, event ) {
    bridge.sendMessage( 'pronunciationClicked', { } );
    event.preventDefault();
} );
