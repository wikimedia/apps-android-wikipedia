module.exports = function ( grunt ) {
    var allScriptFiles = [
        "js/main.js",
        "js/tranformer.js",
        "js/transforms.js",
        "js/bridge.js",
        "js/actions.js",
        "js/editaction.js",
        "js/issues.js",
        "js/disambig.js",
        "js/sections.js",
        "js/rtlsupport.js",
        "js/util.js",
        "js/widenImages.js",
        "lib/js/classList.js",
        "tests/*.js"
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
                        "lib/js/css-color-parser.js",
                        "js/main.js",
                        "js/night.js",
                        "js/transformer.js",
                        "js/transforms.js",
                        "js/bridge.js",
                        "js/actions.js",
                        "js/editaction.js",
                        "js/issues.js",
                        "js/disambig.js",
                        "js/sections.js",
                        "js/rtlsupport.js",
                        "js/util.js",
                        "js/widenImages.js"
                    ].concat( oldDroidPolyfills ),
                    "bundle-test.js": [
                        "js/loader.js",
                        "js/main.js",
                        "js/bridge.js",
                        "tests/*.js"
                    ].concat( oldDroidPolyfills ),
                    "preview.js": [
                        "js/loader.js",
                        "js/bridge.js",
                        "js/night.js",
                        "js/actions.js",
                        "js/preview.js",
                        "js/rtlsupport.js",
                        "js/util.js"
                    ].concat( oldDroidPolyfills )
                }
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
                    {src: ["bundle.js", "index.html"], dest: "../app/src/main/assets/"},

                    // Test files
                    {src: ["bundle-test.js", "tests/index.html"], dest: "../app/src/main/assets/"},

                    // Preview files
                    { src: ["preview.js", "preview.html"], dest: "../app/src/main/assets/" },
                ]
            }
        },
        watch: {
            scripts: {
                files: allScriptFiles.concat( allHTMLFiles ),
                tasks: ["default"]
            }
        }
    } );

    grunt.loadNpmTasks( 'grunt-browserify' );
    grunt.loadNpmTasks( 'grunt-contrib-jshint' );
    grunt.loadNpmTasks( 'grunt-contrib-copy' );
    grunt.loadNpmTasks( 'grunt-contrib-watch' );

    grunt.registerTask( 'default', [ 'browserify', 'copy' ] );
};
