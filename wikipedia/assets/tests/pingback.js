var bridge = require("../bridge");
console.log("Something!");
bridge.registerListener( "ping", function( payload ) {
    bridge.sendMessage( "pong", payload );
});
