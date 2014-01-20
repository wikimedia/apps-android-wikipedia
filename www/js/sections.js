var bridge = require("./bridge");
var transforms = require("./transforms");

bridge.registerListener( "displayLeadSection", function( payload ) {
    // This might be a refresh! Clear out all contents!
    document.getElementById( "content" ).innerHTML = "";

    var title = document.createElement( "h1" );
    title.textContent = payload.title;
    title.id = "heading_" + payload.section.id;
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
    var contentWrapper = document.getElementById( "content" );

    elementsForSection( payload.section ).forEach( function( element ) {
        contentWrapper.appendChild( element );
    });
    if ( !payload.isLast ) {
        bridge.sendMessage( "requestSection", { index: payload.index + 1 } );
    } else {
        document.getElementById( "loading_sections").className = "";
    }
});

bridge.registerListener( "startSectionsDisplay", function() {
    bridge.sendMessage( "requestSection", { index: 1 } );
});

bridge.registerListener( "scrollToSection", function ( payload ) {
    var el = document.getElementById( "heading_" + payload.sectionID);
    // Make sure there's exactly as much space on the left as on the top.
    // The 48 accounts for the search bar
    var scrollY = el.offsetTop - 48 - el.offsetLeft;
    window.scrollTo(0, scrollY);
});
