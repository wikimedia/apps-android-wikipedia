( function() {
    function forEach( list, fun ) {
        // Hack from https://developer.mozilla.org/en-US/docs/Web/API/NodeList#Workarounds
        // To let me use forEach on things like NodeList objects
        Array.prototype.forEach.call( list, fun );
    }

    bridge.registerListener( "displayLeadSection", function( payload ) {
        var title = document.createElement( "h1" );
        title.textContent = payload.title;
        document.getElementById( "content" ).appendChild( title );

        var content = document.createElement( "div" );
        content.innerHTML = payload.leadSectionHTML;
        content.id = "#content_block_0";
        content = transforms.transform( "lead", content );
        document.getElementById( "content" ).appendChild( content );
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
            content = transforms.transform( "body", content );
            document.getElementById( "content" ).appendChild( content );
        } );
    });

    bridge.registerListener( "displayAttribution", function( payload ) {
        var lastUpdatedA = document.getElementById( "lastupdated" );
        lastUpdatedA.innerText = payload.historyText;
        lastUpdatedA.href = payload.historyTarget;
        var licenseText = document.getElementById( "licensetext" );
        licenseText.innerHTML = payload.licenseHTML;
    });

    bridge.registerListener( "requestImagesList", function ( payload ) {
        var imageURLs = [];
        var images = document.querySelectorAll( "img" );
        for ( var i = 0; i < images.length; i++ ) {
            imageURLs.push( images[i].src );
        }
        bridge.sendMessage( "imagesListResponse", { "images": imageURLs });
    } );

    document.onclick = function() {
        if ( event.target.tagName === "A" ) {
            bridge.sendMessage( 'linkClicked', { href: event.target.getAttribute( "href" ) });
            event.preventDefault();
        }
    };

} )();