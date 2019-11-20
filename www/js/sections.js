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
