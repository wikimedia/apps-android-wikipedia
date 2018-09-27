var transformer = require('./transformer');

transformer.register( 'showIssues', function( content ) {
    var issues = content.querySelectorAll( ".ambox" );
    var style;
    for (var i = 0; i < issues.length; i++ ) {
        style = issues[i].getAttribute('style');
        if (!style) {
            style = "";
        }
        issues[i].setAttribute('style', style + "display:block !important;");
    }
    return content;
} );
