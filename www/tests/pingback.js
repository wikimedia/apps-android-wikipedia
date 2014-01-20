var bridge = require("../js/bridge");
console.log("Something!");
bridge.registerListener( "ping", function( payload ) {
    bridge.sendMessage( "pong", payload );
});
