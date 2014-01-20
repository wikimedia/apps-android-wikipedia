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
        }
    } );

    grunt.loadNpmTasks( 'grunt-browserify' );

    grunt.registerTask( 'default', [ 'browserify' ] );
}