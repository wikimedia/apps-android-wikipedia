var bridge = require("../bridge");
bridge.registerListener( "injectScript", function( payload ) {
    require(payload.src);
});