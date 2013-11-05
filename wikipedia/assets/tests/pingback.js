(function() {
    bridge.sendMessage( "pingBackLoaded", {} );
    bridge.registerListener( "ping", function( payload ) {
        bridge.sendMessage( "pong", payload );
    });
} )();
