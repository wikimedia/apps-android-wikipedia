(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var bridge = require('./bridge');
var util = require('./utilities');

function ActionsHandler() {
}

var actionHandlers = {};

ActionsHandler.prototype.register = function( action, fun ) {
    if ( action in actionHandlers ) {
        actionHandlers[action].push( fun );
    } else {
        actionHandlers[action] = [ fun ];
    }
};

bridge.registerListener( "handleReference", function( payload ) {
    handleReference( payload.anchor, false );
});

function handleReference( targetId, backlink ) {
    var targetElem = document.getElementById( targetId );
    if ( targetElem === null ) {
        console.log( "reference target not found: " + targetId );
    } else if ( !backlink && targetId.slice(0, 4).toLowerCase() === "cite" ) { // treat "CITEREF"s the same as "cite_note"s
        try {
            var refTexts = targetElem.getElementsByClassName( "reference-text" );
            if ( refTexts.length > 0 ) {
                targetElem = refTexts[0];
            }
            bridge.sendMessage( 'referenceClicked', { "ref": targetElem.innerHTML } );
        } catch (e) {
            targetElem.scrollIntoView();
        }
    } else {
        // If it is a link to another anchor in the current page, just scroll to it
        targetElem.scrollIntoView();
    }
}

document.onclick = function() {
    var sourceNode = null;
    var curNode = event.target;
    // If an element was clicked, check if it or any of its parents are <a>
    // This handles cases like <a>foo</a>, <a><strong>foo</strong></a>, etc.
    while (curNode) {
        if (curNode.tagName === "A" || curNode.tagName === "AREA") {
            sourceNode = curNode;
            break;
        }
        curNode = curNode.parentNode;
    }

    if (sourceNode) {
        if ( sourceNode.hasAttribute( "data-action" ) ) {
            var action = sourceNode.getAttribute( "data-action" );
            var handlers = actionHandlers[ action ];
            for ( var i = 0; i < handlers.length; i++ ) {
                handlers[i]( sourceNode, event );
            }
        } else {
            var href = sourceNode.getAttribute( "href" );
            if ( href[0] === "#" ) {
                var targetId = href.slice(1);
                if ( "issues" === targetId ) {
                    issuesClicked( sourceNode );
                } else if ( "disambig" === targetId ) {
                    disambigClicked( sourceNode );
                } else {
                    handleReference( targetId, util.ancestorContainsClass( sourceNode, "mw-cite-backlink" ) );
                }
            } else if (sourceNode.classList.contains( 'app_media' )) {
                bridge.sendMessage( 'mediaClicked', { "href": href } );
            } else if (sourceNode.classList.contains( 'image' )) {
                bridge.sendMessage( 'imageClicked', { "href": href } );
            } else {
                bridge.sendMessage( 'linkClicked', { "href": href } );
            }
            event.preventDefault();
        }
    }
};

function issuesClicked( sourceNode ) {
    var issues = collectIssues( sourceNode.parentNode );
    var disambig = collectDisambig( sourceNode.parentNode.parentNode ); // not clicked node
    bridge.sendMessage( 'issuesClicked', { "hatnotes": disambig, "issues": issues } );
}

function disambigClicked( sourceNode ) {
    var disambig = collectDisambig( sourceNode.parentNode );
    var issues = collectIssues( sourceNode.parentNode.parentNode ); // not clicked node
    bridge.sendMessage( 'disambigClicked', { "hatnotes": disambig, "issues": issues } );
}

function collectDisambig( sourceNode ) {
    var res = [];
    var links = sourceNode.querySelectorAll( 'div.hatnote a' );
    var i = 0,
        len = links.length;
    for (; i < len; i++) {
        // Pass the href; we'll decode it into a proper page title in Java
        res.push( links[i].getAttribute( 'href' ) );
    }
    return res;
}

function collectIssues( sourceNode ) {
    var res = [];
    var issues = sourceNode.querySelectorAll( 'table.ambox' );
    var i = 0,
        len = issues.length;
    for (; i < len; i++) {
        // .ambox- is used e.g. on eswiki
        res.push( issues[i].querySelector( '.mbox-text, .ambox-text' ).innerHTML );
    }
    return res;
}

module.exports = new ActionsHandler();

},{"./bridge":2,"./utilities":24}],2:[function(require,module,exports){
function Bridge() {
}

var eventHandlers = {};

// This is called directly from Java
window.handleMessage = function( type, msgPointer ) {
    var that = this;
    var payload = JSON.parse( marshaller.getPayload( msgPointer ) );
    if ( eventHandlers.hasOwnProperty( type ) ) {
        eventHandlers[type].forEach( function( callback ) {
            callback.call( that, payload );
        } );
    }
};

Bridge.prototype.registerListener = function( messageType, callback ) {
    if ( eventHandlers.hasOwnProperty( messageType ) ) {
        eventHandlers[messageType].push( callback );
    } else {
        eventHandlers[messageType] = [ callback ];
    }
};

Bridge.prototype.sendMessage = function( messageType, payload ) {
    var messagePack = { type: messageType, payload: payload };
    var ret = window.prompt( encodeURIComponent(JSON.stringify( messagePack )) );
    if ( ret ) {
        return JSON.parse( ret );
    }
};

module.exports = new Bridge();
// FIXME: Move this to somewhere else, eh?
window.onload = function() {
    module.exports.sendMessage( "DOMLoaded", {} );
};
},{}],3:[function(require,module,exports){
var transformer = require('./transformer');

transformer.register( 'displayDisambigLink', function( content ) {
    var hatnotes = content.querySelectorAll( "div.hatnote" );
    if ( hatnotes.length > 0 ) {
        var container = document.getElementById( "issues_container" );
        var wrapper = document.createElement( 'div' );
        var link = document.createElement( 'a' );
        link.setAttribute( 'href', '#disambig' );
        link.className = 'disambig_button';
        link.id = 'disambig_button';
        wrapper.appendChild( link );
        var i = 0,
            len = hatnotes.length;
        for (; i < len; i++) {
            wrapper.appendChild( hatnotes[i] );
        }
        container.appendChild( wrapper );
    }
    return content;
} );

},{"./transformer":12}],4:[function(require,module,exports){
var actions = require('./actions');
var bridge = require('./bridge');

actions.register( "edit_section", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { sectionID: el.getAttribute( 'data-id' ) } );
    event.preventDefault();
} );

},{"./actions":1,"./bridge":2}],5:[function(require,module,exports){
var transformer = require('./transformer');

transformer.register( 'displayIssuesLink', function( content ) {
    var issues = content.querySelectorAll( "table.ambox:not([class*='ambox-multiple_issues']):not([class*='ambox-notice'])" );
    if ( issues.length > 0 ) {
        var el = issues[0];
        var container = document.getElementById( "issues_container" );
        var wrapper = document.createElement( 'div' );
        var link = document.createElement( 'a' );
        link.setAttribute( 'href', '#issues' );
        link.className = 'issues_button';
        link.id = 'issues_button';
        wrapper.appendChild( link );
        el.parentNode.replaceChild( wrapper, el );
        var i = 0,
            len = issues.length;
        for (; i < len; i++) {
            wrapper.appendChild( issues[i] );
        }
        container.appendChild( wrapper );
    }
    return content;
} );

},{"./transformer":12}],6:[function(require,module,exports){
var bridge = require( "./bridge" );

function addStyleLink( href ) {
    var link = document.createElement( "link" );
    link.setAttribute( "rel", "stylesheet" );
    link.setAttribute( "type", "text/css" );
    link.setAttribute( "charset", "UTF-8" );
    link.setAttribute( "href", href );
    document.getElementsByTagName( "head" )[0].appendChild( link );
}

bridge.registerListener( "injectStyles", function( payload ) {
    var style_paths = payload.style_paths;
    for ( var i = 0; i < style_paths.length; i++ ) {
        addStyleLink( style_paths[i] );
    }
});

module.exports = {
	addStyleLink: addStyleLink
};
},{"./bridge":2}],7:[function(require,module,exports){
var bridge = require( "./bridge" );

bridge.registerListener( "requestImagesList", function( payload ) {
    var imageURLs = [];
    var images = document.querySelectorAll( "img" );
    for ( var i = 0; i < images.length; i++ ) {
        if (images[i].width < payload.minsize || images[i].height < payload.minsize) {
            continue;
        }
        imageURLs.push( images[i].src );
    }
    bridge.sendMessage( "imagesListResponse", { "images": imageURLs });
} );

// reusing this function
function replaceImageSrc( payload ) {
    var images = document.querySelectorAll( "img[src='" + payload.originalURL + "']" );
    for ( var i = 0; i < images.length; i++ ) {
        var img = images[i];
        img.setAttribute( "src", payload.newURL );
        img.setAttribute( "data-old-src", payload.originalURL );
        img.removeAttribute( "srcset" );
    }
}
bridge.registerListener( "replaceImageSrc", replaceImageSrc );

bridge.registerListener( "replaceImageSources", function( payload ) {
    for ( var i = 0; i < payload.img_map.length; i++ ) {
        replaceImageSrc( payload.img_map[i] );
    }
} );

bridge.registerListener( "setPageProtected", function( payload ) {
    var el = document.getElementsByTagName( "html" )[0];
    if (!el.classList.contains("page-protected") && payload.protect) {
        el.classList.add("page-protected");
    }
    else if (el.classList.contains("page-protected") && !payload.protect) {
        el.classList.remove("page-protected");
    }
    if (!el.classList.contains("no-editing") && payload.noedit) {
        el.classList.add("no-editing");
    }
    else if (el.classList.contains("no-editing") && !payload.noedit) {
        el.classList.remove("no-editing");
    }
} );

},{"./bridge":2}],8:[function(require,module,exports){
var bridge = require("./bridge");
var loader = require("./loader");
var utilities = require("./utilities");

function setImageBackgroundsForDarkMode( content ) {
	var img, allImgs = content.querySelectorAll( 'img' );
	for ( var i = 0; i < allImgs.length; i++ ) {
		img = allImgs[i];
		if ( likelyExpectsLightBackground( img ) ) {
			img.style.background = '#fff';
		}
	}
	// and now, look for Math formula images, and invert them
	var mathImgs = content.querySelectorAll( "[class*='math-fallback']" );
	for ( i = 0; i < mathImgs.length; i++ ) {
		var mathImg = mathImgs[i];
		// KitKat and higher can use webkit to invert colors
		if (window.apiLevel >= 19) {
			mathImg.style.cssText = mathImg.style.cssText + ";-webkit-filter: invert(100%);";
		} else {
			// otherwise, just give it a mild background color
			mathImg.style.backgroundColor = "#ccc";
			// and give it a little padding, since the text is right up against the edge.
			mathImg.style.padding = "2px";
		}
	}
}

/**
/ An heuristic for determining whether an element tagged 'img' is likely to need a white background applied
/ (provided a predefined background color is not supplied).
/
/ Based on trial, error and observation, this is likely to be the case when a background color is not
/ explicitly supplied, and:
/
/ (1) The element is in the infobox; or
/ (2) The element is not in a table.  ('img' elements in tables are frequently generated by random
/ 		templates and should not be altered; see, e.g., T85646).
*/
function likelyExpectsLightBackground( element ) {
	return !hasPredefinedBackgroundColor( element ) && ( isInfoboxImage( element ) || isNotInTable( element ) );
}

function hasPredefinedBackgroundColor( element ) {
	return utilities.ancestorHasStyleProperty( element, 'background-color' );
}

function isInfoboxImage( element ) {
	return utilities.ancestorContainsClass( element, 'image' ) && utilities.ancestorContainsClass( element, 'infobox' );
}

function isNotInTable( element ) {
	return !utilities.isNestedInTable( element );
}

function toggle( nightCSSURL, hasPageLoaded ) {
	window.isNightMode = !window.isNightMode;

	// Remove the <style> tag if it exists, add it otherwise
	var nightStyle = document.querySelector( "link[href='" + nightCSSURL + "']" );
	console.log( nightCSSURL );
	if ( nightStyle ) {
		nightStyle.parentElement.removeChild( nightStyle );
	} else {
		loader.addStyleLink( nightCSSURL );
	}

	if ( hasPageLoaded ) {
		// If we are doing this before the page has loaded, no need to swap colors ourselves
		// If we are doing this after, that means the transforms in transformers.js won't run
		// And we have to do this ourselves
		setImageBackgroundsForDarkMode( document.querySelector( '.content' ) );
	}
}

bridge.registerListener( 'toggleNightMode', function( payload ) {
	toggle( payload.nightStyleBundle.style_paths[0], payload.hasPageLoaded );
} );

module.exports = {
	setImageBackgroundsForDarkMode: setImageBackgroundsForDarkMode
};

},{"./bridge":2,"./loader":6,"./utilities":24}],9:[function(require,module,exports){
var bridge = require("./bridge");

bridge.registerListener( "displayPreviewHTML", function( payload ) {
    var content = document.getElementById( "content" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.html;
} );

},{"./bridge":2}],10:[function(require,module,exports){
var bridge = require("./bridge");

bridge.registerListener( "setDirectionality", function( payload ) {
    window.directionality = payload.contentDirection;
    var html = document.getElementsByTagName( "html" )[0];
    // first, remove all the possible directionality classes...
    html.classList.remove( "content-rtl" );
    html.classList.remove( "content-ltr" );
    html.classList.remove( "ui-rtl" );
    html.classList.remove( "ui-ltr" );
    // and then set the correct class based on our payload.
    html.classList.add( "content-" + window.directionality );
    html.classList.add( "ui-" + payload.uiDirection );
} );

},{"./bridge":2}],11:[function(require,module,exports){
var bridge = require("./bridge");
var transformer = require("./transformer");

bridge.registerListener( "clearContents", function() {
    clearContents();
});

bridge.registerListener( "setMargins", function( payload ) {
    document.getElementById( "content" ).style.marginLeft = payload.marginLeft + "px";
    document.getElementById( "content" ).style.marginRight = payload.marginRight + "px";
});

bridge.registerListener( "setPaddingTop", function( payload ) {
    document.getElementById( "content" ).style.paddingTop = payload.paddingTop + "px";
});

bridge.registerListener( "setPaddingBottom", function( payload ) {
    document.getElementById( "content" ).style.paddingBottom = payload.paddingBottom + "px";
});

bridge.registerListener( "beginNewPage", function( payload ) {
    clearContents();
    // fire an event back to the app, but with a slight timeout, which should
    // have the effect of "waiting" until the page contents have cleared before sending the
    // event, allowing synchronization of sorts between the WebView and the app.
    // (If we find a better way to synchronize the two, it can be done here, as well)
    setTimeout( function() {
        bridge.sendMessage( "onBeginNewPage", payload );
    }, 10);
});

function getLeadParagraph() {
    var text = "";
    var plist = document.getElementsByTagName( "p" );
    if (plist.length > 0) {
        text = plist[0].innerText;
    }
    return text;
}

// Returns currently highlighted text.
// If fewer than two characters are highlighted, returns the text of the first paragraph.
bridge.registerListener( "getTextSelection", function( payload ) {
    var text = window.getSelection().toString().trim();
    if (text.length < 2) {
        text = getLeadParagraph();
    }
    if (text.length > 250) {
        text = text.substring(0, 249);
    }
    bridge.sendMessage( "onGetTextSelection", { "purpose" : payload.purpose, "text" : text } );
});

bridge.registerListener( "displayLeadSection", function( payload ) {
    // This might be a refresh! Clear out all contents!
    clearContents();

    // create an empty div to act as the title anchor
    var titleDiv = document.createElement( "div" );
    titleDiv.id = "heading_" + payload.section.id;
    titleDiv.setAttribute( "data-id", 0 );
    titleDiv.className = "section_heading";
    document.getElementById( "content" ).appendChild( titleDiv );

    var issuesContainer = document.createElement( "div" );
    issuesContainer.setAttribute( "dir", window.directionality );
    issuesContainer.id = "issues_container";
    issuesContainer.className = "issues_container";
    document.getElementById( "content" ).appendChild( issuesContainer );

    var editButton = buildEditSectionButton( payload.section.id );

    var content = document.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.section.text;
    content.id = "content_block_0";

    window.apiLevel = payload.apiLevel;
    window.string_table_infobox = payload.string_table_infobox;
    window.string_table_other = payload.string_table_other;
    window.string_table_close = payload.string_table_close;
    window.string_expand_refs = payload.string_expand_refs;
    window.pageTitle = payload.title;
    window.isMainPage = payload.isMainPage;
    window.isBeta = payload.isBeta;
    window.siteLanguage = payload.siteLanguage;

    // append the content to the DOM now, so that we can obtain
    // dimension measurements for items.
    document.getElementById( "content" ).appendChild( content );

    transformer.transform( "moveFirstGoodParagraphUp" );
    transformer.transform( "addDarkModeStyles", content );
    transformer.transform( "hideRedLinks", content );
    transformer.transform( "setNonGbDivWidth", content );
    transformer.transform( "setMathFormulaImageMaxWidth", content );
    transformer.transform( "anchorPopUpMediaTransforms", content );
    transformer.transform( "hideIPA", content );

    if (!window.isMainPage) {
        transformer.transform( "hideTables", content );
        transformer.transform( "addImageOverflowXContainers", content );
        transformer.transform( "widenImages", content );
    }

    // insert the edit pencil
    content.insertBefore( editButton, content.firstChild );

    transformer.transform("displayDisambigLink", content);
    transformer.transform("displayIssuesLink", content);

    //if there were no page issues, then hide the container
    if (!issuesContainer.hasChildNodes()) {
        document.getElementById( "content" ).removeChild(issuesContainer);
    }
    //update the text of the disambiguation link, if there is one
    var disambigBtn = document.getElementById( "disambig_button" );
    if (disambigBtn !== null) {
        disambigBtn.innerText = payload.string_page_similar_titles;
    }
    //update the text of the page-issues link, if there is one
    var issuesBtn = document.getElementById( "issues_button" );
    if (issuesBtn !== null) {
        issuesBtn.innerText = payload.string_page_issues;
    }
    //if we have both issues and disambiguation, then insert the separator
    if (issuesBtn !== null && disambigBtn !== null) {
        var separator = document.createElement( 'span' );
        separator.innerText = '|';
        separator.className = 'issues_separator';
        issuesContainer.insertBefore(separator, issuesBtn.parentNode);
    }

    document.getElementById( "loading_sections").className = "loading";
    scrolledOnLoad = false;
});

function clearContents() {
    document.getElementById( "content" ).innerHTML = "";
    window.scrollTo( 0, 0 );
}

function buildEditSectionButton(id) {
    var editButtonWrapper = document.createElement( "span" );
    editButtonWrapper.className = "edit_section_button_wrapper android";
    var editButton = document.createElement( "a" );
    editButton.setAttribute( 'data-id', id );
    editButton.setAttribute( 'data-action', "edit_section" );
    editButton.className = "edit_section_button android";
    editButtonWrapper.appendChild( editButton );
    return editButtonWrapper;
}

function elementsForSection( section ) {
    var heading = document.createElement( "h" + ( section.toclevel + 1 ) );
    heading.setAttribute( "dir", window.directionality );
    heading.innerHTML = typeof section.line !== "undefined" ? section.line : "";
    heading.id = section.anchor;
    heading.className = "section_heading";
    heading.setAttribute( 'data-id', section.id );

    heading.appendChild( buildEditSectionButton( section.id ) );

    var content = document.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = section.text;
    content.id = "content_block_" + section.id;
    transformer.transform( "addDarkModeStyles", content );
    transformer.transform( "hideRedLinks", content );
    transformer.transform( "setNonGbDivWidth", content );
    transformer.transform( "setMathFormulaImageMaxWidth", content );
    transformer.transform( "anchorPopUpMediaTransforms", content );
    transformer.transform( "hideIPA", content );
    transformer.transform( "hideRefs", content );
    if (!window.isMainPage) {
        transformer.transform( "hideTables", content );
        transformer.transform( "addImageOverflowXContainers", content );
        transformer.transform( "widenImages", content );
    }

    return [ heading, content ];
}

var scrolledOnLoad = false;

bridge.registerListener( "displaySection", function ( payload ) {
    if ( payload.noMore ) {
        // if we still haven't scrolled to our target offset (if we have one),
        // then do it now.
        if (payload.scrollY > 0 && !scrolledOnLoad) {
            window.scrollTo( 0, payload.scrollY );
            scrolledOnLoad = true;
        }
        document.getElementById( "loading_sections").className = "";
        bridge.sendMessage( "pageLoadComplete", { "sequence": payload.sequence, "savedPage": payload.savedPage } );
    } else {
        var contentWrapper = document.getElementById( "content" );
        elementsForSection(payload.section).forEach(function (element) {
            contentWrapper.appendChild(element);
            // do we have a y-offset to scroll to?
            if (payload.scrollY > 0 && payload.scrollY < element.offsetTop && !scrolledOnLoad) {
                window.scrollTo( 0, payload.scrollY );
                scrolledOnLoad = true;
            }
        });
        // do we have a section to scroll to?
        if ( typeof payload.fragment === "string" && payload.fragment.length > 0 && payload.section.anchor === payload.fragment) {
            scrollToSection( payload.fragment );
        }
        bridge.sendMessage( "requestSection", { "sequence": payload.sequence, "savedPage": payload.savedPage, "index": payload.section.id + 1 });
    }
});

bridge.registerListener( "scrollToSection", function ( payload ) {
    scrollToSection( payload.anchor );
});

function scrollToSection( anchor ) {
    if (anchor === "heading_0") {
        // if it's the first section, then scroll all the way to the top, since there could
        // be a lead image, native title components, etc.
        window.scrollTo( 0, 0 );
    } else {
        var el = document.getElementById( anchor );
        var scrollY = el.offsetTop - 48;
        window.scrollTo( 0, scrollY );
    }
}

bridge.registerListener( "scrollToBottom", function () {
    window.scrollTo(0, document.body.scrollHeight);
});

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

},{"./bridge":2,"./transformer":12}],12:[function(require,module,exports){
function Transformer() {
}

var transforms = {};

Transformer.prototype.register = function( transform, fun ) {
    if ( transform in transforms ) {
        transforms[transform].push( fun );
    } else {
        transforms[transform] = [ fun ];
    }
};

Transformer.prototype.transform = function( transform, element ) {
    var functions = transforms[transform];
    for ( var i = 0; i < functions.length; i++ ) {
        element = functions[i](element);
    }
};

module.exports = new Transformer();

},{}],13:[function(require,module,exports){
var transformer = require("../transformer");
var night = require("../night");

transformer.register( "addDarkModeStyles", function( content ) {
	if ( window.isNightMode ) {
		night.setImageBackgroundsForDarkMode ( content );
	}
} );
},{"../night":8,"../transformer":12}],14:[function(require,module,exports){
var transformer = require("../transformer");
var utilities = require("../utilities");

function shouldAddImageOverflowXContainer(image) {
    if ((image.width > document.getElementById('content').offsetWidth) && !utilities.isNestedInTable(image)) {
        return true;
    } else {
        return false;
    }
}

function addImageOverflowXContainer(image, ancestor) {
    image.setAttribute('hasOverflowXContainer', 'true'); // So "widenImages" transform knows instantly not to widen this one.
    var div = document.createElement( 'div' );
    div.className = 'image_overflow_x_container';
    ancestor.parentElement.insertBefore( div, ancestor );
    div.appendChild(ancestor);
}

function maybeAddImageOverflowXContainer() {
    var image = this;
    if (shouldAddImageOverflowXContainer(image)) {
        var ancestor = utilities.firstAncestorWithMultipleChildren(image);
        if (ancestor) {
            addImageOverflowXContainer(image, ancestor);
        }
    }
}

transformer.register( "addImageOverflowXContainers", function( content ) {
    // Wrap wide images in a <div style="overflow-x:auto">...</div> so they can scroll
    // side to side if needed without causing the entire section to scroll side to side.
    var images = content.getElementsByTagName('img');
    for (var i = 0; i < images.length; ++i) {
        // Load event used so images w/o style or inline width/height
        // attributes can still have their size determined reliably.
        images[i].addEventListener('load', maybeAddImageOverflowXContainer, false);
    }
} );
},{"../transformer":12,"../utilities":24}],15:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "anchorPopUpMediaTransforms", function( content ) {
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
} );
},{"../transformer":12}],16:[function(require,module,exports){
var transformer = require("../transformer");

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

function handleTableCollapseOrExpandClick() {
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
        collapsedDiv.onclick = handleTableCollapseOrExpandClick;
        bottomDiv.onclick = handleTableCollapseOrExpandClick;
    }
} );

module.exports = {
    handleTableCollapseOrExpandClick: handleTableCollapseOrExpandClick
};
},{"../transformer":12}],17:[function(require,module,exports){
var transformer = require("../transformer");
var bridge = require("../bridge");

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
} );
},{"../bridge":2,"../transformer":12}],18:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "hideRedLinks", function( content ) {
	var redLinks = content.querySelectorAll( 'a.new' );
	for ( var i = 0; i < redLinks.length; i++ ) {
		var redLink = redLinks[i];
		var replacementSpan = document.createElement( 'span' );
		replacementSpan.innerHTML = redLink.innerHTML;
		replacementSpan.setAttribute( 'class', redLink.getAttribute( 'class' ) );
		redLink.parentNode.replaceChild( replacementSpan, redLink );
	}
} );
},{"../transformer":12}],19:[function(require,module,exports){
var transformer = require("../transformer");
var collapseTables = require("./collapseTables");

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
        collapsedDiv.onclick = collapseTables.handleTableCollapseOrExpandClick;
        bottomDiv.onclick = collapseTables.handleTableCollapseOrExpandClick;
    }
} );
},{"../transformer":12,"./collapseTables":16}],20:[function(require,module,exports){
var transformer = require("../transformer");

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
transformer.register( "moveFirstGoodParagraphUp", function() {
    if (window.isMainPage) {
        // don't do anything if this is the main page, since many wikis
        // arrange the main page in a series of tables.
        return;
    }
    var block_0 = document.getElementById( "content_block_0" );
    if (!block_0) {
        return;
    }

    var allPs = block_0.getElementsByTagName( "p" );
    if (!allPs) {
        return;
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
} );
},{"../transformer":12}],21:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "setMathFormulaImageMaxWidth", function( content ) {
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
} );
},{"../transformer":12}],22:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "setNonGbDivWidth", function( content ) {
    if (window.apiLevel < 11) {
        //don't do anything for GB
        return;
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
} );
},{"../transformer":12}],23:[function(require,module,exports){
var transformer = require("../transformer");
var utilities = require("../utilities");

var maxStretchRatioAllowedBeforeRequestingHigherResolution = 1.3;

// If enabled, widened images will have thin red dashed border and
// and widened images for which a higher resolution version was
// requested will have thick red dashed border.
var enableDebugBorders = false;

function widenAncestors (el) {
    while ((el = el.parentElement) && !el.classList.contains('content_block')) {
        // Only widen if there was a width setting. Keeps changes minimal.
        if (el.style.width) {
            el.style.width = '100%';
        }
        if (el.style.maxWidth) {
            el.style.maxWidth = '100%';
        }
        if (el.style.float) {
            el.style.float = 'none';
        }
    }
}

function shouldWidenImage(image) {
    if (
        image.width >= 64 &&
        image.hasAttribute('srcset') &&
        !image.hasAttribute('hasOverflowXContainer') &&
        image.parentNode.className === "image" &&
        !utilities.isNestedInTable(image)
        ) {
        return true;
    } else {
        return false;
    }
}

function makeRoomForImageWidening(image) {
    // Expand containment so css wideImageOverride width percentages can take effect.
    widenAncestors (image);

    // Remove width and height attributes so wideImageOverride width percentages can take effect.
    image.removeAttribute("width");
    image.removeAttribute("height");
}

function getStretchRatio(image) {
    var widthControllingDiv = utilities.firstDivAncestor(image);
    if (widthControllingDiv) {
        return (widthControllingDiv.offsetWidth / image.naturalWidth);
    }
    return 1.0;
}

function useHigherResolutionImageSrcFromSrcsetIfNecessary(image) {
    if (image.getAttribute('srcset')) {
        var stretchRatio = getStretchRatio(image);
        if (stretchRatio > maxStretchRatioAllowedBeforeRequestingHigherResolution) {
            var srcsetDict = utilities.getDictionaryFromSrcset(image.getAttribute('srcset'));
            /*
            Grab the highest res url from srcset - avoids the complexity of parsing urls
            to retrieve variants - which can get tricky - canonicals have different paths
            than size variants
            */
            var largestSrcsetDictKey = Object.keys(srcsetDict).reduce(function(a, b) {
              return a > b ? a : b;
            });

            image.src = srcsetDict[largestSrcsetDictKey];

            if (enableDebugBorders) {
                image.style.borderWidth = '10px';
            }
        }
    }
}

function widenImage(image) {
    makeRoomForImageWidening (image);
    image.classList.add("wideImageOverride");

    if (enableDebugBorders) {
        image.style.borderStyle = 'dashed';
        image.style.borderWidth = '1px';
        image.style.borderColor = '#f00';
    }

    useHigherResolutionImageSrcFromSrcsetIfNecessary(image);
}

function maybeWidenImage() {
    var image = this;
    if (shouldWidenImage(image)) {
        widenImage(image);
    }
}

transformer.register( "widenImages", function( content ) {
    var images = content.querySelectorAll( 'img' );
    for ( var i = 0; i < images.length; i++ ) {
        // Load event used so images w/o style or inline width/height
        // attributes can still have their size determined reliably.
        images[i].addEventListener('load', maybeWidenImage, false);
    }
} );
},{"../transformer":12,"../utilities":24}],24:[function(require,module,exports){

function hasAncestor( el, tagName ) {
    if (el !== null && el.tagName === tagName) {
        return true;
    } else {
        if ( el.parentNode !== null && el.parentNode.tagName !== 'BODY' ) {
            return hasAncestor( el.parentNode, tagName );
        } else {
            return false;
        }
    }
}

function ancestorContainsClass( element, className ) {
    var contains = false;
    var curNode = element;
    while (curNode) {
        if (typeof curNode.classList !== "undefined") {
            if (curNode.classList.contains(className)) {
                contains = true;
                break;
            }
        }
        curNode = curNode.parentNode;
    }
    return contains;
}

function ancestorHasStyleProperty( element, styleProperty ) {
    var hasStyleProperty = false;
    var curNode = element;
    while (curNode) {
        if (typeof curNode.classList !== "undefined") {
            if (curNode.style[styleProperty]) {
                hasStyleProperty = true;
                break;
            }
        }
        curNode = curNode.parentNode;
    }
    return hasStyleProperty;
}

function getDictionaryFromSrcset(srcset) {
    /*
    Returns dictionary with density (without "x") as keys and urls as values.
    Parameter 'srcset' string:
        '//image1.jpg 1.5x, //image2.jpg 2x, //image3.jpg 3x'
    Returns dictionary:
        {1.5: '//image1.jpg', 2: '//image2.jpg', 3: '//image3.jpg'}
    */
    var sets = srcset.split(',').map(function(set) {
        return set.trim().split(' ');
    });
    var output = {};
    sets.forEach(function(set) {
        output[set[1].replace('x', '')] = set[0];
    });
    return output;
}

function firstDivAncestor (el) {
    while ((el = el.parentElement)) {
        if (el.tagName === 'DIV') {
            return el;
        }
    }
    return null;
}

function firstAncestorWithMultipleChildren (el) {
    while ((el = el.parentElement) && (el.childElementCount === 1)){}
    return el;
}

function isNestedInTable(el) {
    while ((el = el.parentElement)) {
        if (el.tagName === 'TD') {
            return true;
        }
    }
    return false;
}

module.exports = {
    hasAncestor: hasAncestor,
    ancestorContainsClass: ancestorContainsClass,
    ancestorHasStyleProperty: ancestorHasStyleProperty,
    getDictionaryFromSrcset: getDictionaryFromSrcset,
    firstDivAncestor: firstDivAncestor,
    isNestedInTable: isNestedInTable,
    firstAncestorWithMultipleChildren: firstAncestorWithMultipleChildren
};

},{}],25:[function(require,module,exports){
/**
 * MIT LICENSCE
 * From: https://github.com/remy/polyfills
 * FIXME: Don't copy paste libraries, use a dep management system.
 */
(function () {

if (typeof window.Element === "undefined" || "classList" in document.documentElement) return;

var prototype = Array.prototype,
    push = prototype.push,
    splice = prototype.splice,
    join = prototype.join;

function DOMTokenList(el) {
  this.el = el;
  // The className needs to be trimmed and split on whitespace
  // to retrieve a list of classes.
  var classes = el.className.replace(/^\s+|\s+$/g,'').split(/\s+/);
  for (var i = 0; i < classes.length; i++) {
    push.call(this, classes[i]);
  }
};

DOMTokenList.prototype = {
  add: function(token) {
    if(this.contains(token)) return;
    push.call(this, token);
    this.el.className = this.toString();
  },
  contains: function(token) {
    return this.el.className.indexOf(token) != -1;
  },
  item: function(index) {
    return this[index] || null;
  },
  remove: function(token) {
    if (!this.contains(token)) return;
    for (var i = 0; i < this.length; i++) {
      if (this[i] == token) break;
    }
    splice.call(this, i, 1);
    this.el.className = this.toString();
  },
  toString: function() {
    return join.call(this, ' ');
  },
  toggle: function(token) {
    if (!this.contains(token)) {
      this.add(token);
    } else {
      this.remove(token);
    }

    return this.contains(token);
  }
};

window.DOMTokenList = DOMTokenList;

function defineElementGetter (obj, prop, getter) {
    if (Object.defineProperty) {
        Object.defineProperty(obj, prop,{
            get : getter
        });
    } else {
        obj.__defineGetter__(prop, getter);
    }
}

defineElementGetter(Element.prototype, 'classList', function () {
  return new DOMTokenList(this);
});

})();

},{}],26:[function(require,module,exports){
var bridge = require("../js/bridge");
bridge.registerListener( "injectScript", function( payload ) {
    require(payload.src);
});
},{"../js/bridge":2}],27:[function(require,module,exports){
var bridge = require("../js/bridge");
console.log("Something!");
bridge.registerListener( "ping", function( payload ) {
    bridge.sendMessage( "pong", payload );
});

},{"../js/bridge":2}]},{},[2,7,24,12,13,14,15,16,17,18,19,20,21,22,23,1,3,4,5,6,8,9,10,11,25,26,27])
