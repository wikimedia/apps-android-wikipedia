module.exports = function ( grunt ) {
    var allScriptFiles = [
        "js/bridge.js",
        "js/main.js",
        "js/utilities.js",
        "js/transformer.js",
        "js/transforms/*.js",
        "js/transforms/service/*.js",
        "js/actions.js",
        "js/disambig.js",
        "js/editaction.js",
        "js/issues.js",
        "js/loader.js",
        "js/night.js",
        "js/preview.js",
        "js/rtlsupport.js",
        "js/sections.js",
        "tests/*.js"
    ];
    var allHTMLFiles = [
        "index.html",
        "tests/index.html"
    ];
    var distFolder = "../app/src/main/assets/";

    grunt.loadNpmTasks( 'grunt-browserify' );
    grunt.loadNpmTasks( 'grunt-contrib-jshint' );
    grunt.loadNpmTasks( 'grunt-contrib-copy' );
    grunt.loadNpmTasks( 'grunt-contrib-watch' );

    grunt.initConfig( {
        pkg: grunt.file.readJSON( "package.json" ),
        browserify: {
            dist: {
                files: {
                    "bundle.js": [
                        "js/bridge.js",
                        "js/main.js",
                        "js/utilities.js",
                        "js/transformer.js",
                        "js/transforms/*.js",
                        "js/transforms/service/*.js",
                        "js/actions.js",
                        "js/disambig.js",
                        "js/editaction.js",
                        "js/issues.js",
                        "js/loader.js",
                        "js/night.js",
                        "js/preview.js",
                        "js/rtlsupport.js",
                        "js/sections.js"
                    ],
                    "bundle-test.js": [
                        "js/loader.js",
                        "js/main.js",
                        "js/bridge.js",
                        "tests/*.js"
                    ],
                    "preview.js": [
                        "js/loader.js",
                        "js/bridge.js",
                        "js/night.js",
                        "js/actions.js",
                        "js/preview.js",
                        "js/rtlsupport.js",
                        "js/util.js"
                    ]
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
                    { src: [ "bundle.js", "index.html" ], dest: distFolder },

                    // Test files
                    { src: [ "bundle-test.js", "tests/index.html" ], dest: distFolder },

                    // Preview files
                    { src: [ "preview.js", "preview.html" ], dest: distFolder },
                ]
            }
        },
        watch: {
            scripts: {
                files: allScriptFiles.concat( allHTMLFiles ),
                tasks: [ "default" ]
            }
        }
    } );

    grunt.registerTask( 'default', [ 'browserify', 'copy' ] );
};
