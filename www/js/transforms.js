var transformer = require("./transformer");
var night = require("./night");
var bridge = require("./bridge");
var widenImages = require("./widenImages");
var util = require("./util");

// Takes a block of text, and removes any text within parentheses, but only
// until the end of the first sentence.
// Based on Extensions:Popups - ext.popups.renderer.article.js
function removeParensFromText( string ) {
    var ch;
    var newString = '';
    var level = 0;
    var i = 0;
    for( ; i < string.length; i++ ) {
        ch = string.charAt( i );
        if ( ch === ')' && level === 0  ) {
            // abort if we have an imbalance of parentheses
            return string;
        }
        if ( ch === '(' ) {
            level++;
            continue;
        } else if ( ch === ')' ) {
            level--;
            continue;
        }
        if ( level === 0 ) {
            // Remove leading spaces before parentheses
            if ( ch === ' ' && (i < string.length - 1) && string.charAt( i + 1 ) === '(' ) {
                continue;
            }
            newString += ch;
            if ( ch === '.' ) {
                // stop at the end of the first sentence
                break;
            }
        }
    }
    // fill in the rest of the string
    if ( i + 1 < string.length ) {
        newString += string.substring( i + 1, string.length );
    }
    // if we had an imbalance of parentheses, then return the original string,
    // instead of the transformed one.
    return ( level === 0 ) ? newString : string;
}

// Move the first non-empty paragraph of text to the top of the section.
// This will have the effect of shifting the infobox and/or any images at the top of the page
// below the first paragraph, allowing the user to start reading the page right away.
transformer.register( "leadSection", function( leadContent ) {
    if (window.isMainPage) {
        // don't do anything if this is the main page, since many wikis
        // arrange the main page in a series of tables.
        return leadContent;
    }
    var block_0 = document.getElementById( "content_block_0" );
    if (!block_0) {
        return leadContent;
    }

    var allPs = block_0.getElementsByTagName( "p" );
    if (!allPs) {
        return leadContent;
    }

    for ( var i = 0; i < allPs.length; i++ ) {
        var p = allPs[i];
        // Narrow down to first P which is direct child of content_block_0 DIV.
        // (Don't want to yank P from somewhere in the middle of a table!)
        if (p.parentNode !== block_0) {
            continue;
        }
        // Ensure the P being pulled up has at least a couple lines of text.
        // Otherwise silly things like a empty P or P which only contains a
        // BR tag will get pulled up (see articles on "Chemical Reaction" and
        // "Hawaii").
        // Trick for quickly determining element height:
        // https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement.offsetHeight
        // http://stackoverflow.com/a/1343350/135557
        var minHeight = 24;
        if (p.offsetHeight < minHeight){
            continue;
        }

        // Move the P!
        block_0.insertBefore(p.parentNode.removeChild(p), block_0.firstChild);

        // Transform the first sentence of the first paragraph.
        // (but only for non-production, and only on enwiki)
        if ( window.isBeta && window.siteLanguage.indexOf( "en" ) > -1 ) {
            p.innerHTML = removeParensFromText(p.innerHTML);
        }

        // But only move one P!
        break;
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
    var caption = divCollapsed.querySelector('.app_table_collapsed_caption');
    if (tableFull.style.display !== 'none') {
        tableFull.style.display = 'none';
        divCollapsed.classList.remove('app_table_collapse_close');
        divCollapsed.classList.remove('app_table_collapse_icon');
        divCollapsed.classList.add('app_table_collapsed_open');
        if (caption !== null) {
            caption.style.visibility = 'visible';
        }
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
        if (caption !== null) {
            caption.style.visibility = 'hidden';
        }
        divBottom.style.display = 'block';
    }
}

transformer.register( "hideTables", function( content ) {
    var tables = content.querySelectorAll( "table" );
    for (var i = 0; i < tables.length; i++) {
        //is the table already hidden? if so, don't worry about it
        if (tables[i].style.display === 'none' || tables[i].classList.contains( 'navbox' ) || tables[i].classList.contains( 'vertical-navbox' ) || tables[i].classList.contains( 'navbox-inner' ) || tables[i].classList.contains( 'metadata' )) {
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
            caption += " ...";
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

transformer.register( "hideRefs", function( content ) {
    var refLists = content.querySelectorAll( "div.reflist" );
    for (var i = 0; i < refLists.length; i++) {
        var caption = "<strong class='app_table_collapsed_caption'>" + window.string_expand_refs + "</strong>";

        //create the container div that will contain both the original table
        //and the collapsed version.
        var containerDiv = document.createElement( 'div' );
        containerDiv.className = 'app_table_container';
        refLists[i].parentNode.insertBefore(containerDiv, refLists[i]);
        refLists[i].parentNode.removeChild(refLists[i]);

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
        containerDiv.appendChild(refLists[i]);
        containerDiv.appendChild(bottomDiv);

        //give it just a little padding
        refLists[i].style.padding = "4px";

        //set initial visibility
        refLists[i].style.display = 'none';
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

transformer.register( "section", function( content ) {
    // look for video thumbnail containers (divs that have class "PopUpMediaTransform"),
    // and enclose them in an anchor that will lead to the correct click handler...
	var mediaDivs = content.querySelectorAll( 'div.PopUpMediaTransform' );
	for ( var i = 0; i < mediaDivs.length; i++ ) {
		var mediaDiv = mediaDivs[i];
		var imgTags = mediaDiv.querySelectorAll( 'img' );
		if (imgTags.length === 0) {
		    continue;
		}
		// the first img element is the video thumbnail, and its 'alt' attribute is
		// the file name of the video!
		if (!imgTags[0].getAttribute( 'alt' )) {
		    continue;
		}
		// also, we should hide the "Play media" link that appears under the thumbnail,
		// since we don't need it.
		var aTags = mediaDiv.querySelectorAll( 'a' );
		if (aTags.length > 0) {
		    aTags[0].parentNode.removeChild(aTags[0]);
		}
		var containerLink = document.createElement( 'a' );
        containerLink.setAttribute( 'href', imgTags[0].getAttribute( 'alt' ) );
        containerLink.classList.add( 'app_media' );
        mediaDiv.parentNode.insertBefore(containerLink, mediaDiv);
        mediaDiv.parentNode.removeChild(mediaDiv);
        containerLink.appendChild(imgTags[0]);
	}
	return content;
} );

transformer.register( "widenImages", function( content ) {
    var images = content.querySelectorAll( 'img' );
    for ( var i = 0; i < images.length; i++ ) {
        // Load event used so images w/o style or inline width/height
        // attributes can still have their size determined reliably.
        images[i].addEventListener('load', widenImages.maybeWidenImage, false);
    }
    return content;
} );

transformer.register( "addImageOverflowXContainers", function( content ) {
    // Wrap wide images in a <div style="overflow-x:auto">...</div> so they can scroll
    // side to side if needed without causing the entire section to scroll side to side.
    var images = content.getElementsByTagName('img');
    for (var i = 0; i < images.length; ++i) {
        // Load event used so images w/o style or inline width/height
        // attributes can still have their size determined reliably.
        images[i].addEventListener('load', util.maybeAddImageOverflowXContainer, false);
    }
    return content;
} );
