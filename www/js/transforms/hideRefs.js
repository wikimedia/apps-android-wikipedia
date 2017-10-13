var transformer = require("../transformer");

transformer.register( "hideRefs", function( content ) {
    var refLists = content.querySelectorAll( "div.reflist" );
    for (var i = 0; i < refLists.length; i++) {
        // Wrap this div in a <table>, so that it will be caught by the pagelibrary for collapsing.
        var table = document.createElement( 'table' );
        var tr = document.createElement( 'tr' );
        var th = document.createElement( 'th' );
        var td = document.createElement( 'td' );
        th.style.display = "none";
        th.innerHTML = window.string_expand_refs;
        table.appendChild(th);
        table.appendChild(tr);
        tr.appendChild(td);

        refLists[i].parentNode.insertBefore(table, refLists[i]);
        refLists[i].parentNode.removeChild(refLists[i]);
        td.appendChild(refLists[i]);
    }
} );