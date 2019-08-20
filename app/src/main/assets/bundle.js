(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
var bridge = require('./bridge');
var pagelib = require("wikimedia-page-library");

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

bridge.registerListener( 'handleReference', function( payload ) {
    handleReference( "#" + payload.anchor, null, payload.text );
} );

function handleReference( href, linkNode, linkText ) {
    var targetElem = document.getElementById(href.slice(1));
    if (linkNode && pagelib.ReferenceCollection.isCitation(href)){
        var adjacentReferences = pagelib.ReferenceCollection.collectNearbyReferences(document, linkNode);
        bridge.sendMessage( 'referenceClicked', adjacentReferences );
    } else if ( href.slice(1, 5).toLowerCase() === "cite" ) {
        try {
            var refTexts = targetElem.getElementsByClassName( "reference-text" );
            if ( refTexts.length > 0 ) {
                targetElem = refTexts[0];
            }
            bridge.sendMessage( 'referenceClicked', { "selectedIndex": 0, "referencesGroup": [ { "html": targetElem.innerHTML, "text": linkText } ] });
        } catch (e) {
            targetElem.scrollIntoView();
        }
    } else {
        if ( targetElem === null ) {
            console.log( "reference target not found: " + href );
        } else {
            // If it is a link to another anchor in the current page, just scroll to it
            targetElem.scrollIntoView();
        }
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
                handleReference(href, event.target, null);
            } else if (sourceNode.classList.contains( 'app_media' )) {
                bridge.sendMessage( 'mediaClicked', { "href": href } );
            } else if (sourceNode.classList.contains( 'image' )) {
                bridge.sendMessage( 'imageClicked', { "href": href } );
            } else {
                var response = { "href": href, "text": sourceNode.textContent };
                if (sourceNode.hasAttribute( "title" )) {
                    response.title = sourceNode.getAttribute( "title" );
                }
                bridge.sendMessage( 'linkClicked', response );
            }
            event.preventDefault();
        }
    }
};

module.exports = new ActionsHandler();

},{"./bridge":2,"wikimedia-page-library":17}],2:[function(require,module,exports){
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
    marshaller.onReceiveMessage( JSON.stringify( messagePack ) );
};

module.exports = new Bridge();
// FIXME: Move this to somewhere else, eh?
window.onload = function() {
    module.exports.sendMessage( "DOMLoaded", {} );
};
},{}],3:[function(require,module,exports){
var actions = require('./actions');
var bridge = require('./bridge');

actions.register( "edit_section", function( el, event ) {
    var sectionID = el.getAttribute( 'data-id' );
    if (sectionID === "0") {
        bridge.sendMessage( 'editSectionClicked', { mainPencilClicked: true, 'x': el.parentNode.offsetLeft, 'y': el.parentNode.offsetTop } );
    } else {
        bridge.sendMessage( 'editSectionClicked', { sectionID: sectionID } );
    }
    event.preventDefault();
} );

actions.register( "add_title_description", function( el, event ) {
    bridge.sendMessage( 'editSectionClicked', { editDescriptionClicked: true } );
    event.preventDefault();
} );

actions.register( "title_pronunciation", function( el, event ) {
    bridge.sendMessage( 'pronunciationClicked', { } );
    event.preventDefault();
} );

},{"./actions":1,"./bridge":2}],4:[function(require,module,exports){
var transformer = require('./transformer');

transformer.register( 'showIssues', function( content ) {
    var issues = content.querySelectorAll( ".ambox" );
    var style;
    for (var i = 0; i < issues.length; i++ ) {
        style = issues[i].getAttribute('style');
        if (!style) {
            style = "";
        }
        issues[i].setAttribute('style', style + "display:block !important;");
    }
    return content;
} );

},{"./transformer":8}],5:[function(require,module,exports){
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
},{"./bridge":2,"wikimedia-page-library":17}],6:[function(require,module,exports){
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

},{"./bridge":2}],7:[function(require,module,exports){
var bridge = require("./bridge");
var transformer = require("./transformer");
var pagelib = require("wikimedia-page-library");
var lazyLoadViewportDistanceMultiplier = 2; // Load images on the current screen up to one ahead.
var lazyLoadTransformer = new pagelib.LazyLoadTransformer(window, lazyLoadViewportDistanceMultiplier);

pagelib.PlatformTransform.classify( window );
pagelib.CompatibilityTransform.enableSupport( document );

bridge.registerListener( "setDecorOffset", function( payload ) {
    transformer.setDecorOffset(payload.offset);
} );

bridge.registerListener( "setPaddingTop", function( payload ) {
    setPaddingTop( payload.paddingTop );
});

bridge.registerListener( "setPaddingBottom", function( payload ) {
    document.body.style.paddingBottom = payload.paddingBottom + "px";
});

function setPaddingTop( paddingTop ) {
    document.body.style.paddingTop = paddingTop + "px";
}

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
    var sectionID = getCurrentSection();
    var editDescription = false;
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
    if (sectionID === "0" && window.allowDescriptionEdit) {
        var getSelectionContainerId = window.getSelection().getRangeAt(0).commonAncestorContainer.parentNode.getAttribute('id');
        editDescription = getSelectionContainerId && getSelectionContainerId === "pagelib_edit_section_title_description";
    }
    bridge.sendMessage( "onGetTextSelection", { "purpose" : payload.purpose, "text" : text, "sectionID" : sectionID, "editDescription" : editDescription } );
});

function setWindowAttributes( payload ) {
    window.sequence = payload.sequence;
    window.apiLevel = payload.apiLevel;
    window.string_table_infobox = payload.string_table_infobox;
    window.string_table_other = payload.string_table_other;
    window.string_table_close = payload.string_table_close;
    window.string_expand_refs = payload.string_expand_refs;
    window.string_add_description = payload.string_add_description;
    window.pageTitle = payload.title;
    window.pageDescription = payload.description;
    window.allowDescriptionEdit = payload.allowDescriptionEdit;
    window.hasPronunciation = payload.hasPronunciation;
    window.isMainPage = payload.isMainPage;
    window.isFilePage = payload.isFilePage;
    window.fromRestBase = payload.fromRestBase;
    window.isBeta = payload.isBeta;
    window.siteLanguage = payload.siteLanguage;
    window.showImages = payload.showImages;
    window.collapseTables = payload.collapseTables;
    window.dimImages = payload.dimImages;
    window.imagePlaceholderBackgroundColor = payload.imagePlaceholderBackgroundColor;
}

function setTitleElement( parentNode, section ) {

    var container = pagelib.EditTransform.newEditLeadSectionHeader(document, window.pageTitle,
        window.pageDescription, window.string_add_description,
        window.allowDescriptionEdit, section.noedit, window.hasPronunciation);

    var sectionHeader = container.querySelector( ".pagelib_edit_section_header" );
    sectionHeader.setAttribute( "data-id", 0 );

    parentNode.appendChild(container);
}

function setOrUnsetClass( tag, className, set ) {
    if (set && !tag.classList.contains(className)) {
        tag.classList.add(className);
    } else if (!set && tag.classList.contains(className)) {
        tag.classList.remove(className);
    }
}

bridge.registerListener( "displayLeadSection", function( payload ) {
    lazyLoadTransformer.deregister();

    var htmlTag = document.getElementsByTagName( "html" )[0];
    var contentElem = document.getElementById( "content" );

    // clear all the content!
    while (contentElem.firstChild) {
        contentElem.firstChild.remove();
    }
    window.scrollTo( 0, 0 );

    setWindowAttributes(payload);
    window.offline = false;

    // Set the base URL for the whole page in the HEAD tag.
    document.head.getElementsByTagName( "base" )[0].setAttribute("href", payload.siteBaseUrl);

    // Set the URL for the wiki-specific CSS.
    var localStyleTag = document.getElementById( "localSiteStylesheet" );
    var localStyleUrl = payload.siteBaseUrl + "/api/rest_v1/data/css/mobile/site";
    if (localStyleTag.getAttribute("href") !== localStyleUrl) {
        localStyleTag.setAttribute("href", localStyleUrl);
    }

    if (!htmlTag.classList.contains(payload.theme)) {
        // theme change, which means we can clear out all other classes from the html tag
        htmlTag.className = "";
        htmlTag.classList.add(payload.theme);
    }
    setOrUnsetClass(htmlTag, "pagelib_dim_images", payload.dimImages);
    setOrUnsetClass(htmlTag, "page-protected", payload.protect);
    setOrUnsetClass(htmlTag, "no-editing", payload.noedit);

    setPaddingTop(payload.paddingTop);

    pagelib.DimImagesTransform.dim( window, window.dimImages );

    contentElem.setAttribute( "dir", window.directionality );
    if (!window.isMainPage) {
        setTitleElement(contentElem, payload.section);
    }

    var content = document.createElement( "div" );
    content.innerHTML = payload.section.text;
    content.id = "content_block_0";

    applySectionTransforms(content, true);
    contentElem.appendChild( content );

    // and immediately queue the request to load the remaining sections of the article.
    queueRemainingSections( payload.remainingUrl, payload.sequence, payload.fragment )
});

function getSectionFragment( section ) {
    var content;
    var header = pagelib.EditTransform.newEditSectionHeader(document,
              section.id, section.toclevel + 1, section.line, !section.noedit);
    header.id = section.anchor;
    header.setAttribute( 'data-id', section.id );
    var frag = document.createDocumentFragment();
    content = document.createElement( "div" );
    frag.appendChild(header);
    frag.appendChild(content);
    content.innerHTML = section.text;
    content.id = "content_block_" + section.id;
    applySectionTransforms(content, false);
    return frag;
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
}

function displayRemainingSections(json, sequence, fragment) {
    var contentWrapper = document.getElementById( "content" );
    var response = { "sequence": sequence };

    var frag = document.createDocumentFragment();
    var allowScrollToSection = false;
    json.sections.forEach(function (section) {
        frag.appendChild(getSectionFragment(section));
        if ( typeof fragment === "string" && fragment.length > 0 && section.anchor === fragment) {
            allowScrollToSection = true;
        }
    });

    contentWrapper.appendChild(frag);

    transformer.transform( "fixAudio", document );
    transformer.transform( "hideTables", document );
    transformer.transform( "showIssues", document );

    if (allowScrollToSection) {
        scrollToSection( fragment );
    }

    lazyLoadTransformer.loadPlaceholders();
    bridge.sendMessage( "pageLoadComplete", response );
}

var remainingRequest;

function queueRemainingSections( url, sequence, fragment ) {
    if (remainingRequest) {
        remainingRequest.abort();
    }
    remainingRequest = new XMLHttpRequest();
    remainingRequest.open('GET', url);
    remainingRequest.sequence = sequence;
    remainingRequest.fragment = fragment;
    remainingRequest.responseType = 'json';

    remainingRequest.onreadystatechange = function() {
        if (this.readyState !== XMLHttpRequest.DONE || this.status === 0 || this.sequence !== window.sequence) {
            return;
        }
        if (this.status < 200 || this.status > 299) {
            bridge.sendMessage( "loadRemainingError", { "status": this.status, "sequence": this.sequence });
            return;
        }
        try {
            var sectionsObj = this.response;
            if (sectionsObj.mobileview) {
                // If it's a mobileview response, the "sections" object will be one level deeper.
                sectionsObj = sectionsObj.mobileview;
            }
            displayRemainingSections(sectionsObj, this.sequence, this.fragment);
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
}

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
        var scrollY = el.offsetTop - transformer.getDecorOffset();
        window.scrollTo( 0, scrollY );
    }
}

bridge.registerListener( "scrollToBottom", function ( payload ) {
    window.scrollTo(0, document.body.scrollHeight - payload.offset - transformer.getDecorOffset());
});

bridge.registerListener( "requestSectionData", function () {
    var headings = document.getElementsByClassName( "pagelib_edit_section_header" );
    var sections = [];
    var docYOffset = document.documentElement.getBoundingClientRect().top;
    for (var i = 0; i < headings.length; i++) {
        sections[i] = {};
        sections[i].heading = headings[i].textContent;
        sections[i].id = headings[i].getAttribute( "data-id" );
        sections[i].yOffset = headings[i].getBoundingClientRect().top - docYOffset;
    }
    bridge.sendMessage( "sectionDataResponse", { "sections": sections } );
});

/**
 * Returns the section id of the section that has the header closest to but above midpoint of screen,
 * or -1 if the page is scrolled all the way to the bottom (i.e. native bottom content should be shown).
 */
function getCurrentSection() {
    var sectionHeaders = document.querySelectorAll( ".section_heading, .pagelib_edit_section_header" );
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

    return curClosest ? curClosest.getAttribute( "data-id" ) : 0;
}

},{"./bridge":2,"./transformer":8,"wikimedia-page-library":17}],8:[function(require,module,exports){
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
},{}],9:[function(require,module,exports){
var pagelib = require("wikimedia-page-library");
var transformer = require("../transformer");

function scrollWithDecorOffset(container) {
    window.scrollTo( 0, container.parentNode.offsetTop - transformer.getDecorOffset() );
}

function toggleCollapseClickCallback() {
    pagelib.CollapseTable.toggleCollapseClickCallback.call(this, scrollWithDecorOffset);
}

transformer.register( "hideTables", function(document) {
    pagelib.CollapseTable.adjustTables(window, document, window.pageTitle,
        window.isMainPage, window.collapseTables, window.string_table_infobox,
        window.string_table_other, window.string_table_close,
        scrollWithDecorOffset);
});

module.exports = {
    handleTableCollapseOrExpandClick: toggleCollapseClickCallback
};

},{"../transformer":8,"wikimedia-page-library":17}],10:[function(require,module,exports){
var transformer = require("../transformer");

/* TODO: remove once implemented in content service. */

transformer.register( "fixAudio", function( content ) {
    var videoTags = content.querySelectorAll( 'video' );
    // Find audio files masquerading as <video> tags, and fix them by
    // replacing them with <audio> tags.
    for ( var i = 0; i < videoTags.length; i++ ) {
        var videoTag = videoTags[i];
        var resource = videoTag.getAttribute( 'resource' );
        // If the 'resource' attribute is an .ogg file, then it's audio.
        if ( resource && resource.indexOf( '.ogg' ) > 0 ) {
            videoTag.removeAttribute( 'poster' );

            // Make a proper <audio> tag, and replace the <video> tag with it.
            var audioTag = document.createElement( 'audio' );
            // Copy the children
            while (videoTag.firstChild) {
                audioTag.appendChild( videoTag.firstChild ); // Increments the child
            }
            // Copy the attributes
            for (var index = videoTag.attributes.length - 1; index >= 0; --index) {
                audioTag.attributes.setNamedItem(videoTag.attributes[index].cloneNode());
            }
            // Replace it
            videoTag.parentNode.replaceChild(audioTag, videoTag);
        }
    }
} );

},{"../transformer":8}],11:[function(require,module,exports){
var transformer = require("../transformer");

transformer.register( "hideImages", function( content ) {
    var minImageSize = 64;
    var images = content.querySelectorAll( 'img:not(.mwe-math-fallback-image-inline)' );
    for (var i = 0; i < images.length; i++) {
        var img = images[i];
        if (img.width < minImageSize && img.height < minImageSize) {
            continue;
        }
        img.src = "";
        img.srcset = "";
        img.parentElement.style.backgroundColor = window.imagePlaceholderBackgroundColor;
    }
} );

},{"../transformer":8}],12:[function(require,module,exports){
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
},{"../transformer":8}],13:[function(require,module,exports){
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

},{"../../transformer":8}],14:[function(require,module,exports){
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

},{"../../transformer":8}],15:[function(require,module,exports){
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

},{"../transformer":8,"wikimedia-page-library":17}],16:[function(require,module,exports){
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

},{}],17:[function(require,module,exports){
!function(e,t){"object"==typeof exports&&"object"==typeof module?module.exports=t():"function"==typeof define&&define.amd?define([],t):"object"==typeof exports?exports.pagelib=t():e.pagelib=t()}(this,function(){return function(e){var t={};function n(i){if(t[i])return t[i].exports;var r=t[i]={i:i,l:!1,exports:{}};return e[i].call(r.exports,r,r.exports,n),r.l=!0,r.exports}return n.m=e,n.c=t,n.d=function(e,t,i){n.o(e,t)||Object.defineProperty(e,t,{enumerable:!0,get:i})},n.r=function(e){"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},n.t=function(e,t){if(1&t&&(e=n(e)),8&t)return e;if(4&t&&"object"==typeof e&&e&&e.__esModule)return e;var i=Object.create(null);if(n.r(i),Object.defineProperty(i,"default",{enumerable:!0,value:e}),2&t&&"string"!=typeof e)for(var r in e)n.d(i,r,function(t){return e[t]}.bind(null,r));return i},n.n=function(e){var t=e&&e.__esModule?function(){return e.default}:function(){return e};return n.d(t,"a",t),t},n.o=function(e,t){return Object.prototype.hasOwnProperty.call(e,t)},n.p="",n(n.s=40)}([function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i="undefined"!=typeof window&&window.CustomEvent||function(e){var t=arguments.length>1&&void 0!==arguments[1]?arguments[1]:{bubbles:!1,cancelable:!1,detail:void 0},n=document.createEvent("CustomEvent");return n.initCustomEvent(e,t.bubbles,t.cancelable,t.detail),n};t.default={matchesSelector:function(e,t){return e.matches?e.matches(t):e.matchesSelector?e.matchesSelector(t):!!e.webkitMatchesSelector&&e.webkitMatchesSelector(t)},querySelectorAll:function(e,t){return Array.prototype.slice.call(e.querySelectorAll(t))},CustomEvent:i}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,r=n(0),a=(i=r)&&i.__esModule?i:{default:i};var o=function(e,t){var n=void 0;for(n=e.parentElement;n&&!a.default.matchesSelector(n,t);n=n.parentElement);return n};t.default={findClosestAncestor:o,isNestedInTable:function(e){return Boolean(o(e,"table"))},closestInlineStyle:function(e,t,n){for(var i=e;i;i=i.parentElement){var r=i.style[t];if(r){if(void 0===n)return i;if(n===r)return i}}},isVisible:function(e){return Boolean(e.offsetWidth||e.offsetHeight||e.getClientRects().length)},copyAttributesToDataAttributes:function(e,t,n){n.filter(function(t){return e.hasAttribute(t)}).forEach(function(n){return t.setAttribute("data-"+n,e.getAttribute(n))})},copyDataAttributesToAttributes:function(e,t,n){n.filter(function(t){return e.hasAttribute("data-"+t)}).forEach(function(n){return t.setAttribute(n,e.getAttribute("data-"+n))})}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(18);var i=o(n(1)),r=o(n(3)),a=o(n(0));function o(e){return e&&e.__esModule?e:{default:e}}var l=r.default.NODE_TYPE,s={ICON:"pagelib_collapse_table_icon",CONTAINER:"pagelib_collapse_table_container",COLLAPSED_CONTAINER:"pagelib_collapse_table_collapsed_container",COLLAPSED:"pagelib_collapse_table_collapsed",COLLAPSED_BOTTOM:"pagelib_collapse_table_collapsed_bottom",COLLAPSE_TEXT:"pagelib_collapse_table_collapse_text",EXPANDED:"pagelib_collapse_table_expanded",TABLE_INFOBOX:"pagelib_table_infobox",TABLE_OTHER:"pagelib_table_other"},u=function(e){return e.childNodes&&a.default.querySelectorAll(e,"a").length<3},c=function(e){return e&&e.replace(/[\s0-9]/g,"").length>0},d=function(e){var t=e.match(/\w+/);if(t)return t[0]},f=function(e,t){var n=d(t),i=d(e.textContent);return!(!n||!i)&&n.toLowerCase()===i.toLowerCase()},_=function(e){return e.trim().replace(/\s/g," ")},p=function(e){return e.nodeType===l.ELEMENT_NODE&&"BR"===e.tagName},h=function(e,t){t.parentNode.replaceChild(e.createTextNode(" "),t)},g=function(e,t,n){if(!u(t))return null;var i=e.createDocumentFragment();i.appendChild(t.cloneNode(!0));var o=i.querySelector("th");a.default.querySelectorAll(o,".geo, .coordinates, sup.reference, ol, ul, style, script").forEach(function(e){return e.remove()});var l=Array.prototype.slice.call(o.childNodes);n&&l.filter(r.default.isNodeTypeElementOrText).filter(function(e){return f(e,n)}).forEach(function(e){return e.remove()}),l.filter(p).forEach(function(t){return h(e,t)});var s=o.textContent;return c(s)?_(s):null},m=function(e,t){var n=e.hasAttribute("scope"),i=t.hasAttribute("scope");return n&&i?0:n?-1:i?1:0},v=function(e,t,n){var i=[],r=a.default.querySelectorAll(t,"th");r.sort(m);for(var o=0;o<r.length;++o){var l=g(e,r[o],n);if(l&&-1===i.indexOf(l)&&(i.push(l),2===i.length))break}return i},b=function(e,t,n){var i=e.children[0],r=e.children[1],a=e.children[2],o=i.querySelector(".app_table_collapsed_caption"),l="none"!==r.style.display;return l?(r.style.display="none",i.classList.remove(s.COLLAPSED),i.classList.remove(s.ICON),i.classList.add(s.EXPANDED),o&&(o.style.visibility="visible"),a.style.display="none",t===a&&n&&n(e)):(r.style.display="block",i.classList.remove(s.EXPANDED),i.classList.add(s.COLLAPSED),i.classList.add(s.ICON),o&&(o.style.visibility="hidden"),a.style.display="block"),l},y=function(e){var t=this.parentNode;return b(t,this,e)},E=function(e){var t=["navbox","vertical-navbox","navbox-inner","metadata","mbox-small"].some(function(t){return e.classList.contains(t)});return"none"!==e.style.display&&!t},T=function(e){return e.classList.contains("infobox")||e.classList.contains("infobox_v3")},L=function(e,t){var n=e.createElement("div");return n.classList.add(s.COLLAPSED_CONTAINER),n.classList.add(s.EXPANDED),n.appendChild(t),n},C=function(e,t){var n=e.createElement("div");return n.classList.add(s.COLLAPSED_BOTTOM),n.classList.add(s.ICON),n.innerHTML=t||"",n},A=function(e,t,n,i){var r=e.createDocumentFragment(),a=e.createElement("strong");a.innerHTML=t,a.classList.add(n),r.appendChild(a);var o=e.createElement("span");return o.classList.add(s.COLLAPSE_TEXT),i.length>0&&o.appendChild(e.createTextNode(": "+i[0])),i.length>1&&o.appendChild(e.createTextNode(", "+i[1])),i.length>0&&o.appendChild(e.createTextNode(" …")),r.appendChild(o),r},N=function(e,t){if(e&&t){var n=e,i=e.parentNode;if(i){for(var r,a=!1;i;){if("SECTION"===(r=i).tagName&&r.attributes&&r.attributes["data-mw-section-id"]||"DIV"===r.tagName&&r.classList&&r.classList.contains("content_block")){a=!0;break}n=i,i=i.parentNode}a||(n=e,i=e.parentNode),i.insertBefore(t,n),i.removeChild(n)}}},S=function(e,t,n,r,a){for(var o=e.querySelectorAll("table, .infobox_v3"),l=0;l<o.length;++l){var u=o[l];if(!i.default.findClosestAncestor(u,"."+s.CONTAINER)&&E(u)){var c=v(e,u,t);if(c.length||T(u)){var d=A(e,T(u)?n:r,T(u)?s.TABLE_INFOBOX:s.TABLE_OTHER,c),f=e.createElement("div");f.className=s.CONTAINER,N(u,f),u.style.marginTop="0px",u.style.marginBottom="0px";var _=L(e,d);_.style.display="block";var p=C(e,a);p.style.display="none",f.appendChild(_),f.appendChild(u),f.appendChild(p),u.style.display="none"}}}},O=function(e){a.default.querySelectorAll(e,"."+s.CONTAINER).forEach(function(e){b(e)})},w=function(e,t,n,i){var r=function(t){return e.dispatchEvent(new a.default.CustomEvent("section-toggled",{collapsed:t}))};a.default.querySelectorAll(t,"."+s.COLLAPSED_CONTAINER).forEach(function(e){e.onclick=function(){var t=y.bind(e)();r(t)}}),a.default.querySelectorAll(t,"."+s.COLLAPSED_BOTTOM).forEach(function(e){e.onclick=function(){var t=y.bind(e,i)();r(t)}}),n||O(t)},P=function(e,t,n,i,r,a,o,l,s){i||(S(t,n,a,o,l),w(e,t,r,s))};t.default={CLASS:s,SECTION_TOGGLED_EVENT_TYPE:"section-toggled",toggleCollapsedForAll:O,toggleCollapseClickCallback:y,collapseTables:function(e,t,n,i,r,a,o,l){P(e,t,n,i,!0,r,a,o,l)},adjustTables:P,prepareTables:S,setupEventHandling:w,expandCollapsedTableIfItContainsElement:function(e){if(e){var t='[class*="'+s.CONTAINER+'"]',n=i.default.findClosestAncestor(e,t);if(n){var r=n.firstElementChild;r&&r.classList.contains(s.EXPANDED)&&r.click()}}},test:{elementScopeComparator:m,extractEligibleHeaderText:g,firstWordFromString:d,getTableHeaderTextArray:v,shouldTableBeCollapsed:E,isHeaderEligible:u,isHeaderTextEligible:c,isInfobox:T,newCollapsedHeaderDiv:L,newCollapsedFooterDiv:C,newCaptionFragment:A,isNodeTextContentSimilarToPageTitle:f,stringWithNormalizedWhitespace:_,replaceNodeWithBreakingSpaceTextNode:h}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i={ELEMENT_NODE:1,TEXT_NODE:3};t.default={isNodeTypeElementOrText:function(e){return e.nodeType===i.ELEMENT_NODE||e.nodeType===i.TEXT_NODE},NODE_TYPE:i}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(19);var i=n(10).default,r=n(1).default,a=n(0).default,o={PLACEHOLDER_CLASS:"pagelib_lazy_load_placeholder",PLACEHOLDER_PENDING_CLASS:"pagelib_lazy_load_placeholder_pending",PLACEHOLDER_LOADING_CLASS:"pagelib_lazy_load_placeholder_loading",PLACEHOLDER_ERROR_CLASS:"pagelib_lazy_load_placeholder_error",IMAGE_LOADING_CLASS:"pagelib_lazy_load_image_loading",IMAGE_LOADED_CLASS:"pagelib_lazy_load_image_loaded"},l=["class","style","src","srcset","width","height","alt","usemap","data-file-width","data-file-height","data-image-gallery"],s={px:50,ex:10,em:5};t.default={CLASSES:o,PLACEHOLDER_CLASS:"pagelib_lazy_load_placeholder",queryLazyLoadableImages:function(e){return a.querySelectorAll(e,"img").filter(function(e){return function(e){var t=i.from(e);if(!t.width||!t.height)return!0;var n=s[t.widthUnit]||1/0,r=s[t.heightUnit]||1/0;return t.widthValue>=n&&t.heightValue>=r}(e)})},convertImagesToPlaceholders:function(e,t){return t.map(function(t){return function(e,t){var n=e.createElement("span");t.hasAttribute("class")&&n.setAttribute("class",t.getAttribute("class")||""),n.classList.add("pagelib_lazy_load_placeholder"),n.classList.add("pagelib_lazy_load_placeholder_pending");var a=i.from(t);a.width&&n.style.setProperty("width",""+a.width),r.copyAttributesToDataAttributes(t,n,l);var o=e.createElement("span");if(a.width&&a.height){var s=a.heightValue/a.widthValue;o.style.setProperty("padding-top",100*s+"%")}return n.appendChild(o),t.parentNode&&t.parentNode.replaceChild(n,t),n}(e,t)})},loadPlaceholder:function(e,t){t.classList.add("pagelib_lazy_load_placeholder_loading"),t.classList.remove("pagelib_lazy_load_placeholder_pending");var n=e.createElement("img"),i=function(e){n.setAttribute("src",n.getAttribute("src")||""),e.stopPropagation(),e.preventDefault()};return n.addEventListener("load",function(){t.removeEventListener("click",i),t.parentNode&&t.parentNode.replaceChild(n,t),n.classList.add("pagelib_lazy_load_image_loaded"),n.classList.remove("pagelib_lazy_load_image_loading")},{once:!0}),n.addEventListener("error",function(){t.classList.add("pagelib_lazy_load_placeholder_error"),t.classList.remove("pagelib_lazy_load_placeholder_loading"),t.addEventListener("click",i)},{once:!0}),r.copyDataAttributesToAttributes(t,n,l),n.classList.add("pagelib_lazy_load_image_loading"),n}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}();var r=function(){function e(t,n,i){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this._window=t,this._period=n,this._function=i,this._context=void 0,this._arguments=void 0,this._result=void 0,this._timeout=0,this._timestamp=0}return i(e,null,[{key:"wrap",value:function(t,n,i){var r=new e(t,n,i),a=function(){return r.queue(this,arguments)};return a.result=function(){return r.result},a.pending=function(){return r.pending()},a.delay=function(){return r.delay()},a.cancel=function(){return r.cancel()},a.reset=function(){return r.reset()},a}}]),i(e,[{key:"queue",value:function(e,t){var n=this;return this._context=e,this._arguments=t,this.pending()||(this._timeout=this._window.setTimeout(function(){n._timeout=0,n._timestamp=Date.now(),n._result=n._function.apply(n._context,n._arguments)},this.delay())),this.result}},{key:"pending",value:function(){return Boolean(this._timeout)}},{key:"delay",value:function(){return this._timestamp?Math.max(0,this._period-(Date.now()-this._timestamp)):0}},{key:"cancel",value:function(){this._timeout&&this._window.clearTimeout(this._timeout),this._timeout=0}},{key:"reset",value:function(){this.cancel(),this._result=void 0,this._timestamp=0}},{key:"result",get:function(){return this._result}}]),e}();t.default=r},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(13);t.default={containerFragment:function(e){var t=e.createDocumentFragment(),n=e.createElement("section");n.id="pagelib_footer_container_menu",n.className="pagelib_footer_section",n.innerHTML="<h2 id='pagelib_footer_container_menu_heading'></h2>\n   <div id='pagelib_footer_container_menu_items'></div>",t.appendChild(n);var i=e.createElement("section");i.id="pagelib_footer_container_readmore",i.className="pagelib_footer_section",i.innerHTML="<h2 id='pagelib_footer_container_readmore_heading'></h2>\n   <div id='pagelib_footer_container_readmore_pages'></div>",t.appendChild(i);var r=e.createElement("section");return r.id="pagelib_footer_container_legal",t.appendChild(r),t},isContainerAttached:function(e){return Boolean(e.querySelector("#pagelib_footer_container"))}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(14);t.default={add:function(e,t,n,i,r,a,o){var l=e.querySelector("#"+i),s=t.split("$1");l.innerHTML="<div class='pagelib_footer_legal_contents'>\n    <hr class='pagelib_footer_legal_divider'>\n    <span class='pagelib_footer_legal_license'>\n      "+s[0]+"\n      <a class='pagelib_footer_legal_license_link'>\n        "+n+"\n      </a>\n      "+s[1]+"\n      <br>\n      <div class=\"pagelib_footer_browser\">\n        <a class='pagelib_footer_browser_link'>\n          "+a+"\n        </a>\n      </div>\n    </span>\n  </div>",l.querySelector(".pagelib_footer_legal_license_link").addEventListener("click",function(){r()}),l.querySelector(".pagelib_footer_browser_link").addEventListener("click",function(){o()})}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,r=n(0),a=(i=r)&&i.__esModule?i:{default:i};var o=function(e,t){if(!t)return[];var n=a.default.querySelectorAll(t,"table.ambox:not(.ambox-multiple_issues):not(.ambox-notice)"),i=e.createDocumentFragment();return n.forEach(function(e){return i.appendChild(e.cloneNode(!0))}),a.default.querySelectorAll(i,".hide-when-compact, .collapsed").forEach(function(e){return e.remove()}),a.default.querySelectorAll(i,"td[class*=mbox-text] > *[class*=mbox-text]")};t.default={collectDisambiguationTitles:function(e){return e?a.default.querySelectorAll(e,'div.hatnote a[href]:not([href=""]):not([redlink="1"])').map(function(e){return e.href}):[]},collectDisambiguationHTML:function(e){return e?a.default.querySelectorAll(e,"div.hatnote").map(function(e){return e.innerHTML}):[]},collectPageIssuesHTML:function(e,t){return o(e,t).map(function(e){return e.innerHTML})},collectPageIssuesText:function(e,t){return o(e,t).map(function(e){return e.textContent.trim()})},test:{collectPageIssues:o}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(17);var i=function(e,t,n){var i=new RegExp("\\s?["+t+"][^"+t+n+"]+["+n+"]","g"),r=0,a=e,o="";do{o=a,a=a.replace(i,""),r++}while(o!==a&&r<30);return a},r=function(e){var t=e;return t=i(t,"(",")"),t=i(t,"/","/")},a=function e(t,n,i,r,a){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this.title=t,this.displayTitle=n,this.thumbnail=i,this.description=r,this.extract=a},o=function(e,t,n,i,o){var l=[],s=o.getElementById(t);e.forEach(function(e,t){var i=e.titles.normalized;l.push(i);var u=function(e,t,n,i){var a=i.createElement("a");if(a.id=t,a.className="pagelib_footer_readmore_page",e.thumbnail&&e.thumbnail.source){var o=i.createElement("div");o.style.backgroundImage="url("+e.thumbnail.source+")",o.classList.add("pagelib_footer_readmore_page_image"),a.appendChild(o)}var l=i.createElement("div");l.classList.add("pagelib_footer_readmore_page_container"),a.appendChild(l),a.href="/wiki/"+encodeURI(e.title)+"?event_logging_label=read_more";var s=void 0;if(e.displayTitle?s=e.displayTitle:e.title&&(s=e.title),s){var u=i.createElement("div");u.id=t,u.className="pagelib_footer_readmore_page_title",u.innerHTML=s.replace(/_/g," "),a.title=e.title.replace(/_/g," "),l.appendChild(u)}var c=void 0;if(e.description&&(c=e.description),(!c||c.length<10)&&e.extract&&(c=r(e.extract)),c){var d=i.createElement("div");d.id=t,d.className="pagelib_footer_readmore_page_description",d.innerHTML=c,l.appendChild(d)}var f=i.createElement("div");return f.id="pagelib_footer_read_more_save_"+encodeURI(e.title),f.className="pagelib_footer_readmore_page_save",f.addEventListener("click",function(t){t.stopPropagation(),t.preventDefault(),n(e.title)}),l.appendChild(f),i.createDocumentFragment().appendChild(a)}(new a(i,e.titles.display,e.thumbnail,e.description,e.extract),t,n,o);s.appendChild(u)}),i(l)},l=function(e){console.log("statusText = "+e)};t.default={add:function(e,t,n,i,r,a,s){!function(e,t,n,i,r,a,o,s){var u=new XMLHttpRequest;u.open("GET",function(e,t,n){return(n||"")+"/page/related/"+e}(e,0,i),!0),u.onload=function(){if(u.readyState===XMLHttpRequest.DONE)if(200===u.status){var e=JSON.parse(u.responseText).pages,i=void 0;if(e.length>t){var c=Math.floor(Math.random()*Math.floor(e.length-t));i=e.slice(c,c+t)}else i=e;r(i,n,a,o,s)}else l(u.statusText)},u.onerror=function(){return l(u.statusText)};try{u.send()}catch(e){l(e.toString())}}(e,t,n,i,o,r,a,s)},setHeading:function(e,t,n){var i=n.getElementById(t);i.innerText=e,i.title=e},updateSaveButtonForTitle:function(e,t,n,i){var r=i.getElementById("pagelib_footer_read_more_save_"+encodeURI(e));r&&(r.innerText=t,r.title=t,function(e,t){var n="pagelib_footer_readmore_bookmark_unfilled",i="pagelib_footer_readmore_bookmark_filled";e.classList.remove(i,n),e.classList.add(t?i:n)}(r,n))},test:{cleanExtract:r,safelyRemoveEnclosures:i}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}();function r(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}var a=function(){function e(t,n){r(this,e),this._value=Number(t),this._unit=n||"px"}return i(e,null,[{key:"fromElement",value:function(t,n){return t.style.getPropertyValue(n)&&e.fromStyle(t.style.getPropertyValue(n))||t.hasAttribute(n)&&new e(t.getAttribute(n))||void 0}},{key:"fromStyle",value:function(t){var n=t.match(/(-?\d*\.?\d*)(\D+)?/)||[];return new e(n[1],n[2])}}]),i(e,[{key:"toString",value:function(){return isNaN(this.value)?"":""+this.value+this.unit}},{key:"value",get:function(){return this._value}},{key:"unit",get:function(){return this._unit}}]),e}(),o=function(){function e(t,n){r(this,e),this._width=t,this._height=n}return i(e,null,[{key:"from",value:function(t){return new e(a.fromElement(t,"width"),a.fromElement(t,"height"))}}]),i(e,[{key:"width",get:function(){return this._width}},{key:"widthValue",get:function(){return this._width&&!isNaN(this._width.value)?this._width.value:NaN}},{key:"widthUnit",get:function(){return this._width&&this._width.unit||"px"}},{key:"height",get:function(){return this._height}},{key:"heightValue",get:function(){return this._height&&!isNaN(this._height.value)?this._height.value:NaN}},{key:"heightUnit",get:function(){return this._height&&this._height.unit||"px"}}]),e}();t.default=o},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i={ANDROID:"pagelib_platform_android",IOS:"pagelib_platform_ios"};t.default={CLASS:i,classify:function(e){var t=e.document.querySelector("html");(function(e){return/android/i.test(e.navigator.userAgent)})(e)&&t.classList.add(i.ANDROID),function(e){return/ipad|iphone|ipod/i.test(e.navigator.userAgent)}(e)&&t.classList.add(i.IOS)},setPlatform:function(e,t){e.querySelector("html").classList.add(t)}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(28);var i=a(n(1)),r=a(n(0));function a(e){return e&&e.__esModule?e:{default:e}}var o={IMAGE_PRESUMES_WHITE_BACKGROUND:"pagelib_theme_image_presumes_white_background",DIV_DO_NOT_APPLY_BASELINE:"pagelib_theme_div_do_not_apply_baseline"},l={DEFAULT:"pagelib_theme_default",DARK:"pagelib_theme_dark",SEPIA:"pagelib_theme_sepia",BLACK:"pagelib_theme_black"},s=new RegExp("Kit_(body|socks|shorts|right_arm|left_arm)(.*).png$"),u=function(e){return!s.test(e.src)&&(!e.classList.contains("mwe-math-fallback-image-inline")&&!i.default.closestInlineStyle(e,"background"))};t.default={CONSTRAINT:o,THEME:l,setTheme:function(e,t){var n=e.querySelector("html");for(var i in n.classList.add(t),l)Object.prototype.hasOwnProperty.call(l,i)&&l[i]!==t&&n.classList.remove(l[i])},classifyElements:function(e){r.default.querySelectorAll(e,"img").filter(u).forEach(function(e){e.classList.add(o.IMAGE_PRESUMES_WHITE_BACKGROUND)});var t=["div.color_swatch div",'div[style*="position: absolute"]','div.barbox table div[style*="background:"]','div.chart div[style*="background-color"]','div.chart ul li span[style*="background-color"]',"span.legend-color","div.mw-highlight span","code.mw-highlight span",".BrickChartTemplate div",".PieChartTemplate div",".BarChartTemplate div",".StackedBarTemplate td",".chess-board div",'span[style*="background"]'].join();r.default.querySelectorAll(e,t).forEach(function(e){return e.classList.add(o.DIV_DO_NOT_APPLY_BASELINE)})}}},function(e,t,n){},function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}();n(16);var r,a=n(8),o=(r=a)&&r.__esModule?r:{default:r};var l={languages:1,lastEdited:2,pageIssues:3,disambiguation:4,coordinate:5,talkPage:6},s=function(){function e(t,n,i,r){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this.title=t,this.subtitle=n,this.itemType=i,this.clickHandler=r,this.payload=[]}return i(e,[{key:"iconClass",value:function(){switch(this.itemType){case l.languages:return"pagelib_footer_menu_icon_languages";case l.lastEdited:return"pagelib_footer_menu_icon_last_edited";case l.talkPage:return"pagelib_footer_menu_icon_talk_page";case l.pageIssues:return"pagelib_footer_menu_icon_page_issues";case l.disambiguation:return"pagelib_footer_menu_icon_disambiguation";case l.coordinate:return"pagelib_footer_menu_icon_coordinate";default:return""}}},{key:"payloadExtractor",value:function(){switch(this.itemType){case l.pageIssues:return o.default.collectPageIssuesText;case l.disambiguation:return function(e,t){return o.default.collectDisambiguationTitles(t)};default:return}}}]),e}();t.default={MenuItemType:l,setHeading:function(e,t,n){var i=n.getElementById(t);i.innerText=e,i.title=e},maybeAddItem:function(e,t,n,i,r,a){var o=new s(e,t,n,r),l=o.payloadExtractor();l&&(o.payload=l(a,a.querySelector('section[data-mw-section-id="0"]')),0===o.payload.length)||function(e,t,n){n.getElementById(t).appendChild(function(e,t){var n=t.createElement("div");n.className="pagelib_footer_menu_item";var i=t.createElement("a");if(i.addEventListener("click",function(){e.clickHandler(e.payload)}),n.appendChild(i),e.title){var r=t.createElement("div");r.className="pagelib_footer_menu_item_title",r.innerText=e.title,i.title=e.title,i.appendChild(r)}if(e.subtitle){var a=t.createElement("div");a.className="pagelib_footer_menu_item_subtitle",a.innerText=e.subtitle,i.appendChild(a)}var o=e.iconClass();return o&&n.classList.add(o),t.createDocumentFragment().appendChild(n)}(e,n))}(o,i,a)}}},function(e,t,n){},function(e,t,n){},function(e,t,n){},function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=o(n(1)),r=o(n(3)),a=o(n(0));function o(e){return e&&e.__esModule?e:{default:e}}function l(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}var s=function(e){return e.indexOf("#cite_note")>-1},u=function(e){return Boolean(e)&&e.nodeType===Node.TEXT_NODE&&Boolean(e.textContent.match(/^\s+$/))},c=function(e){var t=e.querySelector("a");return t&&s(t.hash)},d=function(e,t){var n=t.querySelector("A").getAttribute("href").slice(1);return e.getElementById(n)||e.getElementById(decodeURIComponent(n))},f=function(e,t){var n=d(e,t);if(!n)return"";var i=e.createDocumentFragment(),o=e.createElement("div");i.appendChild(o);Array.prototype.slice.call(n.childNodes).filter(r.default.isNodeTypeElementOrText).forEach(function(e){return o.appendChild(e.cloneNode(!0))});return a.default.querySelectorAll(o,"link, style, sup[id^=cite_ref], .mw-cite-backlink").forEach(function(e){return e.remove()}),o.innerHTML.trim()},_=function(e){return a.default.matchesSelector(e,".reference, .mw-ref")?e:i.default.findClosestAncestor(e,".reference, .mw-ref")},p=function e(t,n,i,r){l(this,e),this.id=t,this.rect=n,this.text=i,this.html=r},h=function e(t,n){l(this,e),this.href=t,this.text=n},g=function e(t,n){l(this,e),this.selectedIndex=t,this.referencesGroup=n},m=function(e,t){var n=e;do{n=t(n)}while(u(n));return n},v=function(e,t,n){for(var i=e;(i=m(i,t))&&i.nodeType===Node.ELEMENT_NODE&&c(i);)n(i)},b=function(e){return e.previousSibling},y=function(e){return e.nextSibling},E=function(e){var t=[e];return v(e,b,function(e){return t.unshift(e)}),v(e,y,function(e){return t.push(e)}),t};t.default={collectNearbyReferences:function(e,t){var n=t.parentElement,i=E(n),r=i.indexOf(n),a=i.map(function(t){return function(e,t){return new p(_(t).id,t.getBoundingClientRect(),t.textContent,f(e,t))}(e,t)});return new g(r,a)},collectNearbyReferencesAsText:function(e,t){var n=t.parentElement,i=E(n),r=i.indexOf(n),a=i.map(function(e){return function(e,t){return new h(t.querySelector("A").getAttribute("href"),t.textContent)}(0,e)});return new g(r,a)},isCitation:s,test:{adjacentNonWhitespaceNode:m,closestReferenceClassElement:_,collectAdjacentReferenceNodes:v,collectNearbyReferenceNodes:E,collectRefText:f,getRefTextContainer:d,hasCitationLink:c,isWhitespaceTextNode:u,nextSiblingGetter:y,prevSiblingGetter:b}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(22);var i={SECTION_HEADER:"pagelib_edit_section_header",TITLE:"pagelib_edit_section_title",LINK_CONTAINER:"pagelib_edit_section_link_container",LINK:"pagelib_edit_section_link",PROTECTION:{UNPROTECTED:"",PROTECTED:"page-protected",FORBIDDEN:"no-editing"}},r="pagelib_edit_section_title_description",a="pagelib_edit_section_add_title_description",o="pagelib_edit_section_divider",l="pagelib_edit_section_title_pronunciation",s="data-id",u="data-action",c=function(e,t){var n=e.createElement("span");n.classList.add(i.LINK_CONTAINER);var r=function(e,t){var n=e.createElement("a");return n.href="",n.setAttribute(s,t),n.setAttribute(u,"edit_section"),n.classList.add(i.LINK),n}(e,t);return n.appendChild(r),n},d=function(e,t,n,r){var a=!(arguments.length>4&&void 0!==arguments[4])||arguments[4],o=e.createElement("div");o.className=i.SECTION_HEADER;var l=e.createElement("h"+n);if(l.innerHTML=r||"",l.className=i.TITLE,l.setAttribute(s,t),o.appendChild(l),a){var u=c(e,t);o.appendChild(u)}return o},f=function(e,t,n,i){if(void 0!==t&&t.length>0){var o=e.createElement("p");return o.id=r,o.innerHTML=t,o}if(i){var l=e.createElement("a");l.href="#",l.setAttribute(u,"add_title_description");var s=e.createElement("p");return s.id=a,s.innerHTML=n,l.appendChild(s),l}return null};t.default={CLASS:i,ADD_TITLE_DESCRIPTION:a,newEditSectionButton:c,newEditSectionHeader:d,newEditLeadSectionHeader:function(e,t,n,i,r){var a=!(arguments.length>5&&void 0!==arguments[5])||arguments[5],s=arguments.length>6&&void 0!==arguments[6]&&arguments[6],c=e.createDocumentFragment(),_=d(e,0,1,t,a);if(s){var p=e.createElement("a");p.setAttribute(u,"title_pronunciation"),p.id=l,_.querySelector("h1").appendChild(p)}c.appendChild(_);var h=f(e,n,i,r);h&&c.appendChild(h);var g=e.createElement("hr");return g.id=o,c.appendChild(g),c}}},function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});t.default={setPercentage:function(e,t){t&&(e.style["-webkit-text-size-adjust"]=t,e.style["text-size-adjust"]=t)}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});t.default={setMargins:function(e,t){void 0!==t.top&&(e.style.marginTop=t.top),void 0!==t.right&&(e.style.marginRight=t.right),void 0!==t.bottom&&(e.style.marginBottom=t.bottom),void 0!==t.left&&(e.style.marginLeft=t.left)},setPadding:function(e,t){void 0!==t.top&&(e.style.paddingTop=t.top),void 0!==t.right&&(e.style.paddingRight=t.right),void 0!==t.bottom&&(e.style.paddingBottom=t.bottom),void 0!==t.left&&(e.style.paddingLeft=t.left)}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(26);var i="pagelib_dim_images",r=function(e,t){e.querySelector("html").classList[t?"add":"remove"](i)},a=function(e){return e.querySelector("html").classList.contains(i)};t.default={CLASS:i,dim:function(e,t){return r(e.document,t)},isDim:function(e){return a(e.document)},dimImages:r,areImagesDimmed:a}},function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}(),r=s(n(2)),a=s(n(1)),o=s(n(4)),l=s(n(5));function s(e){return e&&e.__esModule?e:{default:e}}var u=["scroll","resize",r.default.SECTION_TOGGLED_EVENT_TYPE],c=100,d=function(){function e(t,n){var i=this;!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this._window=t,this._loadDistanceMultiplier=n,this._placeholders=[],this._registered=!1,this._throttledLoadPlaceholders=l.default.wrap(t,c,function(){return i._loadPlaceholders()})}return i(e,[{key:"convertImagesToPlaceholders",value:function(e){var t=o.default.queryLazyLoadableImages(e),n=o.default.convertImagesToPlaceholders(this._window.document,t);this._placeholders=this._placeholders.concat(n),this._register()}},{key:"collectExistingPlaceholders",value:function(e){var t=Array.from(e.querySelectorAll("."+o.default.PLACEHOLDER_CLASS));this._placeholders=this._placeholders.concat(t),this._register()}},{key:"loadPlaceholders",value:function(){this._throttledLoadPlaceholders()}},{key:"deregister",value:function(){var e=this;this._registered&&(u.forEach(function(t){return e._window.removeEventListener(t,e._throttledLoadPlaceholders)}),this._throttledLoadPlaceholders.reset(),this._placeholders=[],this._registered=!1)}},{key:"_register",value:function(){var e=this;!this._registered&&this._placeholders.length&&(this._registered=!0,u.forEach(function(t){return e._window.addEventListener(t,e._throttledLoadPlaceholders)}))}},{key:"_loadPlaceholders",value:function(){var e=this;this._placeholders=this._placeholders.filter(function(t){var n=!0;return e._isPlaceholderEligibleToLoad(t)&&(o.default.loadPlaceholder(e._window.document,t),n=!1),n}),0===this._placeholders.length&&this.deregister()}},{key:"_isPlaceholderEligibleToLoad",value:function(e){return a.default.isVisible(e)&&this._isPlaceholderWithinLoadDistance(e)}},{key:"_isPlaceholderWithinLoadDistance",value:function(e){var t=e.getBoundingClientRect(),n=this._window.innerHeight*this._loadDistanceMultiplier;return!(t.top>n||t.bottom<-n)}}]),e}();t.default=d},function(e,t,n){},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});t.default={getSectionOffsets:function(e){return{sections:Array.from(e.querySelectorAll("section")).reduce(function(e,t){var n=t.getAttribute("data-mw-section-id"),i=t&&t.firstElementChild&&t.firstElementChild.querySelector(".pagelib_edit_section_title");return n&&parseInt(n)>=1&&e.push({heading:i&&i.innerHTML,id:parseInt(n),yOffset:t.offsetTop}),e},[])}}}},,,,,,,,,,,function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=O(n(12)),r=O(n(23)),a=O(n(24)),o=O(n(2)),l=O(n(8)),s=O(n(41)),u=O(n(25)),c=O(n(21)),d=O(n(10)),f=O(n(1)),_=O(n(42)),p=O(n(6)),h=O(n(7)),g=O(n(15)),m=O(n(9)),v=O(n(43)),b=O(n(4)),y=O(n(27)),E=O(n(11)),T=O(n(0)),L=O(n(44)),C=O(n(20)),A=O(n(5)),N=O(n(29)),S=O(n(45));function O(e){return e&&e.__esModule?e:{default:e}}n(47),t.default={AdjustTextSize:r.default,BodySpacingTransform:a.default,CollapseTable:o.default,CollectionUtilities:l.default,CompatibilityTransform:s.default,DimImagesTransform:u.default,EditTransform:c.default,LeadIntroductionTransform:_.default,FooterContainer:p.default,FooterLegal:h.default,FooterMenu:g.default,FooterReadMore:m.default,FooterTransformer:v.default,LazyLoadTransform:b.default,LazyLoadTransformer:y.default,PlatformTransform:E.default,RedLinks:L.default,ReferenceCollection:C.default,SectionUtilities:N.default,ThemeTransform:i.default,WidenImage:S.default,test:{ElementGeometry:d.default,ElementUtilities:f.default,Polyfill:T.default,Throttle:A.default}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i={FILTER:"pagelib_compatibility_filter"};t.default={COMPATIBILITY:i,enableSupport:function(e){var t=e.querySelector("html");(function(e){return function(e,t,n){var i=e.createElement("span");return t.some(function(e){return i.style[e]=n,i.style.cssText})}(e,["webkitFilter","filter"],"blur(0)")})(e)||t.classList.add(i.FILTER)}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,r=n(0),a=(i=r)&&i.__esModule?i:{default:i};var o=function(e){var t=e.querySelector('[id="coordinates"]'),n=t?t.textContent.length:0;return e.textContent.length-n>=50},l=function(e){var t=[],n=e;do{t.push(n),n=n.nextSibling}while(n&&(1!==n.nodeType||"P"!==n.tagName));return t},s=function(e,t){return a.default.querySelectorAll(e,"#"+t+" > p").find(o)};t.default={moveLeadIntroductionUp:function(e,t,n){var i=s(e,t);if(i){var r=e.createDocumentFragment();l(i).forEach(function(e){return r.appendChild(e)});var a=e.getElementById(t),o=n?n.nextSibling:a.firstChild;a.insertBefore(r,o)}},test:{isParagraphEligible:o,extractLeadIntroductionNodes:l,getEligibleParagraph:s}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i=function(){function e(e,t){for(var n=0;n<t.length;n++){var i=t[n];i.enumerable=i.enumerable||!1,i.configurable=!0,"value"in i&&(i.writable=!0),Object.defineProperty(e,i.key,i)}}return function(t,n,i){return n&&e(t.prototype,n),i&&e(t,i),t}}(),r=s(n(6)),a=s(n(7)),o=s(n(9)),l=s(n(5));function s(e){return e&&e.__esModule?e:{default:e}}var u=function(){function e(){!function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,e),this._resizeListener=void 0}return i(e,[{key:"add",value:function(e,t,n,i,s,u,c,d,f,_,p,h,g){this.remove(e),t.appendChild(r.default.containerFragment(e.document)),a.default.add(e.document,c,d,"pagelib_footer_container_legal",f,_,p),o.default.setHeading(s,"pagelib_footer_container_readmore_heading",e.document),o.default.add(i,u,"pagelib_footer_container_readmore_pages",n,g,function(t){r.default.updateBottomPaddingToAllowReadMoreToScrollToTop(e),h(t)},e.document),this._resizeListener=l.default.wrap(e,100,function(){return r.default.updateBottomPaddingToAllowReadMoreToScrollToTop(e)}),e.addEventListener("resize",this._resizeListener)}},{key:"remove",value:function(e){this._resizeListener&&(e.removeEventListener("resize",this._resizeListener),this._resizeListener.cancel(),this._resizeListener=void 0);var t=e.document.getElementById("pagelib_footer_container");t&&t.parentNode.removeChild(t)}}]),e}();t.default=u},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var i,r=n(0),a=(i=r)&&i.__esModule?i:{default:i};var o=function(e,t){e.innerHTML=t.innerHTML,e.setAttribute("class",t.getAttribute("class"))},l=function(e){return a.default.querySelectorAll(e,"a.new")},s=function(e){return e.createElement("span")},u=function(e,t){return e.parentNode.replaceChild(t,e)};t.default={hideRedLinks:function(e){var t=s(e);l(e).forEach(function(e){var n=t.cloneNode(!1);o(n,e),u(e,n)})},test:{configureRedLinkTemplate:o,redLinkAnchorsInDocument:l,newRedLinkTemplate:s,replaceAnchorWithSpan:u}}},function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),n(46);var i,r=n(1),a=(i=r)&&i.__esModule?i:{default:i};var o=function(e){for(var t=[],n=e;n.parentElement&&!(n=n.parentElement).classList.contains("content_block");)t.push(n);return t},l=function(e,t,n){e[t]=n},s=function(e,t,n){Boolean(e[t])&&l(e,t,n)},u={width:"100%",height:"auto",maxWidth:"100%",float:"none"},c=function(e){Object.keys(u).forEach(function(t){return s(e.style,t,u[t])})},d=function(e){Object.keys(u).forEach(function(t){return l(e.style,t,u[t])})},f=function(e){o(e).forEach(c);var t=a.default.findClosestAncestor(e,"a.image");t&&d(t)},_=function(e){return!a.default.findClosestAncestor(e,"[class*='noresize']")&&(!a.default.findClosestAncestor(e,"div[class*='tsingle']")&&(!e.hasAttribute("usemap")&&(!a.default.isNestedInTable(e)&&!a.default.closestInlineStyle(e,"position","absolute"))))};t.default={maybeWidenImage:function(e){return!!_(e)&&(function(e){f(e),e.classList.add("pagelib_widen_image_override")}(e),!0)},test:{ancestorsToWiden:o,shouldWidenImage:_,updateExistingStyleValue:s,widenAncestors:f,widenElementByUpdatingExistingStyles:c,widenElementByUpdatingStyles:d}}},function(e,t,n){},function(e,t,n){}]).default});

},{}]},{},[2,16,8,9,10,11,12,15,13,14,1,3,4,5,6,7]);
