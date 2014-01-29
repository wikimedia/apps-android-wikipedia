module.exports = function( grunt ) {
    var allScriptFiles = [
        "js/main.js",
        "js/transforms.js",
        "js/bridge.js",
        "js/linkactions.js",
        "js/sections.js",
        "tests/*.js"
    ];
    var allStyleFiles = [
        "less/*.less"
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
                    "bundle.js": [ "js/main.js", "js/transforms.js", "js/bridge.js", "js/linkactions.js", "js/sections.js"],
                    "bundle-test.js": [ "js/main.js", "js/bridge.js", "tests/*.js" ],
                    "abusefilter.js": [ "js/bridge.js", "js/abusefilter.js" ]
                }
            }
        },
        less: {
            all: {
                files: [
                    { src: ["less/fonts.less", "less/pagestyles.less", "less/ui.less"], dest: "styles.css"},
                    { src: ["less/fonts.less", "less/pagestyles.less"], dest: "abusefilter.css"}
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

                    // Images
                    {src: ["images/*"], dest:"../wikipedia/assets/"},

                    // Fonts
                    {src: ["fonts/*"], dest:"../wikipedia/assets/"}
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