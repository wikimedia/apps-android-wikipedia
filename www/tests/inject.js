var bridge = require("../js/bridge");
bridge.registerListener( "injectScript", function( payload ) {
    require(payload.src);
});