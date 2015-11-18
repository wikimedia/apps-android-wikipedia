var bridge = require("./bridge");

/*
OnClick handler function for IPA spans.
*/
function ipaClickHandler() {
    var container = this;
    bridge.sendMessage( "ipaSpan", { "contents": container.innerHTML });
}

function addIPAonClick( content ) {
    var spans = content.querySelectorAll( "span.ipa_button" );
    for (var i = 0; i < spans.length; i++) {
        var parent = spans[i].parentNode;
        parent.onclick = ipaClickHandler;
    }
}

module.exports = {
    addIPAonClick: addIPAonClick
};