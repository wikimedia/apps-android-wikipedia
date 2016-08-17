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