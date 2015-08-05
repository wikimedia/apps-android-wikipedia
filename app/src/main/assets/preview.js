(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var bridge = require('./bridge');
var util = require('./util');

function ActionsHandler() {
}

var actionHandlers = {};

ActionsHandler.prototype.register = function( action, fun ) {
    if ( action in actionHandlers ) {
        actionHandlers[action].push( fun );
    } else {
        actionHandlers[action] = [ fun ];
    }
};

bridge.registerListener( "handleReference", function( payload ) {
    handleReference( payload.anchor, false );
});

function handleReference( targetId, backlink ) {
    var targetElem = document.getElementById( targetId );
    if ( targetElem === null ) {
        console.log( "reference target not found: " + targetId );
    } else if ( !backlink && targetId.slice(0, 4).toLowerCase() === "cite" ) { // treat "CITEREF"s the same as "cite_note"s
        try {
            var refTexts = targetElem.getElementsByClassName( "reference-text" );
            if ( refTexts.length > 0 ) {
                targetElem = refTexts[0];
            }
            bridge.sendMessage( 'referenceClicked', { "ref": targetElem.innerHTML } );
        } catch (e) {
            targetElem.scrollIntoView();
        }
    } else {
        // If it is a link to another anchor in the current page, just scroll to it
        targetElem.scrollIntoView();
    }
}

document.onclick = function() {
    var sourceNode = null;
    var curNode = event.target;
    // If an element was clicked, check if it or any of its parents are <a>
    // This handles cases like <a>foo</a>, <a><strong>foo</strong></a>, etc.
    while (curNode) {
        if (curNode.tagName === "A" || curNode.tagName === "AREA") {
            sourceNode = curNode;
            break;
        }
        curNode = curNode.parentNode;
    }

    if (sourceNode) {
        if ( sourceNode.hasAttribute( "data-action" ) ) {
            var action = sourceNode.getAttribute( "data-action" );
            var handlers = actionHandlers[ action ];
            for ( var i = 0; i < handlers.length; i++ ) {
                handlers[i]( sourceNode, event );
            }
        } else {
            var href = sourceNode.getAttribute( "href" );
            if ( href[0] === "#" ) {
                var targetId = href.slice(1);
                if ( "issues" === targetId ) {
                    issuesClicked( sourceNode );
                } else if ( "disambig" === targetId ) {
                    disambigClicked( sourceNode );
                } else {
                    handleReference( targetId, util.ancestorContainsClass( sourceNode, "mw-cite-backlink" ) );
                }
            } else if (sourceNode.classList.contains( 'app_media' )) {
                bridge.sendMessage( 'mediaClicked', { "href": href } );
            } else if (sourceNode.classList.contains( 'image' )) {
                bridge.sendMessage( 'imageClicked', { "href": href } );
            } else {
                bridge.sendMessage( 'linkClicked', { "href": href } );
            }
            event.preventDefault();
        }
    }
};

function issuesClicked( sourceNode ) {
    var issues = collectIssues( sourceNode.parentNode );
    var disambig = collectDisambig( sourceNode.parentNode.parentNode ); // not clicked node
    bridge.sendMessage( 'issuesClicked', { "hatnotes": disambig, "issues": issues } );
}

function disambigClicked( sourceNode ) {
    var disambig = collectDisambig( sourceNode.parentNode );
    var issues = collectIssues( sourceNode.parentNode.parentNode ); // not clicked node
    bridge.sendMessage( 'disambigClicked', { "hatnotes": disambig, "issues": issues } );
}

function collectDisambig( sourceNode ) {
    var res = [];
    var links = sourceNode.querySelectorAll( 'div.hatnote a' );
    var i = 0,
        len = links.length;
    for (; i < len; i++) {
        // Pass the href; we'll decode it into a proper page title in Java
        res.push( links[i].getAttribute( 'href' ) );
    }
    return res;
}

function collectIssues( sourceNode ) {
    var res = [];
    var issues = sourceNode.querySelectorAll( 'table.ambox' );
    var i = 0,
        len = issues.length;
    for (; i < len; i++) {
        // .ambox- is used e.g. on eswiki
        res.push( issues[i].querySelector( '.mbox-text, .ambox-text' ).innerHTML );
    }
    return res;
}

module.exports = new ActionsHandler();

},{"./bridge":2,"./util":7}],2:[function(require,module,exports){
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
    var ret = window.prompt( encodeURIComponent(JSON.stringify( messagePack )) );
    if ( ret ) {
        return JSON.parse( ret );
    }
};

module.exports = new Bridge();
// FIXME: Move this to somewhere else, eh?
window.onload = function() {
    module.exports.sendMessage( "DOMLoaded", {} );
};
},{}],3:[function(require,module,exports){
var bridge = require( "./bridge" );

function addStyleLink( href ) {
    var link = document.createElement( "link" );
    link.setAttribute( "rel", "stylesheet" );
    link.setAttribute( "type", "text/css" );
    link.setAttribute( "charset", "UTF-8" );
    link.setAttribute( "href", href );
    document.getElementsByTagName( "head" )[0].appendChild( link );
}

bridge.registerListener( "injectStyles", function( payload ) {
    var style_paths = payload.style_paths;
    for ( var i = 0; i < style_paths.length; i++ ) {
        addStyleLink( style_paths[i] );
    }
});

module.exports = {
	addStyleLink: addStyleLink
};
},{"./bridge":2}],4:[function(require,module,exports){
var parseCSSColor = require("../lib/js/css-color-parser");
var bridge = require("./bridge");
var loader = require("./loader");
var util = require("./util");

function parseRgbValues( el, propertyName ) {
	var property = el.style[propertyName];
	var bits = parseCSSColor( property );
	if ( bits === null ) {
		return;
	}
	return [ parseInt( bits[0] ), parseInt( bits[1] ), parseInt( bits[2] )];
}

function invertColorProperty( el, propertyName ) {
	var rgb = parseRgbValues( el, propertyName );
	if ( rgb ) {
		el.style[propertyName] = 'rgb(' + (255 - rgb[0]) + ', ' + (255 - rgb[1]) + ', ' + (255 - rgb[2]) + ')';
	}
}

function halveRgbValues( el, propertyName ) {
	var rgb = parseRgbValues( el, propertyName );
	if ( rgb ) {
		el.style[propertyName] = 'rgb(' + Math.floor(rgb[0] / 2) + ', ' + Math.floor(rgb[1] / 2) + ', ' + Math.floor(rgb[2] / 2) + ')';
	}
}

var invertProperties = [ 'color', 'background-color', 'border-color' ];
function invertOneElement( el ) {
	var isInTable = util.hasAncestor( el, 'TABLE' );
	for ( var i = 0; i < invertProperties.length; i++ ) {
		if ( el.style[invertProperties[i]] ) {
			if ( isInTable ) {
				halveRgbValues( el, invertProperties[i] );
			} else {
				invertColorProperty( el, invertProperties[i] );
			}
		}
	}
}

function invertElement( el ) {
    // first, invert the colors of tables and other elements
	var allElements = el.querySelectorAll( '*[style]' );
	for ( var i = 0; i < allElements.length; i++ ) {
		invertOneElement( allElements[i] );
	}
    // and now, look for Math formula images, and invert them
    var mathImgs = el.querySelectorAll( "[class*='math-fallback']" );
    for ( i = 0; i < mathImgs.length; i++ ) {
        var mathImg = mathImgs[i];
        // KitKat and higher can use webkit to invert colors
        if (window.apiLevel >= 19) {
            mathImg.style.cssText = mathImg.style.cssText + ";-webkit-filter: invert(100%);";
        } else {
            // otherwise, just give it a mild background color
            mathImg.style.backgroundColor = "#ccc";
            // and give it a little padding, since the text is right up against the edge.
            mathImg.style.padding = "2px";
        }
    }
}

function toggle( nightCSSURL, hasPageLoaded ) {
	window.isNightMode = !window.isNightMode;

	// Remove the <style> tag if it exists, add it otherwise
	var nightStyle = document.querySelector( "link[href='" + nightCSSURL + "']" );
	console.log( nightCSSURL );
	if ( nightStyle ) {
		nightStyle.parentElement.removeChild( nightStyle );
	} else {
		loader.addStyleLink( nightCSSURL );
	}

	if ( hasPageLoaded ) {
		// If we are doing this before the page has loaded, no need to swap colors ourselves
		// If we are doing this after, that means the transforms in transformers.js won't run
		// And we have to do this ourselves
		invertElement( document.querySelector( '.content' ) );
	}
}

bridge.registerListener( 'toggleNightMode', function( payload ) {
	toggle( payload.nightStyleBundle.style_paths[0], payload.hasPageLoaded );
} );

module.exports = {
	invertElement: invertElement
};

},{"../lib/js/css-color-parser":9,"./bridge":2,"./loader":3,"./util":7}],5:[function(require,module,exports){
var bridge = require("./bridge");

bridge.registerListener( "displayPreviewHTML", function( payload ) {
    var content = document.getElementById( "content" );
    content.setAttribute( "dir", window.directionality );
    content.innerHTML = payload.html;
} );

},{"./bridge":2}],6:[function(require,module,exports){
var bridge = require("./bridge");

bridge.registerListener( "setDirectionality", function( payload ) {
    window.directionality = payload.contentDirection;
    var html = document.getElementsByTagName( "html" )[0];
    // first, remove all the possible directionality classes...
    html.classList.remove( "content-rtl" );
    html.classList.remove( "content-ltr" );
    html.classList.remove( "ui-rtl" );
    html.classList.remove( "ui-ltr" );
    // and then set the correct class based on our payload.
    html.classList.add( "content-" + window.directionality );
    html.classList.add( "ui-" + payload.uiDirection );
} );

},{"./bridge":2}],7:[function(require,module,exports){

function hasAncestor( el, tagName ) {
    if (el !== null && el.tagName === tagName) {
        return true;
    } else {
        if ( el.parentNode !== null && el.parentNode.tagName !== 'BODY' ) {
            return hasAncestor( el.parentNode, tagName );
        } else {
            return false;
        }
    }
}

function ancestorContainsClass( element, className ) {
    var contains = false;
    var curNode = element;
    while (curNode) {
        if ((typeof curNode.classList !== "undefined")) {
            if (curNode.classList.contains(className)) {
                contains = true;
                break;
            }
        }
        curNode = curNode.parentNode;
    }
    return contains;
}

function getDictionaryFromSrcset(srcset) {
    /*
    Returns dictionary with density (without "x") as keys and urls as values.
    Parameter 'srcset' string:
        '//image1.jpg 1.5x, //image2.jpg 2x, //image3.jpg 3x'
    Returns dictionary:
        {1.5: '//image1.jpg', 2: '//image2.jpg', 3: '//image3.jpg'}
    */
    var sets = srcset.split(',').map(function(set) {
        return set.trim().split(' ');
    });
    var output = {};
    sets.forEach(function(set) {
        output[set[1].replace('x', '')] = set[0];
    });
    return output;
}

function firstDivAncestor (el) {
    while ((el = el.parentElement)) {
        if (el.tagName === 'DIV') {
            return el;
        }
    }
    return null;
}

function firstAncestorWithMultipleChildren (el) {
    while ((el = el.parentElement) && (el.childElementCount === 1)){}
    return el;
}

function isNestedInTable(el) {
    while ((el = el.parentElement)) {
        if (el.tagName === 'TD') {
            return true;
        }
    }
    return false;
}

function shouldAddImageOverflowXContainer(image) {
    if ((image.width > document.getElementById('content').offsetWidth) && !isNestedInTable(image)) {
        return true;
    } else {
        return false;
    }
}

function addImageOverflowXContainer(image, ancestor) {
    image.setAttribute('hasOverflowXContainer', 'true'); // So "widenImages" transform knows instantly not to widen this one.
    var div = document.createElement( 'div' );
    div.className = 'image_overflow_x_container';
    ancestor.parentElement.insertBefore( div, ancestor );
    div.appendChild(ancestor);
}

function maybeAddImageOverflowXContainer() {
    var image = this;
    if (shouldAddImageOverflowXContainer(image)) {
        var ancestor = firstAncestorWithMultipleChildren(image);
        if (ancestor) {
            addImageOverflowXContainer(image, ancestor);
        }
    }
}

module.exports = {
    hasAncestor: hasAncestor,
    ancestorContainsClass: ancestorContainsClass,
    getDictionaryFromSrcset: getDictionaryFromSrcset,
    firstDivAncestor: firstDivAncestor,
    isNestedInTable: isNestedInTable,
    maybeAddImageOverflowXContainer: maybeAddImageOverflowXContainer
};

},{}],8:[function(require,module,exports){
/**
 * MIT LICENSCE
 * From: https://github.com/remy/polyfills
 * FIXME: Don't copy paste libraries, use a dep management system.
 */
(function () {

if (typeof window.Element === "undefined" || "classList" in document.documentElement) return;

var prototype = Array.prototype,
    push = prototype.push,
    splice = prototype.splice,
    join = prototype.join;

function DOMTokenList(el) {
  this.el = el;
  // The className needs to be trimmed and split on whitespace
  // to retrieve a list of classes.
  var classes = el.className.replace(/^\s+|\s+$/g,'').split(/\s+/);
  for (var i = 0; i < classes.length; i++) {
    push.call(this, classes[i]);
  }
};

DOMTokenList.prototype = {
  add: function(token) {
    if(this.contains(token)) return;
    push.call(this, token);
    this.el.className = this.toString();
  },
  contains: function(token) {
    return this.el.className.indexOf(token) != -1;
  },
  item: function(index) {
    return this[index] || null;
  },
  remove: function(token) {
    if (!this.contains(token)) return;
    for (var i = 0; i < this.length; i++) {
      if (this[i] == token) break;
    }
    splice.call(this, i, 1);
    this.el.className = this.toString();
  },
  toString: function() {
    return join.call(this, ' ');
  },
  toggle: function(token) {
    if (!this.contains(token)) {
      this.add(token);
    } else {
      this.remove(token);
    }

    return this.contains(token);
  }
};

window.DOMTokenList = DOMTokenList;

function defineElementGetter (obj, prop, getter) {
    if (Object.defineProperty) {
        Object.defineProperty(obj, prop,{
            get : getter
        });
    } else {
        obj.__defineGetter__(prop, getter);
    }
}

defineElementGetter(Element.prototype, 'classList', function () {
  return new DOMTokenList(this);
});

})();

},{}],9:[function(require,module,exports){
// (c) Dean McNamee <dean@gmail.com>, 2012.
//
// https://github.com/deanm/css-color-parser-js
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

// http://www.w3.org/TR/css3-color/
var kCSSColorTable = {
  "transparent": [0,0,0,0], "aliceblue": [240,248,255,1],
  "antiquewhite": [250,235,215,1], "aqua": [0,255,255,1],
  "aquamarine": [127,255,212,1], "azure": [240,255,255,1],
  "beige": [245,245,220,1], "bisque": [255,228,196,1],
  "black": [0,0,0,1], "blanchedalmond": [255,235,205,1],
  "blue": [0,0,255,1], "blueviolet": [138,43,226,1],
  "brown": [165,42,42,1], "burlywood": [222,184,135,1],
  "cadetblue": [95,158,160,1], "chartreuse": [127,255,0,1],
  "chocolate": [210,105,30,1], "coral": [255,127,80,1],
  "cornflowerblue": [100,149,237,1], "cornsilk": [255,248,220,1],
  "crimson": [220,20,60,1], "cyan": [0,255,255,1],
  "darkblue": [0,0,139,1], "darkcyan": [0,139,139,1],
  "darkgoldenrod": [184,134,11,1], "darkgray": [169,169,169,1],
  "darkgreen": [0,100,0,1], "darkgrey": [169,169,169,1],
  "darkkhaki": [189,183,107,1], "darkmagenta": [139,0,139,1],
  "darkolivegreen": [85,107,47,1], "darkorange": [255,140,0,1],
  "darkorchid": [153,50,204,1], "darkred": [139,0,0,1],
  "darksalmon": [233,150,122,1], "darkseagreen": [143,188,143,1],
  "darkslateblue": [72,61,139,1], "darkslategray": [47,79,79,1],
  "darkslategrey": [47,79,79,1], "darkturquoise": [0,206,209,1],
  "darkviolet": [148,0,211,1], "deeppink": [255,20,147,1],
  "deepskyblue": [0,191,255,1], "dimgray": [105,105,105,1],
  "dimgrey": [105,105,105,1], "dodgerblue": [30,144,255,1],
  "firebrick": [178,34,34,1], "floralwhite": [255,250,240,1],
  "forestgreen": [34,139,34,1], "fuchsia": [255,0,255,1],
  "gainsboro": [220,220,220,1], "ghostwhite": [248,248,255,1],
  "gold": [255,215,0,1], "goldenrod": [218,165,32,1],
  "gray": [128,128,128,1], "green": [0,128,0,1],
  "greenyellow": [173,255,47,1], "grey": [128,128,128,1],
  "honeydew": [240,255,240,1], "hotpink": [255,105,180,1],
  "indianred": [205,92,92,1], "indigo": [75,0,130,1],
  "ivory": [255,255,240,1], "khaki": [240,230,140,1],
  "lavender": [230,230,250,1], "lavenderblush": [255,240,245,1],
  "lawngreen": [124,252,0,1], "lemonchiffon": [255,250,205,1],
  "lightblue": [173,216,230,1], "lightcoral": [240,128,128,1],
  "lightcyan": [224,255,255,1], "lightgoldenrodyellow": [250,250,210,1],
  "lightgray": [211,211,211,1], "lightgreen": [144,238,144,1],
  "lightgrey": [211,211,211,1], "lightpink": [255,182,193,1],
  "lightsalmon": [255,160,122,1], "lightseagreen": [32,178,170,1],
  "lightskyblue": [135,206,250,1], "lightslategray": [119,136,153,1],
  "lightslategrey": [119,136,153,1], "lightsteelblue": [176,196,222,1],
  "lightyellow": [255,255,224,1], "lime": [0,255,0,1],
  "limegreen": [50,205,50,1], "linen": [250,240,230,1],
  "magenta": [255,0,255,1], "maroon": [128,0,0,1],
  "mediumaquamarine": [102,205,170,1], "mediumblue": [0,0,205,1],
  "mediumorchid": [186,85,211,1], "mediumpurple": [147,112,219,1],
  "mediumseagreen": [60,179,113,1], "mediumslateblue": [123,104,238,1],
  "mediumspringgreen": [0,250,154,1], "mediumturquoise": [72,209,204,1],
  "mediumvioletred": [199,21,133,1], "midnightblue": [25,25,112,1],
  "mintcream": [245,255,250,1], "mistyrose": [255,228,225,1],
  "moccasin": [255,228,181,1], "navajowhite": [255,222,173,1],
  "navy": [0,0,128,1], "oldlace": [253,245,230,1],
  "olive": [128,128,0,1], "olivedrab": [107,142,35,1],
  "orange": [255,165,0,1], "orangered": [255,69,0,1],
  "orchid": [218,112,214,1], "palegoldenrod": [238,232,170,1],
  "palegreen": [152,251,152,1], "paleturquoise": [175,238,238,1],
  "palevioletred": [219,112,147,1], "papayawhip": [255,239,213,1],
  "peachpuff": [255,218,185,1], "peru": [205,133,63,1],
  "pink": [255,192,203,1], "plum": [221,160,221,1],
  "powderblue": [176,224,230,1], "purple": [128,0,128,1],
  "red": [255,0,0,1], "rosybrown": [188,143,143,1],
  "royalblue": [65,105,225,1], "saddlebrown": [139,69,19,1],
  "salmon": [250,128,114,1], "sandybrown": [244,164,96,1],
  "seagreen": [46,139,87,1], "seashell": [255,245,238,1],
  "sienna": [160,82,45,1], "silver": [192,192,192,1],
  "skyblue": [135,206,235,1], "slateblue": [106,90,205,1],
  "slategray": [112,128,144,1], "slategrey": [112,128,144,1],
  "snow": [255,250,250,1], "springgreen": [0,255,127,1],
  "steelblue": [70,130,180,1], "tan": [210,180,140,1],
  "teal": [0,128,128,1], "thistle": [216,191,216,1],
  "tomato": [255,99,71,1], "turquoise": [64,224,208,1],
  "violet": [238,130,238,1], "wheat": [245,222,179,1],
  "white": [255,255,255,1], "whitesmoke": [245,245,245,1],
  "yellow": [255,255,0,1], "yellowgreen": [154,205,50,1]}

function clamp_css_byte(i) {  // Clamp to integer 0 .. 255.
  i = Math.round(i);  // Seems to be what Chrome does (vs truncation).
  return i < 0 ? 0 : i > 255 ? 255 : i;
}

function clamp_css_float(f) {  // Clamp to float 0.0 .. 1.0.
  return f < 0 ? 0 : f > 1 ? 1 : f;
}

function parse_css_int(str) {  // int or percentage.
  if (str[str.length - 1] === '%')
    return clamp_css_byte(parseFloat(str) / 100 * 255);
  return clamp_css_byte(parseInt(str));
}

function parse_css_float(str) {  // float or percentage.
  if (str[str.length - 1] === '%')
    return clamp_css_float(parseFloat(str) / 100);
  return clamp_css_float(parseFloat(str));
}

function css_hue_to_rgb(m1, m2, h) {
  if (h < 0) h += 1;
  else if (h > 1) h -= 1;

  if (h * 6 < 1) return m1 + (m2 - m1) * h * 6;
  if (h * 2 < 1) return m2;
  if (h * 3 < 2) return m1 + (m2 - m1) * (2/3 - h) * 6;
  return m1;
}

function parseCSSColor(css_str) {
  // Remove all whitespace, not compliant, but should just be more accepting.
  var str = css_str.replace(/ /g, '').toLowerCase();

  // Color keywords (and transparent) lookup.
  if (str in kCSSColorTable) return kCSSColorTable[str].slice();  // dup.

  // #abc and #abc123 syntax.
  if (str[0] === '#') {
    if (str.length === 4) {
      var iv = parseInt(str.substr(1), 16);  // TODO(deanm): Stricter parsing.
      if (!(iv >= 0 && iv <= 0xfff)) return null;  // Covers NaN.
      return [((iv & 0xf00) >> 4) | ((iv & 0xf00) >> 8),
              (iv & 0xf0) | ((iv & 0xf0) >> 4),
              (iv & 0xf) | ((iv & 0xf) << 4),
              1];
    } else if (str.length === 7) {
      var iv = parseInt(str.substr(1), 16);  // TODO(deanm): Stricter parsing.
      if (!(iv >= 0 && iv <= 0xffffff)) return null;  // Covers NaN.
      return [(iv & 0xff0000) >> 16,
              (iv & 0xff00) >> 8,
              iv & 0xff,
              1];
    }

    return null;
  }

  var op = str.indexOf('('), ep = str.indexOf(')');
  if (op !== -1 && ep + 1 === str.length) {
    var fname = str.substr(0, op);
    var params = str.substr(op+1, ep-(op+1)).split(',');
    var alpha = 1;  // To allow case fallthrough.
    switch (fname) {
      case 'rgba':
        if (params.length !== 4) return null;
        alpha = parse_css_float(params.pop());
        // Fall through.
      case 'rgb':
        if (params.length !== 3) return null;
        return [parse_css_int(params[0]),
                parse_css_int(params[1]),
                parse_css_int(params[2]),
                alpha];
      case 'hsla':
        if (params.length !== 4) return null;
        alpha = parse_css_float(params.pop());
        // Fall through.
      case 'hsl':
        if (params.length !== 3) return null;
        var h = (((parseFloat(params[0]) % 360) + 360) % 360) / 360;  // 0 .. 1
        // NOTE(deanm): According to the CSS spec s/l should only be
        // percentages, but we don't bother and let float or percentage.
        var s = parse_css_float(params[1]);
        var l = parse_css_float(params[2]);
        var m2 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
        var m1 = l * 2 - m2;
        return [clamp_css_byte(css_hue_to_rgb(m1, m2, h+1/3) * 255),
                clamp_css_byte(css_hue_to_rgb(m1, m2, h) * 255),
                clamp_css_byte(css_hue_to_rgb(m1, m2, h-1/3) * 255),
                alpha];
      default:
        return null;
    }
  }

  return null;
}

try { module.exports = parseCSSColor } catch(e) { }

},{}]},{},[3,2,4,1,5,6,7,8])