var bridge = require('./bridge');
var transformer = require('./transformer');

transformer.register( 'clearInlineStyling', function( content ) {
    var styledElements = content.querySelectorAll( "*[style]" );
    for ( var i = 0; i < styledElements.length; i++ ) {
        styledElements[i].removeAttribute( "style" );
    }
    return content;
} );

bridge.registerListener( 'displayWarning', function( payload ) {
    var content = document.getElementById( 'content' );

    var warning = document.createElement( 'div' );
    warning.innerHTML = payload.html;

    warning = transformer.transform( 'clearInlineStyling', warning );

    content.appendChild( warning );
} );
