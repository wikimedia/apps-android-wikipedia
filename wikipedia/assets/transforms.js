( function() {
    var Transforms = function () {};

    // List of transformation functions by their target type
    var transformsByType = {
        'lead': [
            moveInfobox,
            useLocalImagesForSavedPages
        ],
        'body': [
            useLocalImagesForSavedPages
        ]
    }

    function moveInfobox( leadContent ) {
        // Move infobox to the bottom of the lead section
        var infobox = leadContent.querySelector( "table.infobox" );
        if ( infobox ) {
            infobox.parentNode.removeChild( infobox );
            leadContent.appendChild( infobox );
        }
        return leadContent;
    }

    function useLocalImagesForSavedPages( content ) {
        var images = content.querySelectorAll( "img" );
        function onError() {
            var img = event.target;
            // Only work on http or https URLs. If we do not have this check, we might go on an infinte loop
            if ( img.src.substring( 0, 4 ) === "http" )  {
                // if it is already not a file URL!
                var resp = bridge.sendMessage( "imageUrlToFilePath", { "imageUrl": img.src } );
                console.log( "new filepath is " + resp.filePath );
                img.src = "file://" + resp.filePath;
            }
        }
        for ( var i = 0; i < images.length; i++ ) {
            images[i].onerror = onError;
        }
        return content;
    }

    Transforms.prototype.transform = function( type, content ) {
        var transforms = transformsByType[ type ];
        if ( transforms.length ) {
            transforms.forEach( function ( transform ) {
                content = transform( content );
            } );
        }
        return content;
    };

    window.transforms = new Transforms();
}) ();