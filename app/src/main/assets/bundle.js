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

function handleReference( targetId, backlink, linkText ) {
    var targetElem = document.getElementById( targetId );
    if ( targetElem === null ) {
        console.log( "reference target not found: " + targetId );
    } else if ( !backlink && targetId.slice(0, 4).toLowerCase() === "cite" ) { // treat "CITEREF"s the same as "cite_note"s
        try {
            var refTexts = targetElem.getElementsByClassName( "reference-text" );
            if ( refTexts.length > 0 ) {
                targetElem = refTexts[0];
            }
            bridge.sendMessage( 'referenceClicked', { "ref": targetElem.innerHTML, "linkText": linkText } );
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
                handleReference( targetId, util.ancestorContainsClass( sourceNode, "mw-cite-backlink" ), sourceNode.textContent );
            } else if (sourceNode.classList.contains( 'app_media' )) {
                bridge.sendMessage( 'mediaClicked', { "href": href } );
            } else if (sourceNode.classList.contains( 'image' )) {
                bridge.sendMessage( 'imageClicked', { "href": href } );
            } else {
                bridge.sendMessage( 'linkClicked', sourceNode.hasAttribute( "title" ) ?
                { "href": href, "title": sourceNode.getAttribute( "title" ) } : { "href": href } );
            }
            event.preventDefault();
        }
    }
};

module.exports = new ActionsHandler();

},{"./bridge":2,"./utilities":17}],2:[function(require,module,exports){
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
var bridge = require("./bridge");
var pagelib = require("wikimedia-page-library");

bridge.registerListener( 'setTheme', function( payload ) {
    var theme;
    switch (payload.theme) {
        case 1:
            theme = pagelib.ThemeTransform.THEME.DARK;
            window.isDarkMode = true;
            break;
        case 2:
            theme = pagelib.ThemeTransform.THEME.BLACK;
            window.isDarkMode = true;
            break;
        default:
            theme = pagelib.ThemeTransform.THEME.DEFAULT;
            window.isDarkMode = false;
            break;
    }
    pagelib.ThemeTransform.setTheme( document, theme );
    pagelib.DimImagesTransform.dim( window, window.isDarkMode && payload.dimImages );
} );

bridge.registerListener( 'toggleDimImages', function( payload ) {
    pagelib.DimImagesTransform.dim( window, payload.dimImages );
} );
},{"./bridge":2,"wikimedia-page-library":18}],4:[function(require,module,exports){
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

},{"./transformer":10}],6:[function(require,module,exports){
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

},{"./bridge":2,"./transformer":10}],7:[function(require,module,exports){
var bridge = require("./bridge");
var pagelib = require("wikimedia-page-library");

bridge.registerListener( "displayPreviewHTML", function( payload ) {
    var content = document.getElementById( "content" );
    document.head.getElementsByTagName("base")[0].setAttribute("href", payload.siteBaseUrl);
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.html;

    // todo: remove this when editing page preview uses the same bundle as reading.
    if ( content ) {
        pagelib.ThemeTransform.classifyElements( content );
    }
} );
},{"./bridge":2,"wikimedia-page-library":18}],8:[function(require,module,exports){
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

},{"./bridge":2}],9:[function(require,module,exports){
var bridge = require("./bridge");
var transformer = require("./transformer");
var pagelib = require("wikimedia-page-library");
var lazyLoadViewportDistanceMultiplier = 2; // Load images on the current screen up to one ahead.
var lazyLoadTransformer = new pagelib.LazyLoadTransformer(window, lazyLoadViewportDistanceMultiplier);

pagelib.PlatformTransform.classify( window );
pagelib.CompatibilityTransform.enableSupport( document );

bridge.registerListener( "clearContents", function() {
    clearContents();
});

bridge.registerListener( "setMargins", function( payload ) {
    document.getElementById( "content" ).style.marginTop = payload.marginTop + "px";
    document.getElementById( "content" ).style.marginLeft = payload.marginLeft + "px";
    document.getElementById( "content" ).style.marginRight = payload.marginRight + "px";
});

bridge.registerListener( "setPaddingTop", function( payload ) {
    document.body.style.paddingTop = payload.paddingTop + "px";
});

bridge.registerListener( "setPaddingBottom", function( payload ) {
    document.body.style.paddingBottom = payload.paddingBottom + "px";
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

function setWindowAttributes( payload ) {
    document.head.getElementsByTagName("base")[0].setAttribute("href", payload.siteBaseUrl);

    window.sequence = payload.sequence;
    window.apiLevel = payload.apiLevel;
    window.string_table_infobox = payload.string_table_infobox;
    window.string_table_other = payload.string_table_other;
    window.string_table_close = payload.string_table_close;
    window.string_expand_refs = payload.string_expand_refs;
    window.pageTitle = payload.title;
    window.isMainPage = payload.isMainPage;
    window.isFilePage = payload.isFilePage;
    window.fromRestBase = payload.fromRestBase;
    window.isBeta = payload.isBeta;
    window.siteLanguage = payload.siteLanguage;
    window.showImages = payload.showImages;
}

function setTitleElement( parentNode ) {
    // create an empty div to act as the title anchor
    var titleDiv = document.createElement( "div" );
    titleDiv.id = "heading_0";
    titleDiv.setAttribute( "data-id", 0 );
    titleDiv.className = "section_heading";
    parentNode.appendChild( titleDiv );
}

function setIssuesElement( parentNode ) {
    var issuesContainer = document.createElement( "div" );
    issuesContainer.setAttribute( "dir", window.directionality );
    issuesContainer.id = "issues_container";
    parentNode.appendChild( issuesContainer );
    return issuesContainer;
}

bridge.registerListener( "displayLeadSection", function( payload ) {
    var lazyDocument;

    // This might be a refresh! Clear out all contents!
    clearContents();
    setWindowAttributes(payload);
    window.offline = false;

    var contentElem = document.getElementById( "content" );
    setTitleElement(contentElem);

    var issuesContainer = setIssuesElement(contentElem);

    lazyDocument = document.implementation.createHTMLDocument( );
    var content = lazyDocument.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.section.text;
    content.id = "content_block_0";

    // append the content to the DOM now, so that we can obtain
    // dimension measurements for items.
    document.getElementById( "content" ).appendChild( content );

    applySectionTransforms(content, true);

    bridge.sendMessage( "pageInfo", {
      "issues" : collectIssues(),
      "disambiguations" : collectDisambig()
    });
    //if there were no page issues, then hide the container
    if (!issuesContainer.hasChildNodes()) {
        document.getElementById( "content" ).removeChild(issuesContainer);
    }
    transformer.transform( "hideTables", document );
    lazyLoadTransformer.loadPlaceholders();
});

function clearContents() {
    lazyLoadTransformer.deregister();
    document.getElementById( "content" ).innerHTML = "";
    window.scrollTo( 0, 0 );
}

function elementsForSection( section ) {
    var content, lazyDocument;
    var header = pagelib.EditTransform.newEditSectionHeader(document,
              section.id, section.toclevel + 1,section.line);
    header.id = section.anchor;
    header.setAttribute( "dir", window.directionality );
    header.setAttribute( 'data-id', section.id );
    lazyDocument = document.implementation.createHTMLDocument( );
    content = lazyDocument.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = section.text;
    content.id = "content_block_" + section.id;
    applySectionTransforms(content, false);
    return [ header, content ];
}

function applySectionTransforms( content, isLeadSection ) {
    if (!window.showImages) {
        transformer.transform( "hideImages", content );
    }

    if (!window.fromRestBase) {
        // Content service transformations
        if (isLeadSection) {
            transformer.transform( "moveFirstGoodParagraphUp" );
        }
        pagelib.RedLinks.hideRedLinks( document );
        transformer.transform( "anchorPopUpMediaTransforms", content );
    }

    pagelib.ThemeTransform.classifyElements( content );

    if (!isLeadSection) {
        transformer.transform( "hideRefs", content );
    }
    if (!window.isMainPage) {
        transformer.transform( "widenImages", content );

        if (!window.isFilePage) {
            lazyLoadTransformer.convertImagesToPlaceholders( content );
        }
    }
    if (isLeadSection) {
        transformer.transform("displayIssuesLink", content);
    }
}

function displayRemainingSections(json, sequence, scrollY, fragment) {
    var contentWrapper = document.getElementById( "content" );
    var scrolled = false;

    json.sections.forEach(function (section) {
        elementsForSection(section).forEach(function (element) {
            contentWrapper.appendChild(element);
            // do we have a y-offset to scroll to?
            if (scrollY > 0 && scrollY < element.offsetTop && !scrolled) {
                window.scrollTo( 0, scrollY );
                scrolled = true;
            }
        });
        // do we have a section to scroll to?
        if ( typeof fragment === "string" && fragment.length > 0 && section.anchor === fragment) {
            scrollToSection( fragment );
        }
    });

    // if we still haven't scrolled to our target offset (if we have one), then do it now.
    if (scrollY > 0 && !scrolled) {
        window.scrollTo( 0, scrollY );
    }
    transformer.transform( "hideTables", document );
    lazyLoadTransformer.loadPlaceholders();
    bridge.sendMessage( "pageLoadComplete", { "sequence": sequence });
}

var remainingRequest;

bridge.registerListener( "queueRemainingSections", function ( payload ) {
    if (remainingRequest) {
        remainingRequest.abort();
    }
    remainingRequest = new XMLHttpRequest();
    remainingRequest.open('GET', payload.url);
    remainingRequest.sequence = payload.sequence;
    remainingRequest.scrollY = payload.scrollY;
    remainingRequest.fragment = payload.fragment;
    if (window.apiLevel > 19 && window.responseType !== 'json') {
        remainingRequest.responseType = 'json';
    }
    remainingRequest.onreadystatechange = function() {
        if (this.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        if (this.sequence !== window.sequence) {
            return;
        }
        if (this.status !== 200) {
            bridge.sendMessage( "loadRemainingError", { "status": this.status, "sequence": this.sequence });
            return;
        }
        try {
            // On API <20, the XMLHttpRequest does not support responseType = json,
            // so we have to call JSON.parse() ourselves.
            var sectionsObj = window.apiLevel > 19 ? this.response : JSON.parse(this.response);
            if (sectionsObj.mobileview) {
                // If it's a mobileview response, the "sections" object will be one level deeper.
                sectionsObj = sectionsObj.mobileview;
            }
            displayRemainingSections(sectionsObj, this.sequence, this.scrollY, this.fragment);
        } catch (e) {
            // Catch any errors that might have come from deserializing or rendering the
            // remaining sections.
            // TODO: Boil this up to the Java layer more properly, even though this kind of error
            // really shouldn't happen.
            console.log(e);
            // In case of such an error, send a completion event to the Java layer, so that the
            // PageActivity can consider the page loaded, and enable the user to take additional
            // actions that might have been dependent on page completion (e.g. refreshing).
            bridge.sendMessage( "pageLoadComplete", { "sequence": this.sequence });
        }
    };
    remainingRequest.send();
});

// -- Begin custom processing of ZIM html data --

bridge.registerListener( "displayFromZim", function( payload ) {
    // This might be a refresh! Clear out all contents!
    clearContents();
    setWindowAttributes(payload);
    window.isOffline = true;
    window.mainPageHint = payload.mainPageHint;
    window.offlineContentProvider = payload.offlineContentProvider;

    var contentElem = document.getElementById( "content" );
    setTitleElement(contentElem);

    if (window.isMainPage) {
        // TODO: remove this when the actual Main Pages in ZIM files contain more descriptive content.
        var helperDiv = document.createElement( "div" );
        helperDiv.innerHTML = window.mainPageHint;
        helperDiv.style = "font-size: 85%; margin: 12px 0 20px 0; padding: 12px; line-height: 120%; background-color: rgba(0, 0, 0, 0.04); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: 2px;";
        contentElem.appendChild( helperDiv );
    }

    var issuesContainer = setIssuesElement(contentElem);

    var parser = new DOMParser();
    var zimDoc = parser.parseFromString(payload.zimhtml, "text/html");
    if (!zimDoc) {
        zimDoc = document.implementation.createHTMLDocument("");
        zimDoc.documentElement.innerHTML = payload.zimhtml;
    }

    var zimTextNode = zimDoc.getElementById( "mw-content-text" );
    zimTextNode.parentNode.removeChild( zimTextNode );

    var zimNodes = zimTextNode.childNodes;
    var sectionIndex = 0;
    var sectionsJson = [];
    var sectionJson;
    var i;

    var lazyDocument = document.implementation.createHTMLDocument( );
    var currentSectionNode = lazyDocument.createElement( "div" );
    currentSectionNode.setAttribute( "dir", window.directionality );
    currentSectionNode.id = "content_block_" + sectionIndex;
    contentElem.appendChild( currentSectionNode );

    for ( i = 0; i < zimNodes.length; i++ ) {
        if (zimNodes[i].tagName === undefined || zimNodes[i].tagName === 'H1') {
            continue;
        }

        if ( zimNodes[i].tagName.length === 2 && zimNodes[i].tagName.substring(0, 1) === 'H' ) {

            // perform transforms on the previous section
            performZimSectionTransforms( sectionIndex, currentSectionNode );

            sectionIndex++;

            sectionJson = {};
            sectionJson.id = sectionIndex;
            sectionJson.toclevel = zimNodes[i].tagName.substring(1, 2);
            sectionJson.line = zimNodes[i].innerHTML;
            sectionJson.anchor = "heading_" + sectionIndex;
            sectionsJson.push(sectionJson);

            lazyDocument = document.implementation.createHTMLDocument( );
            currentSectionNode = lazyDocument.createElement( "div" );
            currentSectionNode.setAttribute( "dir", window.directionality );
            currentSectionNode.id = "content_block_" + sectionIndex;
            contentElem.appendChild( currentSectionNode );

            // dress up the header node a bit
            zimNodes[i].setAttribute( "dir", window.directionality );
            zimNodes[i].id = sectionJson.anchor;
            zimNodes[i].className = "pagelib_edit_section_header";
            zimNodes[i].setAttribute( 'data-id', sectionIndex );
        }
        currentSectionNode.appendChild(zimNodes[i]);
    }

    // perform transforms on the last section
    performZimSectionTransforms( sectionIndex, currentSectionNode );
    if (currentSectionNode.childNodes && currentSectionNode.childNodes.length > 1) {
        // In the current version of ZIM files, the last div in the last section is the
        // manually-appended "issued from Wikipedia" disclaimer, which we need to remove.
        // (Unfortunately this div doesn't have any identifying classes or ids, so we can't
        // find it using a selector.)
        currentSectionNode.removeChild(currentSectionNode.childNodes[currentSectionNode.childNodes.length - 1]);
    }

    bridge.sendMessage( "pageInfo", {
      "issues" : collectIssues(),
      "disambiguations" : collectDisambig()
    });
    //if there were no page issues, then hide the container
    if (!issuesContainer.hasChildNodes()) {
        contentElem.removeChild(issuesContainer);
    }

    if (payload.scrollY > 0) {
        window.scrollTo( 0, payload.scrollY );
    }
    document.getElementById( "loading_sections").className = "";
    transformer.transform( "hideTables", document );
    lazyLoadTransformer.loadPlaceholders();
    bridge.sendMessage( "pageLoadComplete", {
      "sequence": payload.sequence,
      "savedPage": payload.savedPage,
      "sections": sectionsJson });
});

function performZimSectionTransforms( sectionIndex, currentSectionNode ) {
    applySectionTransforms(currentSectionNode, sectionIndex === 0);

    var i;
    var imgTags = currentSectionNode.querySelectorAll( 'img' );
    for ( i = 0; i < imgTags.length; i++ ) {
        var imgSrc = imgTags[i].getAttribute( 'src' );
        if (imgSrc !== null) {
            imgTags[i].setAttribute( 'src', imgSrc.replace("../I/", window.offlineContentProvider + "I/") );
        }
    }

    var placeholderTags = currentSectionNode.querySelectorAll( 'span.pagelib_lazy_load_placeholder' );
    for ( i = 0; i < placeholderTags.length; i++ ) {
        var dataSrc = placeholderTags[i].getAttribute( 'data-src' );
        if (dataSrc !== null) {
            placeholderTags[i].setAttribute( 'data-src', dataSrc.replace("../I/", window.offlineContentProvider + "I/") );
        }
    }
}

// -- End custom processing of ZIM html data --

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

bridge.registerListener( "scrollToBottom", function ( payload ) {
    window.scrollTo(0, document.body.scrollHeight - payload.offset - transformer.getDecorOffset());
});

/**
 * Returns the section id of the section that has the header closest to but above midpoint of screen,
 * or -1 if the page is scrolled all the way to the bottom (i.e. native bottom content should be shown).
 */
function getCurrentSection() {
    var sectionHeaders = document.getElementsByClassName( "pagelib_edit_section_header" );
    var bottomDiv = document.getElementById( "bottom_stopper" );
    var topCutoff = window.scrollY + ( document.documentElement.clientHeight / 2 );
    if (topCutoff > bottomDiv.offsetTop) {
        return -1;
    }
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

},{"./bridge":2,"./transformer":10,"wikimedia-page-library":18}],10:[function(require,module,exports){
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
},{}],11:[function(require,module,exports){
var pagelib = require("wikimedia-page-library");
var transformer = require("../transformer");

function scrollWithDecorOffset(container) {
    window.scrollTo( 0, container.parentNode.offsetTop - transformer.getDecorOffset() );
}

function toggleCollapseClickCallback() {
    pagelib.CollapseTable.toggleCollapseClickCallback.call(this, scrollWithDecorOffset);
}

transformer.register( "hideTables", function(document) {
    pagelib.CollapseTable.collapseTables(window, document, window.pageTitle,
        window.isMainPage, window.string_table_infobox,
        window.string_table_other, window.string_table_close,
        scrollWithDecorOffset);
});

module.exports = {
    handleTableCollapseOrExpandClick: toggleCollapseClickCallback
};

},{"../transformer":10,"wikimedia-page-library":18}],12:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "hideImages", function( content ) {
    var minImageSize = 64;
    var images = content.querySelectorAll( 'img:not(.mwe-math-fallback-image-inline)' );
    for (var i = 0; i < images.length; i++) {
        var img = images[i];
        if (img.width < minImageSize && img.height < minImageSize) {
            continue;
        }
        // Just replace the src of the image with a placeholder image from our assets.
        img.src = "file:///android_asset/image_placeholder.png";
        img.srcset = "";
    }
} );

},{"../transformer":10}],13:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "hideRefs", function( content ) {
    var refLists = content.querySelectorAll( "div.reflist" );
    for (var i = 0; i < refLists.length; i++) {
        // Wrap this div in a <table>, so that it will be caught by the pagelibrary for collapsing.
        var table = document.createElement( 'table' );
        var tr = document.createElement( 'tr' );
        var th = document.createElement( 'th' );
        var td = document.createElement( 'td' );
        th.style.display = "none";
        th.innerHTML = window.string_expand_refs;
        table.appendChild(th);
        table.appendChild(tr);
        tr.appendChild(td);

        refLists[i].parentNode.insertBefore(table, refLists[i]);
        refLists[i].parentNode.removeChild(refLists[i]);
        td.appendChild(refLists[i]);
    }
} );
},{"../transformer":10}],14:[function(require,module,exports){
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

},{"../../transformer":10}],15:[function(require,module,exports){
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

},{"../../transformer":10}],16:[function(require,module,exports){
var maybeWidenImage = require('wikimedia-page-library').WidenImage.maybeWidenImage;
var transformer = require("../transformer");

function isGalleryImage(image) {
  return (
      image.width >= 64 &&
      image.parentNode.className === "image"
    );
}

transformer.register( "widenImages", function( content ) {
    var images = content.querySelectorAll( 'img' );
    var image;
    for ( var i = 0; i < images.length; i++ ) {
        image = images[i];
        if (isGalleryImage(image)) {
            maybeWidenImage(image);
        }
    }
} );

},{"../transformer":10,"wikimedia-page-library":18}],17:[function(require,module,exports){
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

module.exports = {
    ancestorContainsClass: ancestorContainsClass,
    getDictionaryFromSrcset: getDictionaryFromSrcset,
    firstDivAncestor: firstDivAncestor
};

},{}],18:[function(require,module,exports){
!function(e,t){"object"==typeof exports&&"object"==typeof module?module.exports=t():"function"==typeof define&&define.amd?define([],t):"object"==typeof exports?exports.pagelib=t():e.pagelib=t()}(window,function(){return function(e){var t={};function n(i){if(t[i])return t[i].exports;var a=t[i]={i:i,l:!1,exports:{}};return e[i].call(a.exports,a,a.exports,n),a.l=!0,a.exports}return n.m=e,n.c=t,n.d=function(e,t,i){n.o(e,t)||Object.defineProperty(e,t,{configurable:!1,enumerable:!0,get:i})},n.r=function(e){Object.defineProperty(e,"__esModule",{value:!0})},n.n=function(e){var t=e&&e.__esModule?function(){return e.default}:function(){return e};return n.d(t,"a",t),t},n.o=function(e,t){return Object.prototype.hasOwnProperty.call(e,t)},n.p="",n(n.s=44)}([function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i="undefined"!=typeof window&&window.CustomEvent||function(e){var t=arguments.length>1&&void 0!==arguments[1]?arguments[1]:{bubbles:!1,cancelable:!1,detail:void 0},n=document.createEvent("CustomEvent");return n.initCustomEvent(e,t.bubbles,t.cancelable,t.detail),n};t.default={matchesSelector:function(e,t){return e.matches?e.matches(t):e.matchesSelector?e.matchesSelector(t):!!e.webkitMatchesSelector&&e.webkitMatchesSelector(t)},querySelectorAll:function(e,t){return Array.prototype.slice.call(e.querySelectorAll(t))},CustomEvent:i}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,a=n(0),r=(i=a)&&i.__esModule?i:{default:i};var o=function(e,t){var n=void 0;for(n=e.parentElement;n&&!r.default.matchesSelector(n,t);n=n.parentElement);return n};t.default={findClosestAncestor:o,isNestedInTable:function(e){return Boolean(o(e,"table"))},closestInlineStyle:function(e,t){for(var n=e;n;n=n.parentElement)if(n.style[t])return n},isVisible:function(e){return Boolean(e.offsetWidth||e.offsetHeight||e.getClientRects().length)},copyAttributesToDataAttributes:function(e,t,n){n.filter(function(t){return e.hasAttribute(t)}).forEach(function(n){return t.setAttribute("data-"+n,e.getAttribute(n))})},copyDataAttributesToAttributes:function(e,t,n){n.filter(function(t){return e.hasAttribute("data-"+t)}).forEach(function(n){return t.setAttribute(n,e.getAttribute("data-"+n))})}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}();var a=function(){function e(t,n,i){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this._window=t,this._period=n,this._function=i,this._context=void 0,this._arguments=void 0,this._result=void 0,this._timeout=0,this._timestamp=0}return i(e,null,[{key:"wrap",value:function(t,n,i){var a=new e(t,n,i),r=function(){return a.queue(this,arguments)};return r.result=function(){return a.result},r.pending=function(){return a.pending()},r.delay=function(){return a.delay()},r.cancel=function(){return a.cancel()},r.reset=function(){return a.reset()},r}}]),i(e,[{key:"queue",value:function(e,t){var n=this;return this._context=e,this._arguments=t,this.pending()||(this._timeout=this._window.setTimeout(function(){n._timeout=0,n._timestamp=Date.now(),n._result=n._function.apply(n._context,n._arguments)},this.delay())),this.result}},{key:"pending",value:function(){return Boolean(this._timeout)}},{key:"delay",value:function(){return this._timestamp?Math.max(0,this._period-(Date.now()-this._timestamp)):0}},{key:"cancel",value:function(){this._timeout&&this._window.clearTimeout(this._timeout),this._timeout=0}},{key:"reset",value:function(){this.cancel(),this._result=void 0,this._timestamp=0}},{key:"result",get:function(){return this._result}}]),e}();t.default=a},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(20);var i=o(n(7)),a=o(n(1)),r=o(n(0));function o(e){return e&&e.__esModule?e:{default:e}}var l=["class","style","src","srcset","width","height","alt","usemap","data-file-width","data-file-height","data-image-gallery"],s={px:50,ex:10,em:5};t.default={queryLazyLoadableImages:function(e){return r.default.querySelectorAll(e,"img").filter(function(e){return function(e){var t=i.default.from(e);return!t.width||!t.height||t.widthValue>=s[t.widthUnit]&&t.heightValue>=s[t.heightUnit]}(e)})},convertImagesToPlaceholders:function(e,t){return t.map(function(t){return function(e,t){var n=e.createElement("span");t.hasAttribute("class")&&n.setAttribute("class",t.getAttribute("class")),n.classList.add("pagelib_lazy_load_placeholder"),n.classList.add("pagelib_lazy_load_placeholder_pending");var r=i.default.from(t);r.width&&n.style.setProperty("width",""+r.width),a.default.copyAttributesToDataAttributes(t,n,l);var o=e.createElement("span");if(r.width&&r.height){var s=r.heightValue/r.widthValue;o.style.setProperty("padding-top",100*s+"%")}return n.appendChild(o),t.parentNode.replaceChild(n,t),n}(e,t)})},loadPlaceholder:function(e,t){t.classList.add("pagelib_lazy_load_placeholder_loading"),t.classList.remove("pagelib_lazy_load_placeholder_pending");var n=e.createElement("img"),i=function(e){n.setAttribute("src",n.getAttribute("src")),e.stopPropagation(),e.preventDefault()};return n.addEventListener("load",function(){t.removeEventListener("click",i),t.parentNode.replaceChild(n,t),n.classList.add("pagelib_lazy_load_image_loaded"),n.classList.remove("pagelib_lazy_load_image_loading")},{once:!0}),n.addEventListener("error",function(){t.classList.add("pagelib_lazy_load_placeholder_error"),t.classList.remove("pagelib_lazy_load_placeholder_loading"),t.addEventListener("click",i)},{once:!0}),a.default.copyDataAttributesToAttributes(t,n,l),n.classList.add("pagelib_lazy_load_image_loading"),n}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(23);var i=function(e,t,n){var i=new RegExp("\\s?["+t+"][^"+t+n+"]+["+n+"]","g"),a=0,r=e,o="";do{o=r,r=r.replace(i,""),a++}while(o!==r&&a<30);return r},a=function(e){var t=e;return t=i(t,"(",")"),t=i(t,"/","/")},r=function e(t,n,i,a){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this.title=t,this.thumbnail=n,this.description=i,this.extract=a},o=function(e,t,n,i,o){var l=[],s=o.getElementById(t);e.forEach(function(e,t){var i=e.title.replace(/ /g,"_");l.push(i);var u=function(e,t,n,i){var r=i.createElement("a");if(r.id=t,r.className="pagelib_footer_readmore_page",e.thumbnail&&e.thumbnail.source){var o=i.createElement("div");o.style.backgroundImage="url("+e.thumbnail.source+")",o.classList.add("pagelib_footer_readmore_page_image"),r.appendChild(o)}var l=i.createElement("div");if(l.classList.add("pagelib_footer_readmore_page_container"),r.appendChild(l),r.href="/wiki/"+encodeURI(e.title),e.title){var s=i.createElement("div");s.id=t,s.className="pagelib_footer_readmore_page_title";var u=e.title.replace(/_/g," ");s.innerHTML=u,r.title=u,l.appendChild(s)}var c=void 0;if(e.description&&(c=e.description),(!c||c.length<10)&&e.extract&&(c=a(e.extract)),c){var d=i.createElement("div");d.id=t,d.className="pagelib_footer_readmore_page_description",d.innerHTML=c,l.appendChild(d)}var f=i.createElement("div");return f.id="pagelib_footer_read_more_save_"+encodeURI(e.title),f.className="pagelib_footer_readmore_page_save",f.addEventListener("click",function(t){t.stopPropagation(),t.preventDefault(),n(e.title)}),l.appendChild(f),i.createDocumentFragment().appendChild(r)}(new r(i,e.thumbnail,e.description,e.extract),t,n,o);s.appendChild(u)}),i(l)},l=function(e,t,n){return(n||"")+"/w/api.php?"+(i=function(e,t){return{action:"query",format:"json",formatversion:2,prop:"extracts|pageimages|description",generator:"search",gsrlimit:t,gsrprop:"redirecttitle",gsrsearch:"morelike:"+e,gsrwhat:"text",exchars:256,exintro:"",exlimit:t,explaintext:"",pilicense:"any",pilimit:t,piprop:"thumbnail",pithumbsize:120}}(e,t),Object.keys(i).map(function(e){return encodeURIComponent(e)+"="+encodeURIComponent(i[e])}).join("&"));var i},s=function(e){console.log("statusText = "+e)};t.default={add:function(e,t,n,i,a,r,u){!function(e,t,n,i,a,r,o,u){var c=new XMLHttpRequest;c.open("GET",l(e,t,i),!0),c.onload=function(){c.readyState===XMLHttpRequest.DONE&&(200===c.status?a(JSON.parse(c.responseText).query.pages,n,r,o,u):s(c.statusText))},c.onerror=function(){return s(c.statusText)};try{c.send()}catch(e){s(e.toString())}}(e,t,n,i,o,a,r,u)},setHeading:function(e,t,n){var i=n.getElementById(t);i.innerText=e,i.title=e},updateSaveButtonForTitle:function(e,t,n,i){var a=i.getElementById("pagelib_footer_read_more_save_"+encodeURI(e));a&&(a.innerText=t,a.title=t,function(e,t){var n="pagelib_footer_readmore_bookmark_unfilled",i="pagelib_footer_readmore_bookmark_filled";e.classList.remove(i,n),e.classList.add(t?i:n)}(a,n))},test:{cleanExtract:a,safelyRemoveEnclosures:i}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(28);t.default={add:function(e,t,n,i,a,r,o){var l=e.querySelector("#"+i),s=t.split("$1");l.innerHTML="<div class='pagelib_footer_legal_contents'>\n    <hr class='pagelib_footer_legal_divider'>\n    <span class='pagelib_footer_legal_license'>\n      "+s[0]+"\n      <a class='pagelib_footer_legal_license_link'>\n        "+n+"\n      </a>\n      "+s[1]+"\n      <br>\n      <div class=\"pagelib_footer_browser\">\n        <a class='pagelib_footer_browser_link'>\n          "+r+"\n        </a>\n      </div>\n    </span>\n  </div>",l.querySelector(".pagelib_footer_legal_license_link").addEventListener("click",function(){a()}),l.querySelector(".pagelib_footer_browser_link").addEventListener("click",function(){o()})}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(30);var i,a=n(0),r=(i=a)&&i.__esModule?i:{default:i};t.default={containerFragment:function(e){var t=e.createElement("div"),n=e.createDocumentFragment();return n.appendChild(t),t.innerHTML="<div id='pagelib_footer_container' class='pagelib_footer_container'>\n    <div id='pagelib_footer_container_section_0'>\n      <div id='pagelib_footer_container_menu'>\n        <div id='pagelib_footer_container_menu_heading' class='pagelib_footer_container_heading'>\n        </div>\n        <div id='pagelib_footer_container_menu_items'>\n        </div>\n      </div>\n    </div>\n    <div id='pagelib_footer_container_ensure_can_scroll_to_top'>\n      <div id='pagelib_footer_container_section_1'>\n        <div id='pagelib_footer_container_readmore'>\n          <div\n            id='pagelib_footer_container_readmore_heading' class='pagelib_footer_container_heading'>\n          </div>\n          <div id='pagelib_footer_container_readmore_pages'>\n          </div>\n        </div>\n      </div>\n      <div id='pagelib_footer_container_legal'></div>\n    </div>\n  </div>",n},isContainerAttached:function(e){return Boolean(e.querySelector("#pagelib_footer_container"))},updateBottomPaddingToAllowReadMoreToScrollToTop:function(e){var t=e.document.getElementById("pagelib_footer_container_ensure_can_scroll_to_top"),n=parseInt(t.style.paddingBottom,10)||0,i=t.clientHeight-n,a=Math.max(0,e.innerHeight-i);t.style.paddingBottom=a+"px"},updateLeftAndRightMargin:function(e,t){r.default.querySelectorAll(t,["#pagelib_footer_container_menu_heading","#pagelib_footer_container_readmore","#pagelib_footer_container_legal"].join()).forEach(function(t){t.style.marginLeft=e+"px",t.style.marginRight=e+"px"});var n="rtl"===t.querySelector("html").dir?"right":"left";r.default.querySelectorAll(t,".pagelib_footer_menu_item").forEach(function(t){t.style.backgroundPosition=n+" "+e+"px center",t.style.paddingLeft=e+"px",t.style.paddingRight=e+"px"})}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}();function a(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}var r=function(){function e(t,n){a(this,e),this._value=Number(t),this._unit=n||"px"}return i(e,null,[{key:"fromElement",value:function(t,n){return t.style.getPropertyValue(n)&&e.fromStyle(t.style.getPropertyValue(n))||t.hasAttribute(n)&&new e(t.getAttribute(n))||void 0}},{key:"fromStyle",value:function(t){var n=t.match(/(-?\d*\.?\d*)(\D+)?/)||[];return new e(n[1],n[2])}}]),i(e,[{key:"toString",value:function(){return isNaN(this.value)?"":""+this.value+this.unit}},{key:"value",get:function(){return this._value}},{key:"unit",get:function(){return this._unit}}]),e}(),o=function(){function e(t,n){a(this,e),this._width=t,this._height=n}return i(e,null,[{key:"from",value:function(t){return new e(r.fromElement(t,"width"),r.fromElement(t,"height"))}}]),i(e,[{key:"width",get:function(){return this._width}},{key:"widthValue",get:function(){return this._width&&!isNaN(this._width.value)?this._width.value:NaN}},{key:"widthUnit",get:function(){return this._width&&this._width.unit||"px"}},{key:"height",get:function(){return this._height}},{key:"heightValue",get:function(){return this._height&&!isNaN(this._height.value)?this._height.value:NaN}},{key:"heightUnit",get:function(){return this._height&&this._height.unit||"px"}}]),e}();t.default=o},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,a=n(0),r=(i=a)&&i.__esModule?i:{default:i};var o=function(e,t){if(!t)return[];var n=r.default.querySelectorAll(t,"table.ambox:not(.ambox-multiple_issues):not(.ambox-notice)"),i=e.createDocumentFragment();return n.forEach(function(e){return i.appendChild(e.cloneNode(!0))}),r.default.querySelectorAll(i,".hide-when-compact, .collapsed").forEach(function(e){return e.remove()}),r.default.querySelectorAll(i,"td[class*=mbox-text] > *[class*=mbox-text]")};t.default={collectDisambiguationTitles:function(e){return e?r.default.querySelectorAll(e,'div.hatnote a[href]:not([href=""]):not([redlink="1"])').map(function(e){return e.href}):[]},collectDisambiguationHTML:function(e){return e?r.default.querySelectorAll(e,"div.hatnote").map(function(e){return e.innerHTML}):[]},collectPageIssuesHTML:function(e,t){return o(e,t).map(function(e){return e.innerHTML})},collectPageIssuesText:function(e,t){return o(e,t).map(function(e){return e.textContent.trim()})},test:{collectPageIssues:o}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(40);var i=r(n(0)),a=r(n(1));function r(e){return e&&e.__esModule?e:{default:e}}var o=function(e){return e.childNodes&&i.default.querySelectorAll(e,"a").length<3},l=function(e){return e&&e.replace(/[\s0-9]/g,"").length>0},s=function(e){var t=e.match(/\w+/);if(t)return t[0]},u=function(e,t){var n=s(t),i=s(e.textContent);return!(!n||!i)&&n.toLowerCase()===i.toLowerCase()},c=function(e){return 1===e.nodeType||3===e.nodeType},d=function(e){return e.trim().replace(/\s/g," ")},f=function(e){return 1===e.nodeType&&"BR"===e.tagName},_=function(e,t){return t.parentNode.replaceChild(e.createTextNode(" "),t)},p=function(e,t,n){if(!o(t))return null;var a=e.createDocumentFragment();a.appendChild(t.cloneNode(!0));var r=a.querySelector("th");i.default.querySelectorAll(r,".geo, .coordinates, sup.reference, ol, ul").forEach(function(e){return e.remove()});var s=Array.prototype.slice.call(r.childNodes);n&&s.filter(c).filter(function(e){return u(e,n)}).forEach(function(e){return e.remove()}),s.filter(f).forEach(function(t){return _(e,t)});var p=r.textContent;return l(p)?d(p):null},h=function(e,t){var n=e.hasAttribute("scope"),i=t.hasAttribute("scope");return n&&i?0:n?-1:i?1:0},g=function(e,t,n){var a=[],r=i.default.querySelectorAll(t,"th");r.sort(h);for(var o=0;o<r.length;++o){var l=p(e,r[o],n);if(l&&-1===a.indexOf(l)&&(a.push(l),2===a.length))break}return a},m=function(e,t,n){var i=e.children[0],a=e.children[1],r=e.children[2],o=i.querySelector(".app_table_collapsed_caption"),l="none"!==a.style.display;return l?(a.style.display="none",i.classList.remove("pagelib_collapse_table_collapsed"),i.classList.remove("pagelib_collapse_table_icon"),i.classList.add("pagelib_collapse_table_expanded"),o&&(o.style.visibility="visible"),r.style.display="none",t===r&&n&&n(e)):(a.style.display="block",i.classList.remove("pagelib_collapse_table_expanded"),i.classList.add("pagelib_collapse_table_collapsed"),i.classList.add("pagelib_collapse_table_icon"),o&&(o.style.visibility="hidden"),r.style.display="block"),l},v=function(e){var t=this.parentNode;return m(t,this,e)},b=function(e){var t=["navbox","vertical-navbox","navbox-inner","metadata","mbox-small"].some(function(t){return e.classList.contains(t)});return"none"!==e.style.display&&!t},y=function(e){return e.classList.contains("infobox")},E=function(e,t){var n=e.createElement("div");return n.classList.add("pagelib_collapse_table_collapsed_container"),n.classList.add("pagelib_collapse_table_expanded"),n.appendChild(t),n},T=function(e,t){var n=e.createElement("div");return n.classList.add("pagelib_collapse_table_collapsed_bottom"),n.classList.add("pagelib_collapse_table_icon"),n.innerHTML=t||"",n},L=function(e,t,n){var i=e.createDocumentFragment(),a=e.createElement("strong");a.innerHTML=t,i.appendChild(a);var r=e.createElement("span");return r.classList.add("pagelib_collapse_table_collapse_text"),n.length>0&&r.appendChild(e.createTextNode(": "+n[0])),n.length>1&&r.appendChild(e.createTextNode(", "+n[1])),n.length>0&&r.appendChild(e.createTextNode(" …")),i.appendChild(r),i},w=function(e,t,n,r,o,l,s,u,c){if(!r)for(var d=t.querySelectorAll("table"),f=function(r){var f=d[r];if(a.default.findClosestAncestor(f,".pagelib_collapse_table_container")||!b(f))return"continue";var _=g(t,f,n);if(!_.length&&!y(f))return"continue";var p=L(t,y(f)?l:s,_),h=t.createElement("div");h.className="pagelib_collapse_table_container",f.parentNode.insertBefore(h,f),f.parentNode.removeChild(f),f.style.marginTop="0px",f.style.marginBottom="0px";var w=E(t,p);w.style.display="block";var C=T(t,u);C.style.display="none",h.appendChild(w),h.appendChild(f),h.appendChild(C),f.style.display="none";var x=function(t){return e.dispatchEvent(new i.default.CustomEvent("section-toggled",{collapsed:t}))};w.onclick=function(){var e=v.bind(w)();x(e)},C.onclick=function(){var e=v.bind(C,c)();x(e)},o||m(h)},_=0;_<d.length;++_)f(_)};t.default={SECTION_TOGGLED_EVENT_TYPE:"section-toggled",toggleCollapseClickCallback:v,collapseTables:function(e,t,n,i,a,r,o,l){w(e,t,n,i,!0,a,r,o,l)},adjustTables:w,expandCollapsedTableIfItContainsElement:function(e){if(e){var t=a.default.findClosestAncestor(e,'[class*="pagelib_collapse_table_container"]');if(t){var n=t.firstElementChild;n&&n.classList.contains("pagelib_collapse_table_expanded")&&n.click()}}},test:{elementScopeComparator:h,extractEligibleHeaderText:p,firstWordFromString:s,getTableHeaderTextArray:g,shouldTableBeCollapsed:b,isHeaderEligible:o,isHeaderTextEligible:l,isInfobox:y,newCollapsedHeaderDiv:E,newCollapsedFooterDiv:T,newCaptionFragment:L,isNodeTextContentSimilarToPageTitle:u,stringWithNormalizedWhitespace:d,replaceNodeWithBreakingSpaceTextNode:_}}},,,,,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(14);var i,a=n(1),r=(i=a)&&i.__esModule?i:{default:i};var o=function(e){for(var t=[],n=e;n.parentElement&&!(n=n.parentElement).classList.contains("content_block");)t.push(n);return t},l=function(e,t,n){e[t]=n},s=function(e,t,n){Boolean(e[t])&&l(e,t,n)},u={width:"100%",height:"auto",maxWidth:"100%",float:"none"},c=function(e){Object.keys(u).forEach(function(t){return s(e.style,t,u[t])})},d=function(e){Object.keys(u).forEach(function(t){return l(e.style,t,u[t])})},f=function(e){o(e).forEach(c);var t=r.default.findClosestAncestor(e,"a.image");t&&d(t)},_=function(e){return!r.default.findClosestAncestor(e,"[class*='noresize']")&&(!r.default.findClosestAncestor(e,"div[class*='tsingle']")&&(!e.hasAttribute("usemap")&&!r.default.isNestedInTable(e)))};t.default={maybeWidenImage:function(e){return!!_(e)&&(function(e){f(e),e.classList.add("pagelib_widen_image_override")}(e),!0)},test:{ancestorsToWiden:o,shouldWidenImage:_,updateExistingStyleValue:s,widenAncestors:f,widenElementByUpdatingExistingStyles:c,widenElementByUpdatingStyles:d}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,a=n(0),r=(i=a)&&i.__esModule?i:{default:i};var o=function(e,t){e.innerHTML=t.innerHTML,e.setAttribute("class",t.getAttribute("class"))},l=function(e){return r.default.querySelectorAll(e,"a.new")},s=function(e){return e.createElement("span")},u=function(e,t){return e.parentNode.replaceChild(t,e)};t.default={hideRedLinks:function(e){var t=s(e);l(e).forEach(function(e){var n=t.cloneNode(!1);o(n,e),u(e,n)})},test:{configureRedLinkTemplate:o,redLinkAnchorsInDocument:l,newRedLinkTemplate:s,replaceAnchorWithSpan:u}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i={ANDROID:"pagelib_platform_android",IOS:"pagelib_platform_ios"};t.default={CLASS:i,classify:function(e){var t=e.document.querySelector("html");(function(e){return/android/i.test(e.navigator.userAgent)})(e)&&t.classList.add(i.ANDROID),function(e){return/ipad|iphone|ipod/i.test(e.navigator.userAgent)}(e)&&t.classList.add(i.IOS)}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}(),a=s(n(9)),r=s(n(1)),o=s(n(3)),l=s(n(2));function s(e){return e&&e.__esModule?e:{default:e}}var u=["scroll","resize",a.default.SECTION_TOGGLED_EVENT_TYPE],c=100,d=function(){function e(t,n){var i=this;!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this._window=t,this._loadDistanceMultiplier=n,this._placeholders=[],this._registered=!1,this._throttledLoadPlaceholders=l.default.wrap(t,c,function(){return i._loadPlaceholders()})}return i(e,[{key:"convertImagesToPlaceholders",value:function(e){var t=o.default.queryLazyLoadableImages(e),n=o.default.convertImagesToPlaceholders(this._window.document,t);this._placeholders=this._placeholders.concat(n),this._register()}},{key:"loadPlaceholders",value:function(){this._throttledLoadPlaceholders()}},{key:"deregister",value:function(){var e=this;this._registered&&(u.forEach(function(t){return e._window.removeEventListener(t,e._throttledLoadPlaceholders)}),this._throttledLoadPlaceholders.reset(),this._placeholders=[],this._registered=!1)}},{key:"_register",value:function(){var e=this;!this._registered&&this._placeholders.length&&(this._registered=!0,u.forEach(function(t){return e._window.addEventListener(t,e._throttledLoadPlaceholders)}))}},{key:"_loadPlaceholders",value:function(){var e=this;this._placeholders=this._placeholders.filter(function(t){var n=!0;return e._isPlaceholderEligibleToLoad(t)&&(o.default.loadPlaceholder(e._window.document,t),n=!1),n}),0===this._placeholders.length&&this.deregister()}},{key:"_isPlaceholderEligibleToLoad",value:function(e){return r.default.isVisible(e)&&this._isPlaceholderWithinLoadDistance(e)}},{key:"_isPlaceholderWithinLoadDistance",value:function(e){var t=e.getBoundingClientRect(),n=this._window.innerHeight*this._loadDistanceMultiplier;return!(t.top>n||t.bottom<-n)}}]),e}();t.default=d},,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}(),a=s(n(6)),r=s(n(5)),o=s(n(4)),l=s(n(2));function s(e){return e&&e.__esModule?e:{default:e}}var u=function(){function e(){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this._resizeListener=void 0}return i(e,[{key:"add",value:function(e,t,n,i,s,u,c,d,f,_,p,h,g){this.remove(e),t.appendChild(a.default.containerFragment(e.document)),r.default.add(e.document,c,d,"pagelib_footer_container_legal",f,_,p),o.default.setHeading(s,"pagelib_footer_container_readmore_heading",e.document),o.default.add(i,u,"pagelib_footer_container_readmore_pages",n,g,function(t){a.default.updateBottomPaddingToAllowReadMoreToScrollToTop(e),h(t)},e.document),this._resizeListener=l.default.wrap(e,100,function(){return a.default.updateBottomPaddingToAllowReadMoreToScrollToTop(e)}),e.addEventListener("resize",this._resizeListener)}},{key:"remove",value:function(e){this._resizeListener&&(e.removeEventListener("resize",this._resizeListener),this._resizeListener.cancel(),this._resizeListener=void 0);var t=e.document.getElementById("pagelib_footer_container");t&&t.parentNode.removeChild(t)}}]),e}();t.default=u},,function(e,t,n){},,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}();n(25);var a,r=n(8),o=(a=r)&&a.__esModule?a:{default:a};var l={languages:1,lastEdited:2,pageIssues:3,disambiguation:4,coordinate:5,talkPage:6},s=function(){function e(t,n,i,a){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this.title=t,this.subtitle=n,this.itemType=i,this.clickHandler=a,this.payload=[]}return i(e,[{key:"iconClass",value:function(){switch(this.itemType){case l.languages:return"pagelib_footer_menu_icon_languages";case l.lastEdited:return"pagelib_footer_menu_icon_last_edited";case l.talkPage:return"pagelib_footer_menu_icon_talk_page";case l.pageIssues:return"pagelib_footer_menu_icon_page_issues";case l.disambiguation:return"pagelib_footer_menu_icon_disambiguation";case l.coordinate:return"pagelib_footer_menu_icon_coordinate";default:return""}}},{key:"payloadExtractor",value:function(){switch(this.itemType){case l.pageIssues:return o.default.collectPageIssuesText;case l.disambiguation:return function(e,t){return o.default.collectDisambiguationTitles(t)};default:return}}}]),e}();t.default={MenuItemType:l,setHeading:function(e,t,n){var i=n.getElementById(t);i.innerText=e,i.title=e},maybeAddItem:function(e,t,n,i,a,r){var o=new s(e,t,n,a),l=o.payloadExtractor();l&&(o.payload=l(r,r.querySelector("div#content_block_0")),0===o.payload.length)||function(e,t,n){n.getElementById(t).appendChild(function(e,t){var n=t.createElement("div");n.className="pagelib_footer_menu_item";var i=t.createElement("a");if(i.addEventListener("click",function(){e.clickHandler(e.payload)}),n.appendChild(i),e.title){var a=t.createElement("div");a.className="pagelib_footer_menu_item_title",a.innerText=e.title,i.title=e.title,i.appendChild(a)}if(e.subtitle){var r=t.createElement("div");r.className="pagelib_footer_menu_item_subtitle",r.innerText=e.subtitle,i.appendChild(r)}var o=e.iconClass();return o&&n.classList.add(o),t.createDocumentFragment().appendChild(n)}(e,n))}(o,i,r)}}},,function(e,t,n){},,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,a=n(0),r=(i=a)&&i.__esModule?i:{default:i};var o=function(e){var t=e.querySelector('[id="coordinates"]'),n=t?t.textContent.length:0;return e.textContent.length-n>=50},l=function(e){var t=[],n=e;do{t.push(n),n=n.nextSibling}while(n&&(1!==n.nodeType||"P"!==n.tagName));return t},s=function(e,t){return r.default.querySelectorAll(e,"#"+t+" > p").find(o)};t.default={moveLeadIntroductionUp:function(e,t,n){var i=s(e,t);if(i){var a=e.createDocumentFragment();l(i).forEach(function(e){return a.appendChild(e)});var r=e.getElementById(t),o=n?n.nextSibling:r.firstChild;r.insertBefore(a,o)}},test:{isParagraphEligible:o,extractLeadIntroductionNodes:l,getEligibleParagraph:s}}},,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(33);var i={SECTION_HEADER:"pagelib_edit_section_header",TITLE:"pagelib_edit_section_title",LINK_CONTAINER:"pagelib_edit_section_link_container",LINK:"pagelib_edit_section_link",PROTECTION:{UNPROTECTED:"",PROTECTED:"page-protected",FORBIDDEN:"no-editing"}},a="data-id",r="data-action",o=function(e,t){var n=e.createElement("span");n.classList.add(i.LINK_CONTAINER);var o=function(e,t){var n=e.createElement("a");return n.href="",n.setAttribute(a,t),n.setAttribute(r,"edit_section"),n.classList.add(i.LINK),n}(e,t);return n.appendChild(o),n};t.default={CLASS:i,newEditSectionButton:o,newEditSectionHeader:function(e,t,n,r){var l=e.createElement("div");l.className=i.SECTION_HEADER;var s=e.createElement("h"+n);s.innerHTML=r||"",s.className=i.TITLE,s.setAttribute(a,t),l.appendChild(s);var u=o(e,t);return l.appendChild(u),l}}},,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(36);var i="pagelib_dim_images";t.default={CLASS:i,isDim:function(e){return e.document.querySelector("html").classList.contains(i)},dim:function(e,t){e.document.querySelector("html").classList[t?"add":"remove"](i)}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i={FILTER:"pagelib_compatibility_filter"};t.default={COMPATIBILITY:i,enableSupport:function(e){var t=e.querySelector("html");(function(e){return function(e,t,n){var i=e.createElement("span");return t.some(function(e){return i.style[e]=n,i.style.cssText})}(e,["webkitFilter","filter"],"blur(0)")})(e)||t.classList.add(i.FILTER)}}},,function(e,t,n){},,function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(42);var i=r(n(1)),a=r(n(0));function r(e){return e&&e.__esModule?e:{default:e}}var o={IMAGE_PRESUMES_WHITE_BACKGROUND:"pagelib_theme_image_presumes_white_background",DIV_DO_NOT_APPLY_BASELINE:"pagelib_theme_div_do_not_apply_baseline"},l={DEFAULT:"pagelib_theme_default",DARK:"pagelib_theme_dark",SEPIA:"pagelib_theme_sepia",BLACK:"pagelib_theme_black"},s=new RegExp("Kit_(body|socks|shorts|right_arm|left_arm)(.*).png$"),u=function(e){return!s.test(e.src)&&(!e.classList.contains("mwe-math-fallback-image-inline")&&!i.default.closestInlineStyle(e,"background"))};t.default={CONSTRAINT:o,THEME:l,setTheme:function(e,t){var n=e.querySelector("html");for(var i in n.classList.add(t),l)Object.prototype.hasOwnProperty.call(l,i)&&l[i]!==t&&n.classList.remove(l[i])},classifyElements:function(e){a.default.querySelectorAll(e,"img").filter(u).forEach(function(e){e.classList.add(o.IMAGE_PRESUMES_WHITE_BACKGROUND)});var t=["div.color_swatch div",'div[style*="position: absolute"]','div.barbox table div[style*="background:"]','div.chart div[style*="background-color"]','div.chart ul li span[style*="background-color"]',"span.legend-color","div.mw-highlight span","code.mw-highlight span"].join();a.default.querySelectorAll(e,t).forEach(function(e){return e.classList.add(o.DIV_DO_NOT_APPLY_BASELINE)})}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=w(n(43)),a=w(n(9)),r=w(n(8)),o=w(n(38)),l=w(n(37)),s=w(n(34)),u=w(n(7)),c=w(n(1)),d=w(n(31)),f=w(n(6)),_=w(n(5)),p=w(n(26)),h=w(n(4)),g=w(n(21)),m=w(n(3)),v=w(n(18)),b=w(n(17)),y=w(n(0)),E=w(n(16)),T=w(n(2)),L=w(n(15));function w(e){return e&&e.__esModule?e:{default:e}}t.default={CollapseTable:a.default,CollectionUtilities:r.default,CompatibilityTransform:o.default,DimImagesTransform:l.default,EditTransform:s.default,LeadIntroductionTransform:d.default,FooterContainer:f.default,FooterLegal:_.default,FooterMenu:p.default,FooterReadMore:h.default,FooterTransformer:g.default,LazyLoadTransform:m.default,LazyLoadTransformer:v.default,PlatformTransform:b.default,RedLinks:E.default,ThemeTransform:i.default,WidenImage:L.default,test:{ElementGeometry:u.default,ElementUtilities:c.default,Polyfill:y.default,Throttle:T.default}}}]).default});

},{}]},{},[2,6,17,10,11,12,13,16,14,15,1,4,5,3,7,8,9]);
