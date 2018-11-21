var bridge = require("./bridge");
var transformer = require("./transformer");
var theme = require("./theme");
var pagelib = require("wikimedia-page-library");
var lazyLoadViewportDistanceMultiplier = 2; // Load images on the current screen up to one ahead.
var lazyLoadTransformer = new pagelib.LazyLoadTransformer(window, lazyLoadViewportDistanceMultiplier);

var opacityLight = 0.54;
var opacityDark = 0.7;

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
}

function isRtl() {
    return window.directionality === 'rtl';
}

function makeSvgElement( width, height, color, svgContents ) {
    var svg = document.createElement( "svg" );
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svg.setAttribute("width", width);
    svg.setAttribute("height", height);
    svg.setAttribute("style", "vertical-align:middle;fill:" + color + ";");
    svg.innerHTML = svgContents;
    return svg;
}

/*
TODO: migrate this upstream to page-library, or to MCS.
*/
function setTitleElement( parentNode ) {
    var titleDiv = document.createElement( "div" );
    titleDiv.id = "heading_0";
    titleDiv.setAttribute( "data-id", 0 );
    titleDiv.className = "section_heading";
    titleDiv.setAttribute( "dir", window.directionality );

    // Create the actual H1 heading
    var h1 = document.createElement( "h1" );
    h1.innerHTML = window.pageTitle;

    if (window.hasPronunciation) {
        // Create and append the audio "speaker" icon
        var audioIcon = makeSvgElement(16, 16, 'currentColor', '<path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"></path><path d="M0 0h24v24H0z" fill="none"></path>');
        audioIcon.setAttribute("viewBox", "0 0 24 24");
        audioIcon.style.margin = "0 6px 2px 6px";
        if (isRtl()) { audioIcon.style.transform = "scaleX(-1)"; }
        var audioAnchor = document.createElement( "a" );
        audioAnchor.setAttribute("data-action", "pronunciation_click");
        audioAnchor.setAttribute("style", "color:inherit;");
        audioAnchor.innerHTML = audioIcon.outerHTML;
        h1.appendChild(audioAnchor);
    }

    // Div that will contain the description and edit pencil
    var descriptionDiv = document.createElement( "div" );
    var mainEditPencilAnchor = document.createElement( "a" );
    mainEditPencilAnchor.setAttribute("data-action", "edit_main");
    mainEditPencilAnchor.setAttribute("class", "pagelib_edit_section_link");
    var style = window.isDarkMode ? "filter:invert(100%);-webkit-filter:invert(100%);" : "";
    style += isRtl() ? "float:left;transform:scaleX(-1);" : "float:right;"
    mainEditPencilAnchor.setAttribute("style", style);
    mainEditPencilAnchor.style.opacity = window.isDarkMode ? opacityDark : opacityLight;

    // Span that will contain the description and/or the two-line icon
    var descriptionSpan = document.createElement( "span" );
    descriptionSpan.setAttribute("style", "font-size:90%;");
    if (window.pageDescription) {
        descriptionSpan.innerHTML = window.pageDescription;
        descriptionSpan.style.opacity = window.isDarkMode ? opacityDark : opacityLight;
    } else if (window.allowDescriptionEdit) {
        var descriptionAnchor = document.createElement( "a" );
        var descSvg = makeSvgElement(24, 16, 'currentColor', '<defs><path id="a" d="M0 0h24v24H0V0z"/></defs><clipPath id="b"><use xlink:href="#a" overflow="visible"/></clipPath><path d="M4 9h16v2H4zm0 4h10v2H4z" clip-path="url(#b)"/>');
        descSvg.setAttribute("viewBox", "4 9 24 8");
        if (isRtl()) { descSvg.style.transform = "scaleX(-1)"; }
        descriptionAnchor.setAttribute("href", "#");
        descriptionAnchor.setAttribute("data-action", "edit_description");
        descriptionAnchor.innerHTML = descSvg.outerHTML + window.string_add_description;
        descriptionAnchor.setAttribute("style", "font-style:italic;");
        descriptionSpan.appendChild(descriptionAnchor);
    } else {
        descriptionSpan.innerHTML = " ";
    }

    // Decorative divider that appears below the title and description
    var dividerLine = document.createElement( "div" );
    dividerLine.setAttribute("style", "width:56px;border-top: 1px solid currentColor !important;margin-top:12px;margin-bottom:16px;");
    dividerLine.style.opacity = window.isDarkMode ? opacityDark : opacityLight;

    descriptionDiv.appendChild(mainEditPencilAnchor);
    descriptionDiv.appendChild(descriptionSpan);
    if (!window.isMainPage) {
        titleDiv.appendChild(h1);
        titleDiv.appendChild(descriptionDiv);
        titleDiv.appendChild(dividerLine);
    }
    parentNode.appendChild(titleDiv);
}

bridge.registerListener( "displayLeadSection", function( payload ) {
    var lazyDocument;

    // This might be a refresh! Clear out all contents!
    clearContents();
    setWindowAttributes(payload);
    window.offline = false;

    theme.applyTheme(payload);

    var contentElem = document.getElementById( "content" );
    setTitleElement(contentElem);

    lazyDocument = document.implementation.createHTMLDocument( );
    var content = lazyDocument.createElement( "div" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.section.text;
    content.id = "content_block_0";

    // append the content to the DOM now, so that we can obtain
    // dimension measurements for items.
    document.getElementById( "content" ).appendChild( content );

    applySectionTransforms(content, true);

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
              section.id, section.toclevel + 1, section.line, !section.noedit);
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
}

function displayRemainingSections(json, sequence, scrollY, fragment) {
    var contentWrapper = document.getElementById( "content" );
    var scrolled = false;

    var response = { "sequence": sequence };

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
    transformer.transform( "fixAudio", document );
    transformer.transform( "hideTables", document );
    transformer.transform( "showIssues", document );
    lazyLoadTransformer.loadPlaceholders();
    bridge.sendMessage( "pageLoadComplete", response );
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
        if (this.readyState !== XMLHttpRequest.DONE || this.status === 0 || this.sequence !== window.sequence) {
            return;
        }
        if (this.status < 200 || this.status > 299) {
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
