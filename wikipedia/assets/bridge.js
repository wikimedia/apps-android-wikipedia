function Bridge() {
    this.eventHandlers = {};
}

// This is called directly from Java, and hence needs to be available
Bridge.prototype.handleMessage = function( type, msgPointer ) {
    var that = this;
    var payload = JSON.parse( marshaller.getPayload( msgPointer ) );
    if ( this.eventHandlers.hasOwnProperty( type ) ) {
        this.eventHandlers[type].forEach( function( callback ) {
            callback.call( that, payload );
        } );
    }
};

Bridge.prototype.registerListener = function( messageType, callback ) {
    if ( this.eventHandlers.hasOwnProperty( messageType ) ) {
        this.eventHandlers[messageType].push( callback );
    } else {
        this.eventHandlers[messageType] = [ callback ];
    }
};

Bridge.prototype.sendMessage = function( messageType, payload ) {
    var messagePack = { type: messageType, payload: payload };
    var ret = window.prompt( JSON.stringify( messagePack) );
    if ( ret ) {
        return JSON.parse( ret );
    }
};

module.exports = new Bridge();
// FIXME: Move this to somwehere else, eh?
window.onload = function() {
    module.exports.sendMessage( "DOMLoaded", {} );
};