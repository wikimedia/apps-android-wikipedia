module.exports = function( grunt ) {
    var allScriptFiles = [
        "js/main.js",
        "js/transforms.js",
        "js/bridge.js",
        "js/linkactions.js",
        "tests/*.js"
    ];
    var allStyleFiles = [
        "pagestyles.css",
        "ui.css"
    ];
    var allHTMLFiles = [
        "index.html",
        "tests/index.html"
    ];
    grunt.initConfig( {
        pkg: grunt.file.readJSON( "package.json" ),
        browserify: {
            dist: {
                files: {
                    "bundle.js": [ "js/main.js", "js/transforms.js", "js/bridge.js", "js/linkactions.js"],
                    "bundle-test.js": [ "js/main.js", "js/bridge.js", "tests/*.js" ]
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
                    {src: ["bundle.js", "index.html", "pagestyles.css", "ui.css"], dest: "../wikipedia/assets/"},

                    // Test files
                    {src: ["bundle-test.js", "tests/index.html"], dest: "../wikipedia/assets/"},

                    // Images
                    {src: ["images/*"], dest:"../wikipedia/assets/"}
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

    grunt.registerTask( 'default', [ 'browserify', 'copy' ] );
};