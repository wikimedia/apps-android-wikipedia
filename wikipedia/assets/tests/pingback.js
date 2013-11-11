(function() {
    bridge.registerListener( "ping", function( payload ) {
        bridge.sendMessage( "pong", payload );
    });
    bridge.sendMessage( "pingBackLoaded", {} );
} )();
