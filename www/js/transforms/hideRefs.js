var transformer = require("../transformer");
var collapseTables = require("./collapseTables");

transformer.register( "hideRefs", function( content ) {
    var refLists = content.querySelectorAll( "div.reflist" );
    for (var i = 0; i < refLists.length; i++) {
        var caption = "<strong class='app_table_collapsed_caption'>" + window.string_expand_refs + "</strong>";

        //create the container div that will contain both the original table
        //and the collapsed version.
        var containerDiv = document.createElement( 'div' );
        containerDiv.className = 'app_table_container';
        refLists[i].parentNode.insertBefore(containerDiv, refLists[i]);
        refLists[i].parentNode.removeChild(refLists[i]);

        //create the collapsed div
        var collapsedDiv = document.createElement( 'div' );
        collapsedDiv.classList.add('app_table_collapsed_container');
        collapsedDiv.classList.add('app_table_collapsed_open');
        collapsedDiv.innerHTML = caption;

        //create the bottom collapsed div
        var bottomDiv = document.createElement( 'div' );
        bottomDiv.classList.add('app_table_collapsed_bottom');
        bottomDiv.classList.add('app_table_collapse_icon');
        bottomDiv.innerHTML = window.string_table_close;

        //add our stuff to the container
        containerDiv.appendChild(collapsedDiv);
        containerDiv.appendChild(refLists[i]);
        containerDiv.appendChild(bottomDiv);

        //give it just a little padding
        refLists[i].style.padding = "4px";

        //set initial visibility
        refLists[i].style.display = 'none';
        collapsedDiv.style.display = 'block';
        bottomDiv.style.display = 'none';

        //assign click handler to the collapsed divs
        collapsedDiv.onclick = collapseTables.handleTableCollapseOrExpandClick;
        bottomDiv.onclick = collapseTables.handleTableCollapseOrExpandClick;
    }
} );