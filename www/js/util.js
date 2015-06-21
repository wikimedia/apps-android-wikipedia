
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
