var transformer = require("../transformer");

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

        // But only move one P!
        break;
    }
} );