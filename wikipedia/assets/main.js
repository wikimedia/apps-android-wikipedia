( function() {
    window.onload = function() {
        bridge.sendMessage( "DOMLoaded", {} );
    };

    bridge.registerListener( "displayLeadSection", function( payload ) {
        document.getElementById( "content" ).innerHTML += payload.leadSectionHTML;
    });

} )();