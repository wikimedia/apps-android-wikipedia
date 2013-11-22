( function() {
    function forEach( list, fun ) {
        // Hack from https://developer.mozilla.org/en-US/docs/Web/API/NodeList#Workarounds
        // To let me use forEach on things like NodeList objects
        Array.prototype.forEach.call( list, fun );
    }

    bridge.registerListener( "displayLeadSection", function( payload ) {
        document.getElementById( "content" ).innerHTML += payload.leadSectionHTML;
    });

    bridge.registerListener( "displaySectionsList", function( payload ) {
        payload.sectionHeadings.forEach( function( section ) {
            var heading = document.createElement( "h2" );
            heading.textContent = section.heading;
            heading.id = "#heading_" + section.index;
            heading.attributes['data-index'] = section.index;
            document.getElementById( "content" ).appendChild( heading );

            var content = document.createElement( "div" );
            content.innerHTML = section.content;
            content.id = "#content_block_" + section.index;
            document.getElementById( "content" ).appendChild( content );
        } );
    });

    document.onclick = function() {
        if ( event.target.tagName === "A" ) {
            bridge.sendMessage( 'linkClicked', { href: event.target.getAttribute( "href" ) });
            event.preventDefault();
        }
    };

} )();