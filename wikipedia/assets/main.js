( function() {
    function forEach( list, fun ) {
        // Hack from https://developer.mozilla.org/en-US/docs/Web/API/NodeList#Workarounds
        // To let me use forEach on things like NodeList objects
        Array.prototype.forEach.call( list, fun );
    }

    bridge.registerListener( "displayLeadSection", function( payload ) {
        document.getElementById( "content" ).innerHTML += payload.leadSectionHTML;
    });

    document.onclick = function() {
        if ( event.target.tagName === "A" ) {
            bridge.sendMessage( 'linkClicked', { href: event.target.getAttribute( "href" ) });
            event.preventDefault();
        }
    }

} )();