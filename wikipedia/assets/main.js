( function() {
    function forEach( list, fun ) {
        // Hack from https://developer.mozilla.org/en-US/docs/Web/API/NodeList#Workarounds
        // To let me use forEach on things like NodeList objects
        Array.prototype.forEach.call( list, fun );
    }

    bridge.registerListener( "displayLeadSection", function( payload ) {
        // This might be a refresh! Clear out all contents!
        document.getElementById( "content" ).innerHTML = "";

        var title = document.createElement( "h1" );
        title.textContent = payload.title;
        document.getElementById( "content" ).appendChild( title );

        var content = document.createElement( "div" );
        content.innerHTML = payload.section.text;
        content.id = "#content_block_0";
        content = transforms.transform( "lead", content );
        document.getElementById( "content" ).appendChild( content );

        document.getElementById( "loading_sections").className = "loading";
    });

    function elementsForSection( section ) {
        var heading = document.createElement( "h" + ( section.toclevel + 1 ) );
        heading.textContent = section.line;
        heading.id = "heading_" + section.id;
        heading.setAttribute( 'data-id', section.id );

        var editButton = document.createElement( "a" );
        editButton.setAttribute( 'data-id', section.id );
        editButton.setAttribute( 'data-action', "edit_section" );
        editButton.className = "edit_section_button";
        heading.appendChild( editButton );

        var content = document.createElement( "div" );
        content.innerHTML = section.text;
        content.id = "content_block_" + section.id;
        content = transforms.transform( "body", content );

        return [ heading, content ];
    }

    bridge.registerListener( "displaySection", function ( payload ) {
        var content_wrapper = document.getElementById( "content" );

        elementsForSection( payload.section ).forEach( function( element ) {
            content_wrapper.appendChild( element );
        });
        if ( !payload.isLast ) {
            bridge.sendMessage( "requestSection", { index: payload.index + 1 } );
        } else {
            document.getElementById( "loading_sections").className = "";
        }
    });

    bridge.registerListener( "startSectionsDisplay", function( payload ) {
        bridge.sendMessage( "requestSection", { index: 1 } );
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

} )();