var transformer = require("./transformer");
var night = require("./night");
var bridge = require( "./bridge" );

// Move infobox to the bottom of the lead section
transformer.register( "leadSection", function( leadContent ) {
    var infobox = leadContent.querySelector( "table.infobox" );
    var pTags;
    if ( infobox ) {

        /*
        If the infobox table itself sits within a table or series of tables,
        move the most distant ancestor table instead of just moving the
        infobox. Otherwise you end up with table(s) with a hole where the
        infobox had been. World War II article on enWiki has this issue.
        Note that we need to stop checking ancestor tables when we hit
        content_block_0.
        */
        var infoboxParentTable = null;
        var el = infobox;
        while (el.parentNode) {
            el = el.parentNode;
            if (el.id === 'content_block_0') {
                break;
            }
            if (el.tagName === 'TABLE') {
                infoboxParentTable = el;
            }
        }
        if (infoboxParentTable) {
            infobox = infoboxParentTable;
        }

        infobox.parentNode.removeChild( infobox );
        pTags = leadContent.getElementsByTagName( "p" );
        if ( pTags.length ) {
            pTags[0].appendChild( infobox );
        } else {
            leadContent.appendChild( infobox );
        }
    }
    //also move any thumbnail images to the bottom of the section,
    //since we have a lead image, and we want the content to appear at the very beginning.
    var thumbs = leadContent.querySelectorAll( "div.thumb" );
    for ( var i = 0; i < thumbs.length; i++ ) {
        thumbs[i].parentNode.removeChild( thumbs[i] );
        pTags = leadContent.getElementsByTagName( "p" );
        if ( pTags.length ) {
            pTags[pTags.length - 1].appendChild( thumbs[i] );
        } else {
            leadContent.appendChild( thumbs[i] );
        }
    }
    return leadContent;
} );

/*
Tries to get an array of table header (TH) contents from a given table.
If there are no TH elements in the table, an empty array is returned.
*/
function getTableHeader( element ) {
    var thArray = [];
    if (element.children === undefined || element.children === null) {
        return thArray;
    }
    for (var i = 0; i < element.children.length; i++) {
        var el = element.children[i];
        if (el.tagName === "TH") {
            // ok, we have a TH element!
            // However, if it contains more than two links, then ignore it, because
            // it will probably appear weird when rendered as plain text.
            var aNodes = el.querySelectorAll( "a" );
            if (aNodes.length < 3) {
                // Also ignore it if it's identical to the page title.
                if (el.innerText.length > 0 && el.innerText !== window.pageTitle && el.innerHTML !== window.pageTitle) {
                    thArray.push(el.innerText);
                }
            }
        }
        //if it's a table within a table, don't worry about it
        if (el.tagName === "TABLE") {
            continue;
        }
        //recurse into children of this element
        var ret = getTableHeader(el);
        //did we get a list of TH from this child?
        if (ret.length > 0) {
            thArray = thArray.concat(ret);
        }
    }
    return thArray;
}

/*
OnClick handler function for expanding/collapsing tables and infoboxes.
*/
function tableCollapseClickHandler() {
    var container = this.parentNode;
    var divCollapsed = container.children[0];
    var tableFull = container.children[1];
    var divBottom = container.children[2];
    if (tableFull.style.display !== 'none') {
        tableFull.style.display = 'none';
        divCollapsed.classList.remove('app_table_collapse_close');
        divCollapsed.classList.remove('app_table_collapse_icon');
        divCollapsed.classList.add('app_table_collapsed_open');
        divBottom.style.display = 'none';
        //if they clicked the bottom div, then scroll back up to the top of the table.
        if (this === divBottom) {
            window.scrollTo( 0, container.offsetTop - 48 );
        }
    } else {
        tableFull.style.display = 'block';
        divCollapsed.classList.remove('app_table_collapsed_open');
        divCollapsed.classList.add('app_table_collapse_close');
        divCollapsed.classList.add('app_table_collapse_icon');
        divBottom.style.display = 'block';
    }
}

transformer.register( "hideTables", function( content ) {
    var tables = content.querySelectorAll( "table" );
    for (var i = 0; i < tables.length; i++) {
        //is the table already hidden? if so, don't worry about it
        if (tables[i].style.display === 'none' || tables[i].classList.contains( 'navbox' ) || tables[i].classList.contains( 'vertical-navbox' ) || tables[i].classList.contains( 'navbox-inner' )) {
            continue;
        }

        var isInfobox = tables[i].classList.contains( 'infobox' );
        var headerText = getTableHeader(tables[i]);
        if (headerText.length === 0 && !isInfobox) {
            continue;
        }
        var caption = "<strong>" + (isInfobox ? window.string_table_infobox : window.string_table_other) + "</strong>";
        caption += "<span class='app_span_collapse_text'>";
        if (headerText.length > 0) {
            caption += ": " + headerText[0];
        }
        if (headerText.length > 1) {
            caption += ", " + headerText[1];
        }
        if (headerText.length > 0) {
            caption += ", ...";
        }
        caption += "</span>";

        //create the container div that will contain both the original table
        //and the collapsed version.
        var containerDiv = document.createElement( 'div' );
        containerDiv.className = 'app_table_container';
        tables[i].parentNode.insertBefore(containerDiv, tables[i]);
        tables[i].parentNode.removeChild(tables[i]);

        //remove top and bottom margin from the table, so that it's flush with
        //our expand/collapse buttons
        tables[i].style.marginTop = "0px";
        tables[i].style.marginBottom = "0px";

        //create the collapsed div
        var collapsedDiv = document.createElement( 'div' );
        collapsedDiv.classList.add('app_table_collapsed_container');
        collapsedDiv.classList.add('app_table_collapsed_open');
        collapsedDiv.innerHTML = caption;

        //create the bottom collapsed div
        var bottomDiv = document.createElement( 'div' );
        bottomDiv.classList.add('app_table_collapsed_bottom');
        bottomDiv.classList.add('app_table_collapse_icon');
        bottomDiv.innerHTML = window.string_table_close;

        //add our stuff to the container
        containerDiv.appendChild(collapsedDiv);
        containerDiv.appendChild(tables[i]);
        containerDiv.appendChild(bottomDiv);

        //set initial visibility
        tables[i].style.display = 'none';
        collapsedDiv.style.display = 'block';
        bottomDiv.style.display = 'none';

        //assign click handler to the collapsed divs
        collapsedDiv.onclick = tableCollapseClickHandler;
        bottomDiv.onclick = tableCollapseClickHandler;
    }
    return content;
} );

/*
OnClick handler function for IPA spans.
*/
function ipaClickHandler() {
    var container = this;
    bridge.sendMessage( "ipaSpan", { "contents": container.innerHTML });
}

transformer.register( "hideIPA", function( content ) {
    var spans = content.querySelectorAll( "span.IPA" );
    for (var i = 0; i < spans.length; i++) {
        var parentSpan = spans[i].parentNode;
        if (parentSpan === null) {
            continue;
        }
        var doTransform = false;
        // case 1: we have a sequence of IPA spans contained in a parent "nowrap" span
        if (parentSpan.tagName === "SPAN" && spans[i].classList.contains('nopopups')) {
            doTransform = true;
        }
        if (parentSpan.style.display === 'none') {
            doTransform = false;
        }
        if (!doTransform) {
            continue;
        }

        //we have a new IPA span!

        var containerSpan = document.createElement( 'span' );
        parentSpan.parentNode.insertBefore(containerSpan, parentSpan);
        parentSpan.parentNode.removeChild(parentSpan);

        //create and add the button
        var buttonDiv = document.createElement( 'div' );
        buttonDiv.classList.add('ipa_button');
        containerSpan.appendChild(buttonDiv);
        containerSpan.appendChild(parentSpan);

        //set initial visibility
        parentSpan.style.display = 'none';
        //and assign the click handler to it
        containerSpan.onclick = ipaClickHandler;
    }
    return content;
} );

transformer.register( "section", function( content ) {
	if ( window.isNightMode ) {
		night.invertElement ( content );
	}
	return content;
} );

transformer.register( "section", function( content ) {
	var redLinks = content.querySelectorAll( 'a.new' );
	for ( var i = 0; i < redLinks.length; i++ ) {
		var redLink = redLinks[i];
		var replacementSpan = document.createElement( 'span' );
		replacementSpan.innerHTML = redLink.innerHTML;
		replacementSpan.setAttribute( 'class', redLink.getAttribute( 'class' ) );
		redLink.parentNode.replaceChild( replacementSpan, redLink );
	}
	return content;
} );

transformer.register( "section", function( content ) {
    if (window.apiLevel < 11) {
        //don't do anything for GB
        return content;
    }
    var allDivs = content.querySelectorAll( 'div' );
    var contentWrapper = document.getElementById( "content" );
    var clientWidth = contentWrapper.offsetWidth;
    for ( var i = 0; i < allDivs.length; i++ ) {
        if (allDivs[i].style && allDivs[i].style.width) {
            // if this div has an explicit width, and it's greater than our client width,
            // then make it overflow (with scrolling), and reset its width to 100%
            if (parseInt(allDivs[i].style.width) > clientWidth) {
                allDivs[i].style.overflowX = "auto";
                allDivs[i].style.width = "100%";
            }
        }
    }
    return content;
} );

transformer.register( "section", function( content ) {
    // Prevent horizontally scrollable pages by checking for math formula images (which are
    // often quite wide), and explicitly setting their maximum width to fit the viewport.
    var allImgs = content.querySelectorAll( 'img' );
    for ( var i = 0; i < allImgs.length; i++ ) {
        var imgItem = allImgs[i];
        // is the image a math formula?
        for ( var c = 0; c < imgItem.classList.length; c++ ) {
            if (imgItem.classList[c].indexOf("math") > -1) {
                imgItem.style.maxWidth = "100%";
            }
        }
    }
    return content;
} );
