/*jshint node:true */
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
        "js/dark.js",
        "js/preview.js",
        "js/rtlsupport.js",
        "js/sections.js",
        "tests/*.js"
    ];
    var allHTMLFiles = [
        "index.html",
        "tests/index.html"
    ];
    var allJsonFiles = [
        "package.json"
    ];
    var distFolder = "../app/src/main/assets/";

    grunt.loadNpmTasks( 'grunt-contrib-jshint' );
    grunt.loadNpmTasks( 'grunt-jsonlint' );
    grunt.loadNpmTasks( 'grunt-browserify' );
    grunt.loadNpmTasks( 'grunt-contrib-copy' );
    grunt.loadNpmTasks( 'grunt-contrib-watch' );

    grunt.initConfig( {
        jshint: {
            options: {
                jshintrc: true
            },
            allFiles: allScriptFiles
        },
        jsonlint: {
            allFiles: allJsonFiles
        },
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
                        "js/dark.js",
                        "js/preview.js",
                        "js/rtlsupport.js",
                        "js/sections.js"
                    ],
                    "bundle-test.js": [
                        "js/main.js",
                        "js/bridge.js",
                        "tests/*.js"
                    ],
                    "preview.js": [
                        "js/bridge.js",
                        "js/dark.js",
                        "js/actions.js",
                        "js/preview.js",
                        "js/rtlsupport.js",
                        "js/util.js"
                    ]
                }
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

                    { src: [ "node_modules/wikimedia-page-library/build/wikimedia-page-library-transform.css" ], dest: distFolder + 'wikimedia-page-library.css' }
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

    grunt.registerTask( 'test', [ 'jshint', 'jsonlint', 'browserify', 'copy' ] );
    grunt.registerTask( 'default', 'test' );
};
