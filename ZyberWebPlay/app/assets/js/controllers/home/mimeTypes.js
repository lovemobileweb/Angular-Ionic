define([], function() {
	var mimeTypes = {};
	//TODO complement with full list of supported mimeTypes for viewer
	//take into account mimeTypes returned by our service (right now using tika)
	var documents = ["application/pdf",
	                         "application/vnd.oasis.opendocument.text",
	                         "application/vnd.oasis.opendocument.presentation",
	                         "application/vnd.oasis.opendocument.spreadsheet",
	                         "application/msword",
	                         "application/vnd.ms-word.document.macroenabled.12",
	                         "application/x-tika-ooxml"];
	var images = ["image/jpeg",
                      "image/png",
                      "image/gif"];
	mimeTypes.documentMimetypes = documents;
	mimeTypes.imageMimetypes = images;
	mimeTypes.groupDocTypes = documents.concat(images);

	return mimeTypes;
});