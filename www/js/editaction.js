var actions = require('./actions');
var bridge = require('./bridge');

actions.register( "edit_section", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { sectionID: el.getAttribute( 'data-id' ) } );
    event.preventDefault();
} );

actions.register( "edit_main", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { mainPencilClicked: true, 'x': el.offsetLeft, 'y': el.offsetTop } );
    event.preventDefault();
} );

actions.register( "edit_description", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { editDescriptionClicked: true } );
    event.preventDefault();
} );

actions.register( "pronunciation_click", function( el, event ) {
    bridge.sendMessage( 'pronunciationClicked', { } );
    event.preventDefault();
} );
