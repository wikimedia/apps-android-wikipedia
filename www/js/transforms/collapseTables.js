var getTableHeader = require("applib").CollapseElement.getTableHeader;
var transformer = require("../transformer");

function handleTableCollapseOrExpandClick() {
    var container = this.parentNode;
    var divCollapsed = container.children[0];
    var tableFull = container.children[1];
    var divBottom = container.children[2];
    var caption = divCollapsed.querySelector('.app_table_collapsed_caption');
    if (tableFull.style.display !== 'none') {
        tableFull.style.display = 'none';
        divCollapsed.classList.remove('app_table_collapse_close');
        divCollapsed.classList.remove('app_table_collapse_icon');
        divCollapsed.classList.add('app_table_collapsed_open');
        if (caption !== null) {
            caption.style.visibility = 'visible';
        }
        divBottom.style.display = 'none';
        //if they clicked the bottom div, then scroll back up to the top of the table.
        if (this === divBottom) {
            window.scrollTo( 0, container.offsetTop - transformer.getDecorOffset() );
        }
    } else {
        tableFull.style.display = 'block';
        divCollapsed.classList.remove('app_table_collapsed_open');
        divCollapsed.classList.add('app_table_collapse_close');
        divCollapsed.classList.add('app_table_collapse_icon');
        if (caption !== null) {
            caption.style.visibility = 'hidden';
        }
        divBottom.style.display = 'block';
    }
}

transformer.register( "hideTables", function( content ) {
    var tables = content.querySelectorAll( "table" );
    for (var i = 0; i < tables.length; i++) {
        //is the table already hidden? if so, don't worry about it
        if (tables[i].style.display === 'none' || tables[i].classList.contains( 'navbox' ) || tables[i].classList.contains( 'vertical-navbox' ) || tables[i].classList.contains( 'navbox-inner' ) || tables[i].classList.contains( 'metadata' )) {
            continue;
        }

        var isInfobox = tables[i].classList.contains( 'infobox' );
        var headerText = getTableHeader(tables[i], window.pageTitle);
        if (headerText.length === 0 && !isInfobox) {
            continue;
        }
        var caption = "<strong>" + (isInfobox ? window.string_table_infobox : window.string_table_other) + "</strong>";
        caption += "<span class='app_span_collapse_text'>";
        if (headerText.length > 0) {
            caption += ": " + headerText[0];
        }
        if (headerText.length > 1) {
            caption += ", " + headerText[1];
        }
        if (headerText.length > 0) {
            caption += " ...";
        }
        caption += "</span>";

        //create the container div that will contain both the original table
        //and the collapsed version.
        var containerDiv = document.createElement( 'div' );
        containerDiv.className = 'app_table_container';
        tables[i].parentNode.insertBefore(containerDiv, tables[i]);
        tables[i].parentNode.removeChild(tables[i]);

        //remove top and bottom margin from the table, so that it's flush with
        //our expand/collapse buttons
        tables[i].style.marginTop = "0px";
        tables[i].style.marginBottom = "0px";

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
        containerDiv.appendChild(tables[i]);
        containerDiv.appendChild(bottomDiv);

        //set initial visibility
        tables[i].style.display = 'none';
        collapsedDiv.style.display = 'block';
        bottomDiv.style.display = 'none';

        //assign click handler to the collapsed divs
        collapsedDiv.onclick = handleTableCollapseOrExpandClick;
        bottomDiv.onclick = handleTableCollapseOrExpandClick;
    }
} );

module.exports = {
    handleTableCollapseOrExpandClick: handleTableCollapseOrExpandClick
};