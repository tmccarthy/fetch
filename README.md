# Fetch

A library of tools for fetching data from the web. I will eventually split this into its own project. Includes:
* `db-lib`: A simple jdbc-based library for SQL interaction written in frustration with what's on offer in the Scala 
  ecosystem. I will probably break this into its own library once I'm happy with it.
* cats and fs2 based utilities for downloading and streaming data
* A rate-limiting backend for sttp
* A client for OpenStreetMap that can be used to geocode location strings
* A client for the data.gov.au API
* A local filesystem cache
