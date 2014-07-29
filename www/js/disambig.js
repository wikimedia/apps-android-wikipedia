var transformer = require('./transformer');

transformer.register( 'displayDisambigLink', function( content ) {
    var hatnotes = content.querySelectorAll( "div.hatnote" );
    var i = 0;
    for (; i<hatnotes.length; i++) {
        var el = hatnotes[i];
        //only care about the first hatnote, and remove all others...
        if (i === 0) {
            var links = el.querySelectorAll("a");
            // use the last link in the hatnote!
            if (links.length > 0) {
                var container = document.getElementById("issues_container");
                var newlink = document.createElement('a');
                newlink.setAttribute('href', '#disambig');
                newlink.id = "disambig_button";
                newlink.className = 'disambig_button';
                newlink.setAttribute("title", links[links.length - 1].getAttribute("href"));
                container.appendChild(newlink);
                el.parentNode.removeChild(el);
            }
        } else {
            el.parentNode.removeChild(el);
        }
    }
    return content;
} );
