module.exports = function( grunt ) {
    grunt.initConfig( {
        pkg: grunt.file.readJSON( "package.json" ),
        browserify: {
            dist: {
                files: {
                    "bundle.js": [ "main.js", "transforms.js", "bridge.js", "linkactions.js"],
                    "bundle-test.js": [ "main.js", "bridge.js", "tests/*.js" ]
                }
            }
        },
        jshint: {
            allFiles: [
                "main.js",
                "transforms.js",
                "bridge.js",
                "linkactions.js",
                "tests/*.js"
            ],
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
                    {src: ["bundle-test.js", "pagestlyes.css", "ui.css"], dest: "../wikipedia/assets/"},

                    // Images
                    {src: ["images/*"], dest:"../wikipedia/assets/"}
                ]
            }
        }
    } );

    grunt.loadNpmTasks( 'grunt-browserify' );
    grunt.loadNpmTasks( 'grunt-contrib-jshint' );
    grunt.loadNpmTasks( 'grunt-contrib-copy' );

    grunt.registerTask( 'default', [ 'browserify', 'copy' ] );
};