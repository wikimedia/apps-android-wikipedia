module.exports = function( grunt ) {
    grunt.initConfig( {
        pkg: grunt.file.readJSON( "package.json" ),
        browserify: {
            dist: {
                files: {
                    "bundle.js": [ "main.js", "transforms.js", "bridge.js"],
                    "bundle-test.js": [ "main.js", "bridge.js", "tests/*.js" ]
                }
            }
        },
        jshint: {
            allFiles: [
                "main.js",
                "transforms.js",
                "bridge.js",
                "tests/*.js"
            ],
            options: {
                jshintrc: ".jshintrc"
            }
        }
    } );

    grunt.loadNpmTasks( 'grunt-browserify' );
    grunt.loadNpmTasks( 'grunt-contrib-jshint' );

    grunt.registerTask( 'default', [ 'browserify' ] );
};