var bridge = require("./bridge");
var transformer = require("./transformer");

bridge.registerListener( "clearContents", function() {
    clearContents();
});

bridge.registerListener( "displayLeadSection", function( payload ) {
    // This might be a refresh! Clear out all contents!
    clearContents();

    var title = document.createElement( "h1" );
    title.innerHTML = payload.title;
    title.id = "heading_" + payload.section.id;
    title.className =  "section_heading";
    title.setAttribute( "data-id", 0 );
    document.getElementById( "content" ).appendChild( title );

    var issuesContainer = document.createElement( "div" );
    issuesContainer.id = "issues_container";
    issuesContainer.className = "issues_container";
    document.getElementById( "content" ).appendChild( issuesContainer );

    var editButton = document.createElement( "a" );
    editButton.setAttribute( 'data-id', payload.section.id );
    editButton.setAttribute( 'data-action', "edit_section" );
    editButton.className = "edit_section_button";
    title.appendChild( editButton );

    var content = document.createElement( "div" );
    content.innerHTML = payload.section.text;
    content.id = "#content_block_0";
    content = transformer.transform( "leadSection", content );
    content = transformer.transform( "section", content );

    //content = transformer.transform( "displayDisambigLink", content );
    //content = transformer.transform( "displayIssuesLink", content );

    //if there were no page issues, then hide the container
    if (!issuesContainer.hasChildNodes()) {
        document.getElementById( "content" ).removeChild(issuesContainer);
    }
    //update the text of the disambiguation link, if there is one
    var disambig = document.getElementById( "disambig_button" );
    if (disambig !== null) {
        disambig.innerText = payload.string_page_similar_titles;
    }

    document.getElementById( "content" ).appendChild( content );

    document.getElementById( "loading_sections").className = "loading";
});

function clearContents() {
    document.getElementById( "content" ).innerHTML = "";
    document.getElementById( "lastupdated" ).innerHTML = "";
    document.getElementById( "licensetext" ).innerHTML = "";
}

function elementsForSection( section ) {
    var heading = document.createElement( "h" + ( section.toclevel + 1 ) );
    heading.innerHTML = typeof section.line !== "undefined" ? section.line : "";
    heading.id = section.anchor;
    heading.className = "section_heading";
    heading.setAttribute( 'data-id', section.id );

    var editButton = document.createElement( "a" );
    editButton.setAttribute( 'data-id', section.id );
    editButton.setAttribute( 'data-action', "edit_section" );
    editButton.className = "edit_section_button";
    heading.appendChild( editButton );

    var content = document.createElement( "div" );
    content.innerHTML = section.text;
    content.id = "content_block_" + section.id;
    content = transformer.transform( "section", content );

    return [ heading, content ];
}

function sectionsAllDone() {
    document.getElementById( "loading_sections").className = "";
}

bridge.registerListener( "noMoreSections", function() {
    sectionsAllDone();
} );

bridge.registerListener( "displaySection", function ( payload ) {
    var contentWrapper = document.getElementById( "content" );

    elementsForSection( payload.section ).forEach( function( element ) {
        contentWrapper.appendChild( element );
    });
    if ( !payload.isLast ) {
        bridge.sendMessage( "requestSection", { index: payload.index + 1 } );
    } else {
        sectionsAllDone();
        if ( typeof payload.fragment === "string" ) {
            scrollToSection( payload.fragment );
        }
    }
});

bridge.registerListener( "startSectionsDisplay", function() {
    bridge.sendMessage( "requestSection", { index: 1 } );
});

bridge.registerListener( "scrollToSection", function ( payload ) {
    scrollToSection( payload.anchor );
});

function scrollToSection( anchor ) {
    var el = document.getElementById( anchor );
    // Make sure there's exactly as much space on the left as on the top.
    // The 48 accounts for the search bar
    var scrollY = el.offsetTop - 48 - el.offsetLeft;
    window.scrollTo( 0, scrollY );
}

/**
 * Returns the section id of the section that has the header closest to but above midpoint of screen
 */
function getCurrentSection() {
    var sectionHeaders = document.getElementsByClassName( "section_heading" );
    var topCutoff = window.scrollY + ( document.documentElement.clientHeight / 2 );
    var curClosest = null;
    for ( var i = 0; i < sectionHeaders.length; i++ ) {
        var el = sectionHeaders[i];
        if ( curClosest === null ) {
            curClosest = el;
            continue;
        }
        if ( el.offsetTop >= topCutoff ) {
            break;
        }
        if ( Math.abs(el.offsetTop - topCutoff) < Math.abs(curClosest.offsetTop - topCutoff) ) {
            curClosest = el;
        }
    }

    return curClosest.getAttribute( "data-id" );
}

bridge.registerListener( "requestCurrentSection", function() {
    bridge.sendMessage( "currentSectionResponse", { sectionID: getCurrentSection() } );
} );
