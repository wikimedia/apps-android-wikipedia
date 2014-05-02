module.exports = function ( grunt ) {
    var allScriptFiles = [
        "js/main.js",
        "js/tranformer.js",
        "js/transforms.js",
        "js/bridge.js",
        "js/actions.js",
        "js/editaction.js",
        "js/sections.js",
        "js/rtlsupport.js",
        "lib/js/classList.js",
        "tests/*.js"
    ];
    var allStyleFiles = [
        "less/*.less"
    ];
    var allHTMLFiles = [
        "index.html",
        "tests/index.html"
    ];
    // FIXME: Unconditionally included polyfills. Should be included only for Android 2.3
    var oldDroidPolyfills = [
        "lib/js/classList.js"
    ];

    grunt.initConfig( {
        pkg: grunt.file.readJSON( "package.json" ),
        browserify: {
            dist: {
                files: {
                    "bundle.js": [
                        "js/loader.js",
                        "js/main.js",
                        "js/transformer.js",
                        "js/transforms.js",
                        "js/bridge.js",
                        "js/actions.js",
                        "js/editaction.js",
                        "js/sections.js",
                        "js/rtlsupport.js"
                    ].concat( oldDroidPolyfills ),
                    "bundle-test.js": [
                        "js/loader.js",
                        "js/main.js",
                        "js/bridge.js",
                        "tests/*.js"
                    ].concat( oldDroidPolyfills ),
                    "abusefilter.js": [
                        "js/loader.js",
                        "js/bridge.js",
                        "js/abusefilter.js",
                        "js/rtlsupport.js"
                    ].concat( oldDroidPolyfills ),
                    "preview.js": [
                        "js/loader.js",
                        "js/bridge.js",
                        "js/actions.js",
                        "js/preview.js",
                        "js/rtlsupport.js"
                    ].concat( oldDroidPolyfills )
                }
            }
        },
        less: {
            all: {
                files: [
                    { src: ["less/fonts.less", "less/pagestyles.less", "less/ui.less", "less/wikihacks.less"], dest: "styles.css"},
                    { src: ["less/fonts.less", "less/pagestyles.less", "less/wikihacks.less"], dest: "abusefilter.css"},
                    { src: ["less/fonts.less", "less/pagestyles.less", "less/preview.less", "less/wikihacks.less"], dest: "preview.css"}
                ]
            }
        },
        jshint: {
            allFiles: allScriptFiles,
            options: {
                jshintrc: ".jshintrc"
            }
        },
        copy: {
            main: {
                files: [
                    // App files
                    {src: ["bundle.js", "index.html", "styles.css"], dest: "../wikipedia/assets/"},

                    // Test files
                    {src: ["bundle-test.js", "tests/index.html"], dest: "../wikipedia/assets/"},

                    // Abusefilter files
                    { src: ["abusefilter.js", "abusefilter.css", "abusefilter.html"], dest: "../wikipedia/assets/" },

                    // Preview files
                    { src: ["preview.js", "preview.css", "preview.html"], dest: "../wikipedia/assets/" },

                    // Images
                    {src: ["images/*"], dest: "../wikipedia/assets/"},

                    // Fonts
                    {src: ["fonts/*"], dest: "../wikipedia/assets/"}
                ]
            }
        },
        watch: {
            scripts: {
                files: allScriptFiles.concat( allStyleFiles ).concat( allHTMLFiles ),
                tasks: ["default"]
            }
        }
    } );

    grunt.loadNpmTasks( 'grunt-browserify' );
    grunt.loadNpmTasks( 'grunt-contrib-jshint' );
    grunt.loadNpmTasks( 'grunt-contrib-copy' );
    grunt.loadNpmTasks( 'grunt-contrib-watch' );
    grunt.loadNpmTasks( 'grunt-contrib-less' );

    grunt.registerTask( 'default', [ 'browserify', 'less', 'copy' ] );
};
