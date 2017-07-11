var pagelib = require("wikimedia-page-library");
var transformer = require("../transformer");

function scrollWithDecorOffset(container) {
    window.scrollTo( 0, container.parentNode.offsetTop - transformer.getDecorOffset() );
}

function toggleCollapseClickCallback() {
    pagelib.CollapseTable.toggleCollapseClickCallback.call(this, scrollWithDecorOffset);
}

transformer.register( "hideTables", function(content) {
    pagelib.CollapseTable.collapseTables(window, content, window.pageTitle,
        window.isMainPage, window.string_table_infobox,
        window.string_table_other, window.string_table_close,
        scrollWithDecorOffset);
});

module.exports = {
    handleTableCollapseOrExpandClick: toggleCollapseClickCallback
};
