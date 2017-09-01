var bridge = require("./bridge");
var pagelib = require("wikimedia-page-library");

bridge.registerListener( 'toggleDarkMode', function( payload ) {
    var theme;

    window.isDarkMode = !window.isDarkMode;

    theme = window.isDarkMode ? pagelib.ThemeTransform.THEME.DARK : pagelib.ThemeTransform.THEME.DEFAULT;
    pagelib.ThemeTransform.setTheme( document, theme );
    pagelib.DimImagesTransform.dim( window, window.isDarkMode && payload.dimImages );
} );

bridge.registerListener( 'toggleDimImages', function( payload ) {
    pagelib.DimImagesTransform.dim( window, payload.dimImages );
} );