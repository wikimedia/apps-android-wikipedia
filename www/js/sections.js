var bridge = require("./bridge");
var transformer = require("./transformer");
var clickHandlerSetup = require("./onclick");
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
    document.getElementById( "loading_sections").className = "loading";

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
    var heading = document.createElement( "h" + ( section.toclevel + 1 ) );
    heading.setAttribute( "dir", window.directionality );
    heading.innerHTML = typeof section.line !== "undefined" ? section.line : "";
    heading.id = section.anchor;
    heading.className = "section_heading";
    heading.setAttribute( 'data-id', section.id );

    heading.appendChild( pagelib.EditTransform.newEditSectionButton( document, section.id ) );

    lazyDocument = document.implementation.createHTMLDocument( );
    content = lazyDocument.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = section.text;
    content.id = "content_block_" + section.id;

    applySectionTransforms(content, false);
    return [ heading, content ];
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
        transformer.transform( "hideIPA", content );
    } else {
        clickHandlerSetup.addIPAonClick( content );
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
    document.getElementById( "loading_sections").className = "";
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
            zimNodes[i].className = "section_heading";
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
    var sectionHeaders = document.getElementsByClassName( "section_heading" );
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
