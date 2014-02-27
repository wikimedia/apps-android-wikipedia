(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var bridge = require('./bridge');
var transformer = require('./transformer');

transformer.register( 'clearInlineStyling', function( content ) {
    var styledElements = content.querySelectorAll( "*[style]" );
    for ( var i = 0; i < styledElements.length; i++ ) {
        styledElements[i].removeAttribute( "style" );
    }
    return content;
} );

bridge.registerListener( 'displayWarning', function( payload ) {
    var content = document.getElementById( 'content' );

    var warning = document.createElement( 'div' );
    warning.innerHTML = payload.html;

    warning = transformer.transform( 'clearInlineStyling', warning );

    content.appendChild( warning );
} );

},{"./bridge":2,"./transformer":4}],2:[function(require,module,exports){
function Bridge() {
}

var eventHandlers = {};

// This is called directly from Java
window.handleMessage = function( type, msgPointer ) {
    var that = this;
    var payload = JSON.parse( marshaller.getPayload( msgPointer ) );
    if ( eventHandlers.hasOwnProperty( type ) ) {
        eventHandlers[type].forEach( function( callback ) {
            callback.call( that, payload );
        } );
    }
};

Bridge.prototype.registerListener = function( messageType, callback ) {
    if ( eventHandlers.hasOwnProperty( messageType ) ) {
        eventHandlers[messageType].push( callback );
    } else {
        eventHandlers[messageType] = [ callback ];
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
},{}],3:[function(require,module,exports){
var bridge = require("./bridge");

bridge.registerListener( "setDirectionality", function( payload ) {
    document.getElementsByTagName( "html" )[0].setAttribute( "dir", payload.dir );
} );
},{"./bridge":2}],4:[function(require,module,exports){
function Transformer() {
}

var transforms = {};

Transformer.prototype.register = function( transform, fun ) {
    if ( transform in transforms ) {
        transforms[transform].append( fun );
    } else {
        transforms[transform] = [ fun ];
    }
};

Transformer.prototype.transform = function( transform, element ) {
    var functions = transforms[transform];
    for ( var i = 0; i < functions.length; i++ ) {
        element = functions[i](element);
    }
    return element;
};

module.exports = new Transformer();

},{}]},{},[2,1,3])