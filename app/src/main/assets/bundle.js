(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
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

/**
 * Either gets the title from the title attribute (for mobileview case and newer MCS pages) or,
 * if that doesn't not exists try to derive it from the href attribute value.
 * In the latter case it also unescapes HTML entities to get the correct title string.
 */
function getTitle( sourceNode, href ) {
    if (sourceNode.hasAttribute( "title" )) {
        return sourceNode.getAttribute( "title" );
    } else {
        return href.replace(/^\/wiki\//, '').replace(/^\.\//, '').replace(/#.*$/, '');
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
                handleReference( targetId, util.ancestorContainsClass( sourceNode, "mw-cite-backlink" ) );
            } else if (sourceNode.classList.contains( 'app_media' )) {
                bridge.sendMessage( 'mediaClicked', { "href": href } );
            } else if (sourceNode.classList.contains( 'image' )) {
                bridge.sendMessage( 'imageClicked', { "href": href } );
            } else {
                bridge.sendMessage( 'linkClicked', { "href": href, "title": getTitle(sourceNode, href) } );
            }
            event.preventDefault();
        }
    }
};

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
module.exports = {
    DARK_STYLE_FILENAME: "file:///android_asset/dark.css"
}
},{}],4:[function(require,module,exports){
var bridge = require("./bridge");
var constant = require("./constant");
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
/         templates and should not be altered; see, e.g., T85646).
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

function toggle( darkCSSURL, hasPageLoaded ) {
    window.isDarkMode = !window.isDarkMode;

    // Remove the <style> tag if it exists, add it otherwise
    var darkStyle = document.querySelector( "link[href='" + darkCSSURL + "']" );
    if ( darkStyle ) {
        darkStyle.parentElement.removeChild( darkStyle );
    } else {
        loader.addStyleLink( darkCSSURL );
    }

    if ( hasPageLoaded ) {
        // If we are doing this before the page has loaded, no need to swap colors ourselves
        // If we are doing this after, that means the transforms in transformers.js won't run
        // And we have to do this ourselves
        setImageBackgroundsForDarkMode( document.querySelector( '.content' ) );
    }
}

bridge.registerListener( 'toggleDarkMode', function( payload ) {
    toggle( constant.DARK_STYLE_FILENAME, payload.hasPageLoaded );
} );

module.exports = {
    setImageBackgroundsForDarkMode: setImageBackgroundsForDarkMode
};

},{"./bridge":2,"./constant":3,"./loader":8,"./utilities":24}],5:[function(require,module,exports){
var transformer = require('./transformer');

transformer.register( 'displayDisambigLink', function( content ) {
    var hatnotes = content.querySelectorAll( "div.hatnote" );
    if ( hatnotes.length > 0 ) {
        var container = document.getElementById( "issues_container" );
        var wrapper = document.createElement( 'div' );
        var i = 0,
            len = hatnotes.length;
        for (; i < len; i++) {
            wrapper.appendChild( hatnotes[i] );
        }
        container.appendChild( wrapper );
    }
    return content;
} );

},{"./transformer":14}],6:[function(require,module,exports){
var actions = require('./actions');
var bridge = require('./bridge');

actions.register( "edit_section", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { sectionID: el.getAttribute( 'data-id' ) } );
    event.preventDefault();
} );

},{"./actions":1,"./bridge":2}],7:[function(require,module,exports){
var transformer = require('./transformer');

transformer.register( 'displayIssuesLink', function( content ) {
    var issues = content.querySelectorAll( "table.ambox:not([class*='ambox-multiple_issues']):not([class*='ambox-notice'])" );
    if ( issues.length > 0 ) {
        var el = issues[0];
        var container = document.getElementById( "issues_container" );
        var wrapper = document.createElement( 'div' );
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

},{"./transformer":14}],8:[function(require,module,exports){
function addStyleLink( href ) {
    var link = document.createElement( "link" );
    link.setAttribute( "rel", "stylesheet" );
    link.setAttribute( "type", "text/css" );
    link.setAttribute( "charset", "UTF-8" );
    link.setAttribute( "href", href );
    document.getElementsByTagName( "head" )[0].appendChild( link );
}

module.exports = {
    addStyleLink: addStyleLink
};
},{}],9:[function(require,module,exports){
var bridge = require( "./bridge" );
var transformer = require("./transformer");

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

bridge.registerListener( "setDecorOffset", function( payload ) {
    transformer.setDecorOffset(payload.offset);
} );

},{"./bridge":2,"./transformer":14}],10:[function(require,module,exports){
var bridge = require("./bridge");

/*
OnClick handler function for IPA spans.
*/
function ipaClickHandler() {
    var container = this;
    bridge.sendMessage( "ipaSpan", { "contents": container.innerHTML });
}

function addIPAonClick( content ) {
    var spans = content.querySelectorAll( "span.ipa_button" );
    for (var i = 0; i < spans.length; i++) {
        var parent = spans[i].parentNode;
        parent.onclick = ipaClickHandler;
    }
}

module.exports = {
    addIPAonClick: addIPAonClick
};
},{"./bridge":2}],11:[function(require,module,exports){
var bridge = require("./bridge");

bridge.registerListener( "displayPreviewHTML", function( payload ) {
    var content = document.getElementById( "content" );
    document.head.getElementsByTagName("base")[0].setAttribute("href", payload.siteBaseUrl);
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.html;
} );

},{"./bridge":2}],12:[function(require,module,exports){
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

},{"./bridge":2}],13:[function(require,module,exports){
var bridge = require("./bridge");
var transformer = require("./transformer");
var clickHandlerSetup = require("./onclick");

bridge.registerListener( "clearContents", function() {
    clearContents();
});

bridge.registerListener( "setMargins", function( payload ) {
    document.getElementById( "content" ).style.marginTop = payload.marginTop + "px";
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
    if (text.length < 2 && payload.purpose === "share") {
        text = getLeadParagraph();
    }
    if (text.length > 250) {
        text = text.substring(0, 249);
    }
    if (payload.purpose === "edit_here") {
        var range = window.getSelection().getRangeAt(0);
        var newRangeStart = Math.max(0, range.startOffset - 20);
        range.setStart(range.startContainer, newRangeStart);
        text = range.toString();
    }
    bridge.sendMessage( "onGetTextSelection", { "purpose" : payload.purpose, "text" : text, "sectionID" : getCurrentSection() } );
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
    document.getElementById( "content" ).appendChild( issuesContainer );

    var content = document.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.section.text;
    content.id = "content_block_0";

    document.head.getElementsByTagName("base")[0].setAttribute("href", payload.siteBaseUrl);
    window.apiLevel = payload.apiLevel;
    window.string_table_infobox = payload.string_table_infobox;
    window.string_table_other = payload.string_table_other;
    window.string_table_close = payload.string_table_close;
    window.string_expand_refs = payload.string_expand_refs;
    window.pageTitle = payload.title;
    window.isMainPage = payload.isMainPage;
    window.fromRestBase = payload.fromRestBase;
    window.isBeta = payload.isBeta;
    window.siteLanguage = payload.siteLanguage;
    window.isNetworkMetered = payload.isNetworkMetered;

    // append the content to the DOM now, so that we can obtain
    // dimension measurements for items.
    document.getElementById( "content" ).appendChild( content );

    // Content service transformations
    if (!window.fromRestBase) {
        transformer.transform( "moveFirstGoodParagraphUp" );

        transformer.transform( "hideRedLinks", content );
        transformer.transform( "anchorPopUpMediaTransforms", content );
        transformer.transform( "hideIPA", content );
    } else {
        clickHandlerSetup.addIPAonClick( content );
    }

    // client only transformations:
    transformer.transform( "addDarkModeStyles", content ); // client setting
    transformer.transform( "setDivWidth", content ); // offsetWidth

    if (!window.isMainPage) {
        transformer.transform( "hideTables", content ); // clickHandler

        if (!window.isNetworkMetered) {
            transformer.transform( "widenImages", content ); // offsetWidth
        }
    }

    transformer.transform("displayDisambigLink", content);
    transformer.transform("displayIssuesLink", content);

    document.getElementById( "loading_sections").className = "loading";

    bridge.sendMessage( "pageInfo", {
      "issues" : collectIssues(),
      "disambiguations" : collectDisambig()
    });
    //if there were no page issues, then hide the container
    if (!issuesContainer.hasChildNodes()) {
        document.getElementById( "content" ).removeChild(issuesContainer);
    }

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

    // Content service transformations
    if (!window.fromRestBase) {
        transformer.transform( "hideRedLinks", content );
        transformer.transform( "anchorPopUpMediaTransforms", content );
        transformer.transform( "hideIPA", content );
    } else {
        clickHandlerSetup.addIPAonClick( content );
    }

    transformer.transform( "addDarkModeStyles", content ); // client setting
    transformer.transform( "setDivWidth", content ); // offsetWidth

    transformer.transform( "hideRefs", content ); // clickHandler

    if (!window.isMainPage) {
        transformer.transform( "hideTables", content ); // clickHandler

        if (!window.isNetworkMetered) {
            transformer.transform( "widenImages", content ); // offsetWidth
        }
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
        bridge.sendMessage( "pageLoadComplete", {
          "sequence": payload.sequence });
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
        bridge.sendMessage( "requestSection", { "sequence": payload.sequence, "index": payload.section.id + 1 });
    }
});

bridge.registerListener( "scrollToSection", function ( payload ) {
    scrollToSection( payload.anchor );
});

function collectDisambig() {
    var res = [];
    var links = document.querySelectorAll( 'div.hatnote a' );
    var i = 0,
        len = links.length;
    for (; i < len; i++) {
        // Pass the href; we'll decode it into a proper page title in Java
        res.push( links[i].getAttribute( 'href' ) );
    }
    return res;
}

function collectIssues() {
    var res = [];
    var issues = document.querySelectorAll( 'table.ambox' );
    var i = 0,
        len = issues.length;
    for (; i < len; i++) {
        // .ambox- is used e.g. on eswiki
        res.push( issues[i].querySelector( '.mbox-text, .ambox-text' ).innerHTML );
    }
    return res;
}

function scrollToSection( anchor ) {
    if (anchor === "heading_0") {
        // if it's the first section, then scroll all the way to the top, since there could
        // be a lead image, native title components, etc.
        window.scrollTo( 0, 0 );
    } else {
        var el = document.getElementById( anchor );
        var scrollY = el.offsetTop - transformer.getDecorOffset();
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

},{"./bridge":2,"./onclick":10,"./transformer":14}],14:[function(require,module,exports){
function Transformer() {
}

var transforms = {};
var decorOffset = 0; // The height of the toolbar and, when translucent, status bar in CSS pixels.

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

Transformer.prototype.getDecorOffset = function() {
    return decorOffset;
};

Transformer.prototype.setDecorOffset = function(offset) {
    decorOffset = offset;
};

module.exports = new Transformer();
},{}],15:[function(require,module,exports){
var transformer = require("../transformer");
var dark = require("../dark");

transformer.register( "addDarkModeStyles", function( content ) {
    if ( window.isDarkMode ) {
        dark.setImageBackgroundsForDarkMode ( content );
    }
} );
},{"../dark":4,"../transformer":14}],16:[function(require,module,exports){
var pagelib = require("wikimedia-page-library");
var transformer = require("../transformer");

function scrollWithDecorOffset(container) {
    window.scrollTo( 0, container.parentNode.offsetTop - transformer.getDecorOffset() );
}

function toggleCollapseClickCallback() {
    pagelib.CollapseTable.toggleCollapseClickCallback.call(this, scrollWithDecorOffset);
}

transformer.register( "hideTables", function(content) {
    pagelib.CollapseTable.collapseTables(window, content, window.pageTitle,
        window.isMainPage, window.string_table_infobox,
        window.string_table_other, window.string_table_close,
        scrollWithDecorOffset);
});

module.exports = {
    handleTableCollapseOrExpandClick: toggleCollapseClickCallback
};

},{"../transformer":14,"wikimedia-page-library":25}],17:[function(require,module,exports){
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
},{"../transformer":14,"./collapseTables":16}],18:[function(require,module,exports){
var transformer = require("../../transformer");

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

},{"../../transformer":14}],19:[function(require,module,exports){
var transformer = require("../../transformer");
var bridge = require("../../bridge");

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
},{"../../bridge":2,"../../transformer":14}],20:[function(require,module,exports){
var transformer = require("../../transformer");

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
},{"../../transformer":14}],21:[function(require,module,exports){
var transformer = require("../../transformer");

// Move the first non-empty paragraph (and related elements) to the top of the section.
// This will have the effect of shifting the infobox and/or any images at the top of the page
// below the first paragraph, allowing the user to start reading the page right away.
transformer.register( "moveFirstGoodParagraphUp", function() {
    if ( window.isMainPage ) {
        // don't do anything if this is the main page, since many wikis
        // arrange the main page in a series of tables.
        return;
    }
    var block_0 = document.getElementById( "content_block_0" );
    if ( !block_0 ) {
        return;
    }

    var block_0_children = block_0.childNodes;
    if ( !block_0_children ) {
        return;
    }

    var leadSpan = createLeadSpan(block_0_children);
    block_0.insertBefore( leadSpan, block_0.firstChild );
} );

// Create a lead span to be moved to the top of the page, consisting of the first
// qualifying <p> element encountered and any subsequent non-<p> elements until
// the next <p> is encountered.
//
// Simply moving the first <p> element up may result in elements appearing
// between the first paragraph as designated by <p></p> tags and other elements
// (such as an unnumbered list) that may also be intended as part of the first
// display paragraph.  See T111958.
function createLeadSpan( childNodes ) {
    var leadSpan = document.createElement( 'span' );
    var firstGoodParagraphIndex = findFirstGoodParagraphIn( childNodes );

    if ( firstGoodParagraphIndex ) {
        addNode( leadSpan, childNodes[ firstGoodParagraphIndex ] );
        addTrailingNodes(leadSpan, childNodes, firstGoodParagraphIndex + 1 );
    }

    return leadSpan;
}

function findFirstGoodParagraphIn( nodes ) {
    var minParagraphHeight = 24;
    var firstGoodParagraphIndex;
    var i;

    for ( i = 0; i < nodes.length; i++ ) {
        if ( nodes[i].tagName === 'P' ) {
            // Ensure the P being pulled up has at least a couple lines of text.
            // Otherwise silly things like a empty P or P which only contains a
            // BR tag will get pulled up (see articles on "Chemical Reaction" and
            // "Hawaii").
            // Trick for quickly determining element height:
            // https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement.offsetHeight
            // http://stackoverflow.com/a/1343350/135557
            if ( nodes[i].offsetHeight < minParagraphHeight ){
                continue;
            }
            firstGoodParagraphIndex = i;
            break;
        }
    }

    return firstGoodParagraphIndex;
}

function addNode( span, node ) {
    span.appendChild( node.parentNode.removeChild( node ) );
}

function addTrailingNodes( span, nodes, startIndex ) {
    for ( var i = startIndex; i < nodes.length; i++ ) {
        if ( nodes[i].tagName === 'P' ) {
            break;
        }
        addNode( span, nodes[i] );
    }
}

},{"../../transformer":14}],22:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "setDivWidth", function( content ) {
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
},{"../transformer":14}],23:[function(require,module,exports){
var maybeWidenImage = require('wikimedia-page-library').WidenImage.maybeWidenImage;
var transformer = require("../transformer");
var utilities = require("../utilities");

var maxStretchRatioAllowedBeforeRequestingHigherResolution = 1.3;

function isGalleryImage(image) {
  return (
      image.width >= 64 &&
      image.hasAttribute('srcset') &&
      image.parentNode.className === "image"
    );
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
        }
    }
}

function onImageLoad() {
    var image = this;
    image.removeEventListener('load', onImageLoad, false);
    if (isGalleryImage(image) && maybeWidenImage(image)) {
        useHigherResolutionImageSrcFromSrcsetIfNecessary(image);
    }
}

transformer.register( "widenImages", function( content ) {
    var images = content.querySelectorAll( 'img' );
    for ( var i = 0; i < images.length; i++ ) {
        // Load event used so images w/o style or inline width/height
        // attributes can still have their size determined reliably.
        images[i].addEventListener('load', onImageLoad, false);
    }
} );

},{"../transformer":14,"../utilities":24,"wikimedia-page-library":25}],24:[function(require,module,exports){

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
(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
	typeof define === 'function' && define.amd ? define(factory) :
	(global.pagelib = factory());
}(this, (function () { 'use strict';

// This file exists for CSS packaging only. It imports the CSS which is to be
// packaged in the override CSS build product.

// todo: delete Empty.css when other overrides exist

/**
 * Polyfill function that tells whether a given element matches a selector.
 * @param {!Element} el Element
 * @param {!string} selector Selector to look for
 * @return {!boolean} Whether the element matches the selector
 */
var matchesSelector = function matchesSelector(el, selector) {
  if (el.matches) {
    return el.matches(selector);
  }
  if (el.matchesSelector) {
    return el.matchesSelector(selector);
  }
  if (el.webkitMatchesSelector) {
    return el.webkitMatchesSelector(selector);
  }
  return false;
};

// https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent#Polyfill
// Required by Android API 16 AOSP Nexus S emulator.
// eslint-disable-next-line no-undef
var CustomEvent = typeof window !== 'undefined' && window.CustomEvent || function (type) {
  var parameters = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : { bubbles: false, cancelable: false, detail: undefined };

  // eslint-disable-next-line no-undef
  var event = document.createEvent('CustomEvent');
  event.initCustomEvent(type, parameters.bubbles, parameters.cancelable, parameters.detail);
  return event;
};

var Polyfill = { matchesSelector: matchesSelector, CustomEvent: CustomEvent };

/**
 * Returns closest ancestor of element which matches selector.
 * Similar to 'closest' methods as seen here:
 *  https://api.jquery.com/closest/
 *  https://developer.mozilla.org/en-US/docs/Web/API/Element/closest
 * @param  {!Element} el        Element
 * @param  {!string} selector   Selector to look for in ancestors of 'el'
 * @return {?HTMLElement}       Closest ancestor of 'el' matching 'selector'
 */
var findClosestAncestor = function findClosestAncestor(el, selector) {
  var parentElement = void 0;
  for (parentElement = el.parentElement; parentElement && !Polyfill.matchesSelector(parentElement, selector); parentElement = parentElement.parentElement) {
    // Intentionally empty.
  }
  return parentElement;
};

/**
 * Determines if element has a table ancestor.
 * @param  {!Element}  el   Element
 * @return {boolean}        Whether table ancestor of 'el' is found
 */
var isNestedInTable = function isNestedInTable(el) {
  return Boolean(findClosestAncestor(el, 'table'));
};

var elementUtilities = {
  findClosestAncestor: findClosestAncestor,
  isNestedInTable: isNestedInTable
};

var SECTION_TOGGLED_EVENT_TYPE = 'section-toggled';

/**
 * Find an array of table header (TH) contents. If there are no TH elements in
 * the table or the header's link matches pageTitle, an empty array is returned.
 * @param {!Element} element
 * @param {?string} pageTitle Unencoded page title; if this title matches the
 *                            contents of the header exactly, it will be omitted.
 * @return {!Array<string>}
 */
var getTableHeader = function getTableHeader(element, pageTitle) {
  var thArray = [];

  if (!element.children) {
    return thArray;
  }

  for (var i = 0; i < element.children.length; i++) {
    var el = element.children[i];

    if (el.tagName === 'TH') {
      // ok, we have a TH element!
      // However, if it contains more than two links, then ignore it, because
      // it will probably appear weird when rendered as plain text.
      var aNodes = el.querySelectorAll('a');
      // todo: these conditionals are very confusing. Rewrite by extracting a
      //       method or simplify.
      if (aNodes.length < 3) {
        // todo: remove nonstandard Element.innerText usage
        // Also ignore it if it's identical to the page title.
        if ((el.innerText && el.innerText.length || el.textContent.length) > 0 && el.innerText !== pageTitle && el.textContent !== pageTitle && el.innerHTML !== pageTitle) {
          thArray.push(el.innerText || el.textContent);
        }
      }
    }

    // if it's a table within a table, don't worry about it
    if (el.tagName === 'TABLE') {
      continue;
    }

    // todo: why do we need to recurse?
    // recurse into children of this element
    var ret = getTableHeader(el, pageTitle);

    // did we get a list of TH from this child?
    if (ret.length > 0) {
      thArray = thArray.concat(ret);
    }
  }

  return thArray;
};

/**
 * @typedef {function} FooterDivClickCallback
 * @param {!HTMLElement}
 * @return {void}
 */

/**
 * Ex: toggleCollapseClickCallback.bind(el, (container) => {
 *       window.scrollTo(0, container.offsetTop - transformer.getDecorOffset())
 *     })
 * @this HTMLElement
 * @param {?FooterDivClickCallback} footerDivClickCallback
 * @return {boolean} true if collapsed, false if expanded.
 */
var toggleCollapseClickCallback = function toggleCollapseClickCallback(footerDivClickCallback) {
  var container = this.parentNode;
  var header = container.children[0];
  var table = container.children[1];
  var footer = container.children[2];
  var caption = header.querySelector('.app_table_collapsed_caption');
  var collapsed = table.style.display !== 'none';
  if (collapsed) {
    table.style.display = 'none';
    header.classList.remove('app_table_collapse_close'); // todo: use app_table_collapsed_collapsed
    header.classList.remove('app_table_collapse_icon'); // todo: use app_table_collapsed_icon
    header.classList.add('app_table_collapsed_open'); // todo: use app_table_collapsed_expanded
    if (caption) {
      caption.style.visibility = 'visible';
    }
    footer.style.display = 'none';
    // if they clicked the bottom div, then scroll back up to the top of the table.
    if (this === footer && footerDivClickCallback) {
      footerDivClickCallback(container);
    }
  } else {
    table.style.display = 'block';
    header.classList.remove('app_table_collapsed_open'); // todo: use app_table_collapsed_expanded
    header.classList.add('app_table_collapse_close'); // todo: use app_table_collapsed_collapsed
    header.classList.add('app_table_collapse_icon'); // todo: use app_table_collapsed_icon
    if (caption) {
      caption.style.visibility = 'hidden';
    }
    footer.style.display = 'block';
  }
  return collapsed;
};

/**
 * @param {!HTMLElement} table
 * @return {!boolean} true if table should be collapsed, false otherwise.
 */
var shouldTableBeCollapsed = function shouldTableBeCollapsed(table) {
  var classBlacklist = ['navbox', 'vertical-navbox', 'navbox-inner', 'metadata', 'mbox-small'];
  var blacklistIntersects = classBlacklist.some(function (clazz) {
    return table.classList.contains(clazz);
  });
  return table.style.display !== 'none' && !blacklistIntersects;
};

/**
 * @param {!Element} element
 * @return {!boolean} true if element is an infobox, false otherwise.
 */
var isInfobox = function isInfobox(element) {
  return element.classList.contains('infobox');
};

/**
 * @param {!Document} document
 * @param {?string} content HTML string.
 * @return {!HTMLDivElement}
 */
var newCollapsedHeaderDiv = function newCollapsedHeaderDiv(document, content) {
  var div = document.createElement('div');
  div.classList.add('app_table_collapsed_container');
  div.classList.add('app_table_collapsed_open');
  div.innerHTML = content || '';
  return div;
};

/**
 * @param {!Document} document
 * @param {?string} content HTML string.
 * @return {!HTMLDivElement}
 */
var newCollapsedFooterDiv = function newCollapsedFooterDiv(document, content) {
  var div = document.createElement('div');
  div.classList.add('app_table_collapsed_bottom');
  div.classList.add('app_table_collapse_icon'); // todo: use collapsed everywhere
  div.innerHTML = content || '';
  return div;
};

/**
 * @param {!string} title
 * @param {!string[]} headerText
 * @return {!string} HTML string.
 */
var newCaption = function newCaption(title, headerText) {
  var caption = '<strong>' + title + '</strong>';

  caption += '<span class=app_span_collapse_text>';
  if (headerText.length > 0) {
    caption += ': ' + headerText[0];
  }
  if (headerText.length > 1) {
    caption += ', ' + headerText[1];
  }
  if (headerText.length > 0) {
    caption += ' …';
  }
  caption += '</span>';

  return caption;
};

/**
 * @param {!Window} window
 * @param {!Element} content
 * @param {?string} pageTitle
 * @param {?boolean} isMainPage
 * @param {?string} infoboxTitle
 * @param {?string} otherTitle
 * @param {?string} footerTitle
 * @param {?FooterDivClickCallback} footerDivClickCallback
 * @return {void}
 */
var collapseTables = function collapseTables(window, content, pageTitle, isMainPage, infoboxTitle, otherTitle, footerTitle, footerDivClickCallback) {
  if (isMainPage) {
    return;
  }

  var tables = content.querySelectorAll('table');

  var _loop = function _loop(i) {
    var table = tables[i];

    if (elementUtilities.findClosestAncestor(table, '.app_table_container') || !shouldTableBeCollapsed(table)) {
      return 'continue';
    }

    // todo: this is actually an array
    var headerText = getTableHeader(table, pageTitle);
    if (!headerText.length && !isInfobox(table)) {
      return 'continue';
    }
    var caption = newCaption(isInfobox(table) ? infoboxTitle : otherTitle, headerText);

    // create the container div that will contain both the original table
    // and the collapsed version.
    var containerDiv = window.document.createElement('div');
    containerDiv.className = 'app_table_container';
    table.parentNode.insertBefore(containerDiv, table);
    table.parentNode.removeChild(table);

    // remove top and bottom margin from the table, so that it's flush with
    // our expand/collapse buttons
    table.style.marginTop = '0px';
    table.style.marginBottom = '0px';

    var collapsedHeaderDiv = newCollapsedHeaderDiv(window.document, caption);
    collapsedHeaderDiv.style.display = 'block';

    var collapsedFooterDiv = newCollapsedFooterDiv(window.document, footerTitle);
    collapsedFooterDiv.style.display = 'none';

    // add our stuff to the container
    containerDiv.appendChild(collapsedHeaderDiv);
    containerDiv.appendChild(table);
    containerDiv.appendChild(collapsedFooterDiv);

    // set initial visibility
    table.style.display = 'none';

    // eslint-disable-next-line require-jsdoc, no-loop-func
    var dispatchSectionToggledEvent = function dispatchSectionToggledEvent(collapsed) {
      return (
        // eslint-disable-next-line no-undef
        window.dispatchEvent(new Polyfill.CustomEvent(SECTION_TOGGLED_EVENT_TYPE, { collapsed: collapsed }))
      );
    };

    // assign click handler to the collapsed divs
    collapsedHeaderDiv.onclick = function () {
      var collapsed = toggleCollapseClickCallback.bind(collapsedHeaderDiv)();
      dispatchSectionToggledEvent(collapsed);
    };
    collapsedFooterDiv.onclick = function () {
      var collapsed = toggleCollapseClickCallback.bind(collapsedFooterDiv, footerDivClickCallback)();
      dispatchSectionToggledEvent(collapsed);
    };
  };

  for (var i = 0; i < tables.length; ++i) {
    var _ret = _loop(i);

    if (_ret === 'continue') continue;
  }
};

/**
 * If you tap a reference targeting an anchor within a collapsed table, this
 * method will expand the references section. The client can then scroll to the
 * references section.
 *
 * The first reference (an "[A]") in the "enwiki > Airplane" article from ~June
 * 2016 exhibits this issue. (You can copy wikitext from this revision into a
 * test wiki page for testing.)
 * @param  {?Element} element
 * @return {void}
*/
var expandCollapsedTableIfItContainsElement = function expandCollapsedTableIfItContainsElement(element) {
  if (element) {
    var containerSelector = '[class*="app_table_container"]';
    var container = elementUtilities.findClosestAncestor(element, containerSelector);
    if (container) {
      var collapsedDiv = container.firstElementChild;
      if (collapsedDiv && collapsedDiv.classList.contains('app_table_collapsed_open')) {
        collapsedDiv.click();
      }
    }
  }
};

var CollapseTable = {
  SECTION_TOGGLED_EVENT_TYPE: SECTION_TOGGLED_EVENT_TYPE,
  toggleCollapseClickCallback: toggleCollapseClickCallback,
  collapseTables: collapseTables,
  expandCollapsedTableIfItContainsElement: expandCollapsedTableIfItContainsElement,
  test: {
    getTableHeader: getTableHeader,
    shouldTableBeCollapsed: shouldTableBeCollapsed,
    isInfobox: isInfobox,
    newCollapsedHeaderDiv: newCollapsedHeaderDiv,
    newCollapsedFooterDiv: newCollapsedFooterDiv,
    newCaption: newCaption
  }
};

/**
 * Configures span to be suitable replacement for red link anchor.
 * @param {!HTMLSpanElement} span The span element to configure as anchor replacement.
 * @param {!HTMLAnchorElement} anchor The anchor element being replaced.
 * @return {void}
 */
var configureRedLinkTemplate = function configureRedLinkTemplate(span, anchor) {
  span.innerHTML = anchor.innerHTML;
  span.setAttribute('class', anchor.getAttribute('class'));
};

/**
 * Finds red links in a document or document fragment.
 * @param {!(Document|DocumentFragment)} content Document or fragment in which to seek red links.
 * @return {!HTMLAnchorElement[]} Array of zero or more red link anchors.
 */
var redLinkAnchorsInContent = function redLinkAnchorsInContent(content) {
  return Array.prototype.slice.call(content.querySelectorAll('a.new'));
};

/**
 * Makes span to be used as cloning template for red link anchor replacements.
 * @param  {!Document} document Document to use to create span element. Reminder: this can't be a
 * document fragment because fragments don't implement 'createElement'.
 * @return {!HTMLSpanElement} Span element suitable for use as template for red link anchor
 * replacements.
 */
var newRedLinkTemplate = function newRedLinkTemplate(document) {
  return document.createElement('span');
};

/**
 * Replaces anchor with span.
 * @param  {!HTMLAnchorElement} anchor Anchor element.
 * @param  {!HTMLSpanElement} span Span element.
 * @return {void}
 */
var replaceAnchorWithSpan = function replaceAnchorWithSpan(anchor, span) {
  return anchor.parentNode.replaceChild(span, anchor);
};

/**
 * Hides red link anchors in either a document or a document fragment so they are unclickable and
 * unfocusable.
 * @param {!Document} document Document in which to hide red links.
 * @param {?DocumentFragment} fragment If specified, red links are hidden in the fragment and the
 * document is used only for span cloning.
 * @return {void}
 */
var hideRedLinks = function hideRedLinks(document, fragment) {
  var spanTemplate = newRedLinkTemplate(document);
  var content = fragment !== undefined ? fragment : document;
  redLinkAnchorsInContent(content).forEach(function (redLink) {
    var span = spanTemplate.cloneNode(false);
    configureRedLinkTemplate(span, redLink);
    replaceAnchorWithSpan(redLink, span);
  });
};

var RedLinks = {
  hideRedLinks: hideRedLinks,
  test: {
    configureRedLinkTemplate: configureRedLinkTemplate,
    redLinkAnchorsInContent: redLinkAnchorsInContent,
    newRedLinkTemplate: newRedLinkTemplate,
    replaceAnchorWithSpan: replaceAnchorWithSpan
  }
};

/**
 * To widen an image element a css class called 'wideImageOverride' is applied to the image element,
 * however, ancestors of the image element can prevent the widening from taking effect. This method
 * makes minimal adjustments to ancestors of the image element being widened so the image widening
 * can take effect.
 * @param  {!HTMLElement} el Element whose ancestors will be widened
 * @return {void}
 */
var widenAncestors = function widenAncestors(el) {
  for (var parentElement = el.parentElement; parentElement && !parentElement.classList.contains('content_block'); parentElement = parentElement.parentElement) {
    if (parentElement.style.width) {
      parentElement.style.width = '100%';
    }
    if (parentElement.style.maxWidth) {
      parentElement.style.maxWidth = '100%';
    }
    if (parentElement.style.float) {
      parentElement.style.float = 'none';
    }
  }
};

/**
 * Some images should not be widened. This method makes that determination.
 * @param  {!HTMLElement} image   The image in question
 * @return {boolean}              Whether 'image' should be widened
 */
var shouldWidenImage = function shouldWidenImage(image) {
  // Images within a "<div class='noresize'>...</div>" should not be widened.
  // Example exhibiting links overlaying such an image:
  //   'enwiki > Counties of England > Scope and structure > Local government'
  if (elementUtilities.findClosestAncestor(image, "[class*='noresize']")) {
    return false;
  }

  // Side-by-side images should not be widened. Often their captions mention 'left' and 'right', so
  // we don't want to widen these as doing so would stack them vertically.
  // Examples exhibiting side-by-side images:
  //    'enwiki > Cold Comfort (Inside No. 9) > Casting'
  //    'enwiki > Vincent van Gogh > Letters'
  if (elementUtilities.findClosestAncestor(image, "div[class*='tsingle']")) {
    return false;
  }

  // Imagemaps, which expect images to be specific sizes, should not be widened.
  // Examples can be found on 'enwiki > Kingdom (biology)':
  //    - first non lead image is an image map
  //    - 'Three domains of life > Phylogenetic Tree of Life' image is an image map
  if (image.hasAttribute('usemap')) {
    return false;
  }

  // Images in tables should not be widened - doing so can horribly mess up table layout.
  if (elementUtilities.isNestedInTable(image)) {
    return false;
  }

  return true;
};

/**
 * Removes barriers to images widening taking effect.
 * @param  {!HTMLElement} image   The image in question
 * @return {void}
 */
var makeRoomForImageWidening = function makeRoomForImageWidening(image) {
  widenAncestors(image);

  // Remove width and height attributes so wideImageOverride width percentages can take effect.
  image.removeAttribute('width');
  image.removeAttribute('height');
};

/**
 * Widens the image.
 * @param  {!HTMLElement} image   The image in question
 * @return {void}
 */
var widenImage = function widenImage(image) {
  makeRoomForImageWidening(image);
  image.classList.add('wideImageOverride');
};

/**
 * Widens an image if the image is found to be fit for widening.
 * @param  {!HTMLElement} image   The image in question
 * @return {boolean}              Whether or not 'image' was widened
 */
var maybeWidenImage = function maybeWidenImage(image) {
  if (shouldWidenImage(image)) {
    widenImage(image);
    return true;
  }
  return false;
};

var WidenImage = {
  maybeWidenImage: maybeWidenImage,
  test: {
    shouldWidenImage: shouldWidenImage,
    widenAncestors: widenAncestors
  }
};

var pagelib$1 = {
  CollapseTable: CollapseTable,
  RedLinks: RedLinks,
  WidenImage: WidenImage,
  test: {
    ElementUtilities: elementUtilities, Polyfill: Polyfill
  }
};

// This file exists for CSS packaging only. It imports the override CSS
// JavaScript index file, which also exists only for packaging, as well as the
// real JavaScript, transform/index, it simply re-exports.

return pagelib$1;

})));


},{}]},{},[2,9,24,14,15,16,17,22,23,18,19,20,21,1,5,6,7,8,4,11,12,13]);
