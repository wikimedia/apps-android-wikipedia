var bridge = require("./bridge");
var pagelib = require("wikimedia-page-library");

bridge.registerListener( 'setTheme', function( payload ) {
    var theme;
    switch (payload.theme) {
        case 1:
            theme = pagelib.ThemeTransform.THEME.DARK;
            window.isDarkMode = true;
            break;
        case 2:
            theme = pagelib.ThemeTransform.THEME.BLACK;
            window.isDarkMode = true;
            break;
        default:
            theme = pagelib.ThemeTransform.THEME.DEFAULT;
            window.isDarkMode = false;
            break;
    }
    pagelib.ThemeTransform.setTheme( document, theme );
    pagelib.DimImagesTransform.dim( window, window.isDarkMode && payload.dimImages );
} );

bridge.registerListener( 'toggleDimImages', function( payload ) {
    pagelib.DimImagesTransform.dim( window, payload.dimImages );
} );