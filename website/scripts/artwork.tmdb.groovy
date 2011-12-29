// filebot -script "http://filebot.sf.net/scripts/artwork.tmdb.groovy" -trust-script /path/to/media/

// EXPERIMENTAL // HERE THERE BE DRAGONS
if (net.sourceforge.filebot.Settings.applicationRevisionNumber < 808) throw new Exception("Application revision too old")


/*
 * Fetch movie artwork. The movie is determined using the parent folders name.
 */

def fetchArtwork(outputFile, movieInfo, artworkType, artworkSize) {
	// select and fetch artwork
	def artwork = movieInfo.images.find { it.type == artworkType && it.size == artworkSize }
	if (artwork == null) {
		println "Artwork not found: $outputFile"
		return null
	}
	
	println "Fetching $outputFile => $artwork"
	return artwork.url.saveAs(outputFile)
}


def fetchNfo(outputFile, movieInfo) {
	movieInfo.applyXmlTemplate('''<movie>
			<title>$name</title>
			<year>${released?.year}</year>
			<rating>$rating</rating>
			<votes>$votes</votes>
			<plot>$overview</plot>
			<runtime>$runtime</runtime>
			<mpaa>$certification</mpaa>
			<genre>${genres.size() > 0 ? genres.get(0) : ''}</genre>
			<id>tt${imdbId.pad(7)}</id>
		</movie>
	''').saveAs(outputFile)
}


def fetchMovieArtworkAndNfo(movieDir, movie) {
	println "Fetch nfo and artwork for $movie"
	def movieInfo = TheMovieDB.getMovieInfo(movie, Locale.ENGLISH)
	
	println movieInfo
	movieInfo.images.each {
		println "Available artwork: $it.url => $it"
	}
	
	// fetch nfo
	fetchNfo(movieDir['movie.nfo'], movieInfo)
	
	// fetch series banner, fanart, posters, etc
	fetchArtwork(movieDir['folder.jpg'], movieInfo, 'poster', 'original')
	fetchArtwork(movieDir['backdrop.jpg'], movieInfo, 'backdrop', 'original')
}


args.getFolders().each { dir ->
	def videos = dir.listFiles{ it.isVideo() }
	if (videos.isEmpty()) {
		return null
	}
	
	def query = _args.query ?: dir.name
	def options = TheMovieDB.searchMovie(query, Locale.ENGLISH)
	if (options.isEmpty()) {
		println "Movie not found: $query"
		return null
	}
	
	// auto-select series
	def movie = options.sortBySimilarity(query, { it.name })[0]
	
	println "$dir => $movie"
	fetchMovieArtworkAndNfo(dir, movie)
}
