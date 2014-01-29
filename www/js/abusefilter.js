var bridge = require('./bridge');

bridge.registerListener( 'displayWarning', function( payload ) {
    var content = document.getElementById( 'content' );
    content.innerHTML = payload.html;
} );